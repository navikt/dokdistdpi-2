package no.nav.dokdistdpi.consumer.rdist001;

import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class AdministrerForsendelseMapper {

	public String map(HentMottakerResponse hentMottakerResponse) {
		if (isNull(hentMottakerResponse) || isNull(hentMottakerResponse.getMottaker())) {
			throw new AdminstrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}
		HentMottakerResponse.MottakerTo mottakerTo = hentMottakerResponse.getMottaker();
		return requireNonNull(mottakerTo.getMottakerId(), "Person ident kan ikke være null");
	}
}
