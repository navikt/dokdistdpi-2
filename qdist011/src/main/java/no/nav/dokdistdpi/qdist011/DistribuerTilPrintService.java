package no.nav.dokdistdpi.qdist011;

import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribuerTilNyKanalRequest;
import no.nav.dokdistdpi.exception.functional.AdministrerForsendelseFunctionalException;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

@Component
public class DistribuerTilPrintService {

	private final DokdistadminConsumer dokdistadminConsumer;

	public DistribuerTilPrintService(DokdistadminConsumer dokdistadminConsumer) {
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	@Handler
	public void sendForsendelseTilPrint(String forsendelseId) {
		try {
			long id = Long.parseLong(forsendelseId);
			dokdistadminConsumer.distribuerTilNyKanal(DistribuerTilNyKanalRequest.arsakMeldingsfeil(id));
		} catch (NumberFormatException e) {
			throw new AdministrerForsendelseFunctionalException("Ugyldig forsendelseId for distribusjon til print: " + forsendelseId, e);
		}
	}
}
