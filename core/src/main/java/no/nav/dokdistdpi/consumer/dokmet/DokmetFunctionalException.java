package no.nav.dokdistdpi.consumer.dokmet;

import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;

public class DokmetFunctionalException extends AbstractDokdistdpiFunctionalException {
	public DokmetFunctionalException(String message) {
		super(message);
	}

	public DokmetFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
