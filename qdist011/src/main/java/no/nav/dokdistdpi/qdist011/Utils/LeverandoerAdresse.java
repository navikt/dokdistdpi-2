package no.nav.dokdistdpi.qdist011.Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LeverandoerAdresse {
	DIGIPOST("984661185"),
	EBOKS("922020175");

	private String value;

	public static LeverandoerAdresse getByValue(String value) {
		return Arrays.stream(LeverandoerAdresse.values())
				.filter(kvitteringType -> kvitteringType.getValue().equals(value))
				.findFirst().orElse(null);
	}
}
