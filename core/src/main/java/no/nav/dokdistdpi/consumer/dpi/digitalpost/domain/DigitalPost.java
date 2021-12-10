package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@Builder
public class DigitalPost {
	private Avsender avsender;
	/*Person som er mottaker av en sikker digital post */
	private Personmottaker mottaker;
	private Dokumentpakkefingeravtrykk dokumentpakkefingeravtrykk;
	private String maskinportentoken;
	private Sikkerhetsnivaa sikkerhetsnivaa;
	/*Dato for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDate virkningsdato;

	/*Dato og tidspunkt for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDateTime virkningstidspunkt;

	private boolean aapningskvittering;
	private String ikkesensitivtittel;
	private String spraak;
	private Varsler varsler;

	private Kvittering kvittering;



	@Data
	@Builder
	public static class Avsender {
		private Identifikator virksomhetsidentifikator;
		private String avsenderindentifikator;
		private String fakturaReferanse;
	}

	@Data
	@Builder
	public static class Kvittering {
		private Avsender avsender;
		private Virksomhetmottaker virksomhetmottaker;
		private LocalDateTime tidspunkt;
	}

	@Data
	@Builder
	public static class Personmottaker {
		private String postkasseadresse;
	}

	@Data
	@Builder
	public static class Dokumentpakkefingeravtrykk {
		private String digestMethod;
		private String digestValue;
	}

	@Getter
	@AllArgsConstructor
	public enum Sikkerhetsnivaa {
		NIVAA_3("3"),
		NIVAA_4("4");
		private String value;
	}
}
