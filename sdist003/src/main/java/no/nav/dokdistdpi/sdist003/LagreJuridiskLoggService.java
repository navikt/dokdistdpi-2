package no.nav.dokdistdpi.sdist003;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.juridisklogg.JuridiskLoggConsumer;
import no.nav.dokdistdpi.consumer.juridisklogg.LoggMeldingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;

@Slf4j
@Component
public class LagreJuridiskLoggService {

	static final Integer ANTALL_AAR_LAGRES = 10;
	private final JuridiskLoggConsumer juridiskLoggConsumer;

	@Autowired
	public LagreJuridiskLoggService(JuridiskLoggConsumer juridiskLoggConsumer) {
		this.juridiskLoggConsumer = juridiskLoggConsumer;
	}

	public void lagreJuridiskLogg(JuridiskLoggMetadata juridiskLoggMetadata, String payload) {
		juridiskLoggConsumer.lagreJuridiskLogg(map(juridiskLoggMetadata, payload));
		log.info("Hendelse med konversasjonsId={} logget til juridisk arkiv.", juridiskLoggMetadata.meldingsId());
	}

	LoggMeldingRequest map(JuridiskLoggMetadata juridiskLoggMetadata, String payload) {
		return LoggMeldingRequest.builder()
				.meldingsId(juridiskLoggMetadata.meldingsId())
				.avsender(juridiskLoggMetadata.avsender())
				.mottaker(APP_NAME + "-" + juridiskLoggMetadata.mottaker())
				.joarkRef(null)
				.meldingsInnhold(payload.getBytes())
				.antallAarLagres(ANTALL_AAR_LAGRES)
				.build();
	}
}
