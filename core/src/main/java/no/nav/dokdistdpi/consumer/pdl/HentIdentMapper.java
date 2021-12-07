package no.nav.dokdistdpi.consumer.pdl;

import no.nav.dokdistdpi.exception.functional.PdlFunctionalException;

import static java.util.Objects.isNull;

public class HentIdentMapper {

	public String map(HentIdentResponse hentIdentResponse) {
		if (isNull(hentIdentResponse.getData()) || isNull(hentIdentResponse.getData().getIdenter())) {
			throw new PdlFunctionalException("Identer data kan ikke være null");
		}
		return hentIdentResponse.getData().getIdenter().stream().map(HentIdentResponse.IdentInfo::getIdent).findFirst()
				.orElseThrow(()-> new PdlFunctionalException("Ident kan ikke være null"));

	}
}
