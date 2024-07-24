package no.nav.dokdistdpi.consumer.dokmet.tkat21;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class VarselInfo {
	String varselTypeId;
	boolean stoppRepeterendeVarsel;
	Map<String, String> varslingsTekst;
	List<Integer> antallDagerListe;
	Set<String> preferertKanal;
}
