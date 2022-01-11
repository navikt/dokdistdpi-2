package no.nav.dokdistdpi.exception.technical;

public abstract class AbstractDokdistdpiTechnicalException extends RuntimeException {

	protected AbstractDokdistdpiTechnicalException(String message) {
		super(message);
	}

	protected AbstractDokdistdpiTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

}
