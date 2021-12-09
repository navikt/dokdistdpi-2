package no.nav.dokdistdpi.consumer.dpi.digitalpost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@Builder
public class DigitalPostInfo {
	private Avsender avsender;
	private Virksomhetmottaker virksomhetmottaker;
	private String avsenderidentifikator;
	/*Person som er mottaker av en sikker digital post */
	private Personmottaker  personmottaker;
	private Identifikator virksomhetsidentifikator;
	private Identifikator personidentifikator;
	private String postkasseadresse;
	private String motakeridentifikator;
	private String maskinportentoken;
	private LocalDateTime tidspunkt;
	private Dokumentpakkefingeravtrykk dokumentpakkefingeravtrykk;
	private Kvittering kvittering;
	private Sikkerhetsnivaa sikkerhetsnivaa;
	/*Dato for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDate virkningsdato;
	/*Dato og tidspunkt for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDateTime virkningstidspunkt;
	private boolean aapningskvittering;
	private Varsler varsler;
	private String ikkesensitivtittel;


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
	public static class Identifikator {
		private Authority authority;
		private String value;
	}

	@Data
	@Builder
	public static class Dokumentpakkefingeravtrykk {
		private String digestMethod;
		private String digestValue;
	}

	@Getter
	@AllArgsConstructor
	public enum Authority {
		ISO_6523_ACTORID_UPIS("iso6523-actorid-upis"),
		ISO_3166_1_ALFA2("iso3166-1-alfa2");
		private String value;
	}

	@Getter
	@AllArgsConstructor
	public enum Sikkerhetsnivaa {
		NIVAA_3("3"),
		NIVAA_4("4");
		private String value;
	}
}
