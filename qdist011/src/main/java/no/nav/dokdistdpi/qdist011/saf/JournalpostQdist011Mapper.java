package no.nav.dokdistdpi.qdist011.saf;

import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import org.springframework.stereotype.Component;

@Component
public class JournalpostQdist011Mapper {
	public JournalpostQdist011 map(SafJournalpostResponse safJournalpost) {
		return JournalpostQdist011.builder()
				.dokumenter(safJournalpost.getDokumenter()
						.stream()
						.map(dokumentInfo -> JournalpostQdist011.DokumentInfo.builder()
								.dokumentInfoId(dokumentInfo.getDokumentInfoId())
								.tittel(dokumentInfo.getTittel())
								.build())
						.toList())
				.build();
	}
}
