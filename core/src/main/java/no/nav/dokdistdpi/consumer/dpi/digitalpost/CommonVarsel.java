package no.nav.dokdistdpi.consumer.dpi.digitalpost;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(builderMethodName = "commonVarsel")
public abstract class CommonVarsel {
	private String varslingstekst;
	private Repetisjoner repetisjoner;

	@Data
	@Builder
	public static class Repetisjoner {
		private List<Integer> dagerEtters;
	}
}
