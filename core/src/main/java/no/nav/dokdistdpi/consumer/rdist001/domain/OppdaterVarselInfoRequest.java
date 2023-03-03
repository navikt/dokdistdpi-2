package no.nav.dokdistdpi.consumer.rdist001.domain;


import java.util.Set;

public record OppdaterVarselInfoRequest(
		String forsendelseId,
		Set<Notifikasjon> notifikasjoner
) {

}
