package no.nav.dokdistdpi.consumer.dokkat.tkat21;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class VarselInfoTo {
	private final String varselTypeId;
	private final boolean stoppRepeterendeVarsel;
	private final Map<String, String> varslingsTekst;
	private final List<Integer> antallDagerListe;
	private final Set<String> preferertKanal;
}
