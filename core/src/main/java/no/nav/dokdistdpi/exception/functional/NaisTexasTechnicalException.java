package no.nav.dokdistdpi.exception.functional;

import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;

public class NaisTexasTechnicalException extends AbstractDokdistdpiTechnicalException {
	public NaisTexasTechnicalException(String message) {
		super(message);
	}

	public NaisTexasTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
