package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;

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
	private Integer sikkerhetsnivaa;
	/*Dato for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDate virkningsdato;

	/*Dato og tidspunkt for når en melding skal tilgjengeliggjøres for Innbygger i Innbygger sin postkasse.*/
	private LocalDateTime virkningstidspunkt;

	private boolean aapningskvittering;
	private String ikkesensitivtittel;
	private String spraak;
	private Varsler varsler;

	@Data
	@Builder
	public static class Personmottaker {
		private String postkasseadresse;
	}
}
