package no.nav.dokdistdpi.qdist011.saf;

import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertFieldOnSafDokumenterNotNullOrEmpty;

@Component
public class SafJournalpostValidatorQdist011 {

	public void validate(SafJournalpostResponse safJournalpost, String journalpostid) {
		safJournalpost.getDokumenter().forEach(dokumentInfo -> validateDokument(dokumentInfo, journalpostid));
	}

	private void validateDokument(SafJournalpostResponse.DokumentInfo dokumentInfo, String journalpostId) {
		assertFieldOnSafDokumenterNotNullOrEmpty("dokumentInfo.tittel", dokumentInfo.getTittel(), journalpostId, dokumentInfo.getDokumentInfoId());
		assertFieldOnSafDokumenterNotNullOrEmpty("dokumentInfo.dokumentInfoId", dokumentInfo.getDokumentInfoId(), journalpostId, dokumentInfo
				.getDokumentInfoId());
	}
}
