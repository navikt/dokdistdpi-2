package no.nav.dokdistdpi.sdist003;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.HentKvitteringResponse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JmsTechnicalException;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;

@Slf4j
@Component
public class Sdist003Service {

	private static final String DPI_KVITTERING_COUNTER = "dpi_antall_kvittering_counter";

	private final DpiClient dpiClient;
	private final ProducerTemplate producerTemplate;
	private final Queue qdist014;
	private final MeterRegistry meterRegistry;
	private final LagreJuridiskLoggService juridiskLoggService;
	private final ObjectMapper dpiObjectMapper;

	@Autowired
	public Sdist003Service(DpiClient dpiClient,
						   ProducerTemplate producerTemplate,
						   MeterRegistry meterRegistry,
						   Queue qdist014,
						   LagreJuridiskLoggService juridiskLoggService,
						   @Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper) {
		this.dpiClient = dpiClient;
		this.producerTemplate = producerTemplate;
		this.qdist014 = qdist014;
		this.meterRegistry = meterRegistry;
		this.juridiskLoggService = juridiskLoggService;
		this.dpiObjectMapper = dpiObjectMapper;
	}

	@Handler
	public List<HentKvitteringResponse> hentKvitteringOgBekreft(Exchange exchange) {
		List<HentKvitteringResponse> kvitteringer = dpiClient.hentKvitteringer();
		if (kvitteringer.isEmpty()) {
			exchange.setProperty(Exchange.SCHEDULER_POLLED_MESSAGES, false);
			return kvitteringer;
		}

		log.info("Sdist003 Hentet totalt={} kvitteringer fra DPI", kvitteringer.size());

		kvitteringer.stream()
				.map(this::getForretningsmeldingFromJwt)
				.forEach(forretningsmeldingPayload -> {
					try {
						SimpleStandardBusinessDocument simpleSbd = dpiObjectMapper.readValue(forretningsmeldingPayload, SimpleStandardBusinessDocument.class);
						KvitteringType kvitteringType = getKvitteringType(simpleSbd);
						log.info("Sdist003 har mottatt kvittering fra dpi aksesspunkt med konversasjonId={} og status={}", simpleSbd.getConversationId(), kvitteringType);
						producerTemplate.sendBody("jms:" + qdist014.getQueueName(), forretningsmeldingPayload);
						log.info("Sdist003 har skrevet melding på qdist014 med konversasjonId={}", simpleSbd.getConversationId());

						juridiskLoggService.lagreJuridiskLogg(new JuridiskLoggMetadata(simpleSbd.getDokumentKonversasjonId(), simpleSbd.getSender(), simpleSbd.getReceiver()), forretningsmeldingPayload);

						countDpiKvittering(kvitteringType);

						dpiClient.bekreft(simpleSbd.getDokumentKonversasjonId());
					} catch (JMSException e) {
						throw new JmsTechnicalException("Kunne ikke skrive melding til qdist014", e);
					} catch (JsonProcessingException e) {
						throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
					}
				});
		return kvitteringer;
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
				"kvitteringStatus", kvitteringType.name()).increment();
	}
}
