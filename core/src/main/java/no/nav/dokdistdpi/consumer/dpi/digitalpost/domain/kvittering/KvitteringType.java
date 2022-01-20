package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum KvitteringType {
	AAPNING("aapningskvittering"),
	VARSLINGFEILET("varslingfeiletkvittering"),
	LEVERING("leveringskvittering"),
	MOTTAK("mottakskvittering"),
	FEILET("feil");

	private final String value;

	public static final KvitteringType getByValue(String value){
		return Arrays.stream(KvitteringType.values())
				.filter(kvitteringType -> kvitteringType.getValue().equals(value))
				.findFirst().orElse(null);
	}
}
