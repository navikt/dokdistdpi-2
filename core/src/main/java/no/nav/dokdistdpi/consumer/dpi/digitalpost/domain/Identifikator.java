package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Identifikator {
	private Authority authority;
	private String value;
}
