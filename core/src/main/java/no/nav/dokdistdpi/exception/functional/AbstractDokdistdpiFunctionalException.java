package no.nav.dokdistdpi.exception.functional;

public abstract class AbstractDokdistdpiFunctionalException extends RuntimeException {

	protected AbstractDokdistdpiFunctionalException(String message) {
		super(message);
	}

	protected AbstractDokdistdpiFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
