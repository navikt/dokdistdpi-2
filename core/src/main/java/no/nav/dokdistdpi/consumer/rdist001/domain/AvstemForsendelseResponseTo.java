package no.nav.dokdistdpi.consumer.rdist001.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Subsett av respons fra /henteuekspederforsendelse
 * Kun feltene denne appen behøver.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvstemForsendelseResponseTo {

	private String distribusjonId;
	private List<DokumentInfoTo> dokumenter;
	private String distribusjonKanal;
	private String distribusjonStatus;
	private String opprettetDato;
	private String distribusjonDato;

	@Data
	@Builder
	public static class DokumentInfoTo {
		private final String forsendelseId;
		private final String dokumentId;
		private final String dokumentStatus;
		private final String konversasjonId;
		private final String bestillendeFagsystem;
		private final String fagomradeCode;
		private final String journalpostId;
		private final String avstemtReferanse;
		private final String avstemtDato;
		private final String brevProduksjonApplikasjon;
	}
}
