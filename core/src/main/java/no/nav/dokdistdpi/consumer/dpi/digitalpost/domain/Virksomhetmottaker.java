package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Virksomhetmottaker {
	private Identifikator virksomhetsidentifikator;
	private String motakeridentifikator;
}
