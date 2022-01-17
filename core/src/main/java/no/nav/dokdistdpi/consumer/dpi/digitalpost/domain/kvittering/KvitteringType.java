package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KvitteringType {
	AAPNING("aapningskvittering"),
	VARSLINGFEILET("varslingfeiletkvittering"),
	LEVERING("leveringskvittering"),
	MOTTAK("mottakskvittering"),
	FEILET("feil");

	private String type;
}
