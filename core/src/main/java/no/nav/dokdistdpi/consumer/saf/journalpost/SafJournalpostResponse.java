package no.nav.dokdistdpi.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class SafJournalpostResponse {
	private final String journalpostId;
	private final Sak sak;
	private final String opprettetAvNavn;
	private final Bruker bruker;
	private final LocalDateTime datoOpprettet;
	private final String tittel;
	private final String journalfortAvNavn;
	private final String temanavn;
	private final String journalposttype;
	private final String journalfoerendeEnhet;
	private final AvsenderMottaker avsenderMottaker;

	@Builder.Default
	private final List<RelevantDato> relevanteDatoer = new ArrayList<>();

	@Builder.Default
	private final List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class AvsenderMottaker {
		private final String navn;
	}

	@Value
	@Builder
	public static class Sak {
		private final String arkivsaksnummer;
		private final LocalDateTime datoOpprettet;
	}

	@Value
	@Builder
	public static class RelevantDato {
		private final LocalDateTime dato;
		private final String datotype;
	}

	@Value
	@Builder
	public static class Bruker {
		private final String id;
		private final String type;
	}

	@Value
	@Builder
	public static class DokumentInfo {
		private final String dokumentInfoId;
		private final String dokumentstatus;
		private final String tittel;
		private final String originalJournalpostId;
		@Builder.Default
		private final List<Dokumentvariant> dokumentvarianter = new ArrayList<>();

		@Value
		@Builder
		public static class Dokumentvariant {
			private final String variantformat;
			private final String filtype;
		}
	}
}
