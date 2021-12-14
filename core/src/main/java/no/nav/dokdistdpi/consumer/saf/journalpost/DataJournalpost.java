package no.nav.dokdistdpi.consumer.saf.journalpost;

import java.io.Serializable;

public class DataJournalpost implements Serializable {
	private SafJournalpostResponse journalpost;
	public SafJournalpostResponse getJournalpost() {
		return journalpost;
	}

	public void setJournalpost(SafJournalpostResponse journalpost) {
		this.journalpost = journalpost;
	}
}
