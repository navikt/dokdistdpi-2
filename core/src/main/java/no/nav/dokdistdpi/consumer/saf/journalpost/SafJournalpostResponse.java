package no.nav.dokdistdpi.consumer.saf.journalpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafJournalpostResponse {
	private String journalpostId;
	private Sak sak;
	private String opprettetAvNavn;
	private Bruker bruker;
	private LocalDateTime datoOpprettet;
	private String tittel;
	private String journalfortAvNavn;
	private String temanavn;
	private String journalposttype;
	private String journalfoerendeEnhet;
	private AvsenderMottaker avsenderMottaker;

	@Builder.Default
	private List<RelevantDato> relevanteDatoer = new ArrayList<>();

	@Builder.Default
	private List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class AvsenderMottaker {
		private String navn;
	}

	@Value
	@Builder
	public static class Sak {
		private String arkivsaksnummer;
		private LocalDateTime datoOpprettet;
	}

	@Value
	@Builder
	public static class RelevantDato {
		private LocalDateTime dato;
		private String datotype;
	}

	@Value
	@Builder
	public static class Bruker {
		private String id;
		private String type;
	}

	@Value
	@Builder
	public static class DokumentInfo {
		private String dokumentInfoId;
		private String dokumentstatus;
		private String tittel;
		private String originalJournalpostId;
		@Builder.Default
		private List<Dokumentvariant> dokumentvarianter = new ArrayList<>();

		@Value
		@Builder
		public static class Dokumentvariant {
			private String variantformat;
			private String filtype;
		}
	}
}
