package no.nav.dokdistdpi.consumer.dokmet.tkat21;

import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokmet.api.tkat021.VarselInfoTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;

public class VarselInfoMapper {

	public static VarselInfo mapVarselInfo(final VarselInfoTo response) {
		return isNull(response) ? null : VarselInfo.builder()
				.varselTypeId(response.getVarseltypeId())
				.stoppRepeterendeVarsel(response.getRevarslingIntervall() != null)
				.antallDagerListe(toDagerListe(response))
				.varslingsTekst(getVarslingsTekst(response))
				.preferertKanal(response.getPreferertKanal())
				.build();
	}

	private static Map<String, String> getVarslingsTekst(VarselInfoTo varselInfoRestTo) {
		Map<String, String> varslingsTekst = new HashMap<>();
		varselInfoRestTo.getVarselmals().forEach(
				varselMalRestTo -> varslingsTekst.put(varselMalRestTo.getKanal(), varselMalRestTo.getFoerstegangsvarselTekst()));
		return varslingsTekst;
	}

	private static List<Integer> toDagerListe(VarselInfoTo varselInfoRestTo) {
		List<Integer> antallDagerListe = new ArrayList<>();
		antallDagerListe.add(0);
		IntStream.range(0, varselInfoRestTo.getAntallRevarslinger())
				.forEach(i ->
						antallDagerListe.add(varselInfoRestTo.getRevarslingIntervall() * (i + 1))

				);
		return antallDagerListe;
	}
}
