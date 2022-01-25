package no.nav.dokdistdpi.sdist003;

import com.nimbusds.jose.JOSEObject;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.HentKvitteringResponse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import no.nav.dokdistdpi.exception.technical.KunneIkkeHentKvitteringException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.utils.JsonObjectMapper.mapSimpleSbd;
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
	private final LagreJuridiskLoggService juridiskLoggService;

	@Autowired
	public Sdist003Service(DpiClient dpiClient, ProducerTemplate producerTemplate,
						   MeterRegistry meterRegistry, Queue qdist014, LagreJuridiskLoggService juridiskLoggService) {
		this.dpiClient = dpiClient;
		this.producerTemplate = producerTemplate;
		this.qdist014 = qdist014;
		this.meterRegistry = meterRegistry;
		this.juridiskLoggService = juridiskLoggService;
	}

	@Handler
	public HttpStatus hentKvitteringOgBekreft(Exchange exchange) {

		ResponseEntity<HentKvitteringResponse[]> kvitteringList = dpiClient.hentKvittering();

		if (!OK.equals(kvitteringList.getStatusCode()) && !NO_CONTENT.equals(kvitteringList.getStatusCode())) {
			throw new KunneIkkeHentKvitteringException("Kunne ikke hente kvitteringer fra Digdir");
		}

		Map<String, String> bestillingsIds = new HashMap<>();

		if (OK.equals(kvitteringList.getStatusCode()) && nonNull(kvitteringList.getBody())) {
			Arrays.stream(kvitteringList.getBody())
					.map(this::getForretningsmeldingFromJwt)
					.forEach(payload -> {
						SimpleStandardBusinessDocument simpleSbd = mapSimpleSbd(payload);
						log.info("Mottatt kvittering fra dpi aksesspunkt med bestillingsId={} og conversationId={}", simpleSbd.getBestillingsId(), simpleSbd.getConversationId());
						producerTemplate.sendBody("jms:" + qdist014, payload);
						log.info("Sdist003 har skrevet melding på qdist014 med bestillingsId={} og conversationId={}", simpleSbd.getBestillingsId(), simpleSbd.getConversationId());

						juridiskLoggService.lagreJuridiskLogg(payload);

						KvitteringType kvitteringType = getKvitteringType(simpleSbd);
						countDpiKvittering(kvitteringType);

						dpiClient.bekreft(simpleSbd.getBestillingsId());
						bestillingsIds.put(simpleSbd.getBestillingsId(), simpleSbd.getType());
					});
			log.info("Hentet kvitteringer={}", bestillingsIds);
		}

		return kvitteringList.getStatusCode();
	}

	private String getForretningsmeldingFromJwt(HentKvitteringResponse hentKvitteringResponse) {
		try {
			return JOSEObject.parse(hentKvitteringResponse.getForretningsmelding()).getPayload().toString();
		} catch (ParseException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	private KvitteringType getKvitteringType(SimpleStandardBusinessDocument simpleSbd) {

		if (LEVERING.getValue().equals(simpleSbd.getType())) {
			return LEVERING;
		} else if (VARSLINGFEILET.getValue().equals(simpleSbd.getType())) {
			return VARSLINGFEILET;
		} else if (FEILET.getValue().equals(simpleSbd.getType())) {
			return FEILET;
		}
		throw new SikkerDigitalPostException("Kvittering tilbake fra dpi meldingsformidler var hverken kvittering eller feil.");
	}

	private void countDpiKvittering(KvitteringType kvitteringType) {
		meterRegistry.counter(DPI_KVITTERING_COUNTER,
				"kvitteringStatus", isNull(kvitteringType.name()) ? "UKJENT" : kvitteringType.name()).increment();
	}
}
