package no.nav.dokdistdpi.azure;

import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;

public class AzureTokenException extends AbstractDokdistdpiTechnicalException {
	public AzureTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
