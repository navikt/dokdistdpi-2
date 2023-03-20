package no.nav.dokdistdpi.exception.functional;

/**
 * Forsendelse status finnes ikke hos DPI hjørne2
 */
public class ForsendelseStatusIkkeFunnetException extends AbstractDokdistdpiFunctionalException {
	public ForsendelseStatusIkkeFunnetException(String message, Throwable cause) {
		super(message, cause);
	}
}
