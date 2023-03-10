package no.nav.dokdistdpi.consumer.rdist001.domain;


import java.util.List;

public record OppdaterVarselInfoRequest(
		String forsendelseId,
		List<Notifikasjon> notifikasjoner
) {

}
