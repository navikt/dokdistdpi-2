package no.nav.dokdistdpi.consumer.saf.journalpost;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SafJsonJournalpost implements Serializable {
	private DataJournalpost data;

	private List<Error> errors;

	@Data
	public static class DataJournalpost {
		private SafJournalpostResponse journalpost;
	}

	@Data
	@JsonIgnoreProperties({"locations", "path"})
	public static class Error {
		private String message;
		private Extension extensions;
	}

	@Data
	public static class Extension {
		private String code;
		private String classification;
	}
}
