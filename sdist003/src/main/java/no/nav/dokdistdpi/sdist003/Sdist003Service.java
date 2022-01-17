package no.nav.dokdistdpi.sdist003;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.HentKvitteringResponse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.ForretningsmeldingParseException;
import no.nav.dokdistdpi.exception.technical.KunneIkkeHentKvitteringException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HENT_KVITTERING_STATUS_CODE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class Sdist003Service {

	private static final String DPI_KVITTERING_COUNTER = "dpi_antall_kvittering_counter";

	private final DpiClient dpiClient;
	private final ProducerTemplate producerTemplate;
	private final Queue qdist014;
	private final MeterRegistry meterRegistry;
	private final ObjectMapper objectMapper;
	private final LagreJuridiskLoggService juridiskLoggService;

	@Autowired
	public Sdist003Service(@Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper, DpiClient dpiClient, ProducerTemplate producerTemplate,
						   MeterRegistry meterRegistry, Queue qdist014, LagreJuridiskLoggService juridiskLoggService) {
		this.dpiClient = dpiClient;
		this.producerTemplate = producerTemplate;
		this.qdist014 = qdist014;
		this.meterRegistry = meterRegistry;
		this.objectMapper = dpiObjectMapper;
		this.juridiskLoggService = juridiskLoggService;
	}

	@Handler
	public void hentKvitteringOgBekreft(Exchange exchange) {

		ResponseEntity<HentKvitteringResponse[]> kvitteringList = dpiClient.hentKvittering();

		if (!OK.equals(kvitteringList.getStatusCode()) && !NO_CONTENT.equals(kvitteringList.getStatusCode())) {
			throw new KunneIkkeHentKvitteringException("Kunne ikke hente kvitteringer fra Digdir");
		}

		exchange.setProperty(HENT_KVITTERING_STATUS_CODE, kvitteringList.getStatusCode());

		if (OK.equals(kvitteringList.getStatusCode())) {
			Arrays.stream(kvitteringList.getBody())
					.filter(Objects::nonNull)
					.map(kvittering -> jwtPayload(kvittering))
					.forEach(payload -> {
						SimpleStandardBusinessDocument simpleSbd = mapSbd(payload);
						log.info("Mottatt kvittering fra dpi aksesspunkt med bestillingsId={} og conversationId={}", simpleSbd.getBestillingsId(), simpleSbd.getConversationId());
						producerTemplate.sendBody("jms:" + qdist014, payload);
						log.info("Sdist003 har skrevet melding på qdist014");

						juridiskLoggService.lagreJuridiskLogg(payload);

						KvitteringType kvitteringType = getKvitteringType(simpleSbd);
						dpiKivtteringCounter(kvitteringType);

						dpiClient.bekreft(simpleSbd.getBestillingsId());
					});
		}
	}

	private SimpleStandardBusinessDocument mapSbd(String jwtPayload) {
		try {
			return objectMapper.readValue(jwtPayload, SimpleStandardBusinessDocument.class);
		} catch (JsonProcessingException e) {
			throw new ForretningsmeldingParseException("Feilet å mappe JWT Forretningsmelding", e);
		}
	}

	private String jwtPayload(HentKvitteringResponse hentKvitteringResponse) {
		try {
			return JOSEObject.parse(hentKvitteringResponse.getForretningsmelding()).getPayload().toString();
		} catch (ParseException e) {
			throw new ForretningsmeldingParseException("Feilet å mappe JWT Forretningsmelding", e);
		}
	}

	private KvitteringType getKvitteringType(SimpleStandardBusinessDocument simpleSbd) {

		if (LEVERING.getType().equals(simpleSbd.getType())) {
			return LEVERING;
		} else if (VARSLINGFEILET.getType().equals(simpleSbd.getType())) {
			return VARSLINGFEILET;
		} else if (FEILET.getType().equals(simpleSbd.getType())) {
			return FEILET;
		}
		throw new SikkerDigitalPostException("Kvittering tilbake fra dpi meldingsformidler var verken kvittering eller feil.");
	}

	private void dpiKivtteringCounter(KvitteringType kvitteringType) {
		meterRegistry.counter(DPI_KVITTERING_COUNTER,
				"kvitteringStatus", isNull(kvitteringType.name()) ? "UKJENT" : kvitteringType.name()).increment();
	}
}
