package no.nav.dokdistdpi.exception.functional;

public class KunneIkkeDistribuereForsendelseException extends AbstractDokdistdpiFunctionalException {
	public KunneIkkeDistribuereForsendelseException(String message) {
		super(message);
	}

	public KunneIkkeDistribuereForsendelseException(String message, Throwable cause) {
		super(message, cause);
	}
}
