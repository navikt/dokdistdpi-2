package no.nav.dokdistdpi.sdist003;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.consumer.juridisklogg.JuridiskLoggConsumer;
import no.nav.dokdistdpi.consumer.juridisklogg.LoggMeldingRequest;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;

@Slf4j
@Component
public class LagreJuridiskLoggService {

	static final Integer ANTALL_AAR_LAGRES = 10;
	private final JuridiskLoggConsumer juridiskLoggConsumer;
	private final ObjectMapper objectMapper;

	@Autowired
	public LagreJuridiskLoggService(JuridiskLoggConsumer juridiskLoggConsumer, @Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper) {
		this.juridiskLoggConsumer = juridiskLoggConsumer;
		this.objectMapper = dpiObjectMapper;
	}

	public void lagreJuridiskLogg(String payload) {
		try {
			SimpleStandardBusinessDocument simpleSbd = objectMapper.readValue(payload, SimpleStandardBusinessDocument.class);
			juridiskLoggConsumer.lagreJuridiskLogg(map(payload, simpleSbd));
			log.info("Hendelse med konversasjonsId={} logget til juridisk arkiv.", simpleSbd.getKonversasjonId());
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);

		}
	}

	LoggMeldingRequest map(String payload, SimpleStandardBusinessDocument simpleSbd) {
		return LoggMeldingRequest.builder()
				.meldingsId(simpleSbd.getDokumentKonversasjonId())
				.avsender(simpleSbd.getSender())
				.mottaker(APP_NAME + "-" + simpleSbd.getReceiver())
				.joarkRef(null)
				.meldingsInnhold(payload.getBytes())
				.antallAarLagres(ANTALL_AAR_LAGRES)
				.build();
	}
}
