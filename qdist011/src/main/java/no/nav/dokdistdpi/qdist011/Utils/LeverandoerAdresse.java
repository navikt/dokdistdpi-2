package no.nav.dokdistdpi.qdist011.Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LeverandoerAdresse {
	DIGIPOST("984661185", "Digipost"),
	EBOKS("922020175", "eBoks");

	private final String organisasjonsnummer;
	private final String navn;

	public static LeverandoerAdresse findByOrganisasjonsnummer(String organisasjonsnummer) {
		return Arrays.stream(LeverandoerAdresse.values())
				.filter(leverandoerAdresse -> leverandoerAdresse.getOrganisasjonsnummer().equals(organisasjonsnummer))
				.findFirst().orElse(null);
	}
}
