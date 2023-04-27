package no.nav.dokdistdpi.consumer.rdist001.domain;


import java.util.List;

public record OppdaterVarselInfoRequest(
		Long forsendelseId,
		List<Notifikasjon> notifikasjoner
) {

}
