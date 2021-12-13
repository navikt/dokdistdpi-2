package no.nav.dokdistdpi.exception.functional;

public abstract class AbstractDokdistdpiFunctionalException extends RuntimeException {

	public AbstractDokdistdpiFunctionalException(Throwable cause) {
		super(cause);
	}
	public AbstractDokdistdpiFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistdpiFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
