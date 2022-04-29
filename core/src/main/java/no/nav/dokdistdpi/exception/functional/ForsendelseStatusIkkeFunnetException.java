package no.nav.dokdistdpi.exception.functional;

/**
 * Forsendelse status finnes ikke hos DPI hjørne2
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
public class ForsendelseStatusIkkeFunnetException extends AbstractDokdistdpiFunctionalException {
	public ForsendelseStatusIkkeFunnetException(String message, Throwable cause) {
		super(message, cause);
	}
}
