package no.nav.dokdistdpi.consumer.dokkat.tkat20;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DokumenttypeInfoTo {
	private final String varselTypeId;
	private int sikkerhetsnivaa;
}
