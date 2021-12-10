package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Dokumentpakkefingeravtrykk {
	private String digestMethod;
	private String digestValue;
}
