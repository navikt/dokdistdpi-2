package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder(builderMethodName = "commonVarselBuilder")
public abstract class CommonVarsel {
	private String varslingstekst;
	private List<Integer> repetisjoner;
}
