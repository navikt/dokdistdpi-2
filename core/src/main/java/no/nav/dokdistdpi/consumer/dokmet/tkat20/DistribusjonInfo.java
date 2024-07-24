package no.nav.dokdistdpi.consumer.dokmet.tkat20;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DistribusjonInfo {
	private final String varselTypeId;
	private int sikkerhetsnivaa;
}
