package no.nav.dokdistdpi.consumer.saf.journalpost;

import java.io.Serializable;

public class SafJsonJournalpost implements Serializable {
	private DataJournalpost data;

	public DataJournalpost getData() {
		return data;
	}

	public void setData(DataJournalpost data) {
		this.data = data;
	}

	public SafJournalpostResponse getJournalpost() {
		return data.getJournalpost();
	}
}
