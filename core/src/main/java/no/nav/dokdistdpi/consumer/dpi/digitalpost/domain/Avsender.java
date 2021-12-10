package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Avsender {
	private String virksomhetsidentifikator;
	private String avsenderindentifikator;
	private String fakturaReferanse;
}
