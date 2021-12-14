package no.nav.dokdistdpi.s3storage;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistdpi.exception.technical.AWSS3FailedToGetDocumentTechnicalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import static no.nav.dokdistdpi.s3storage.S3Configuration.BUCKET_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;

public class AmazonS3Storage implements Storage{

	private AmazonS3 amazonS3;

	public AmazonS3Storage(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
	}

	@Override
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public String get(String key) {
		try {
			return amazonS3.getObjectAsString(BUCKET_NAME, key);
		} catch (SdkClientException e) {
			throw new AWSS3FailedToGetDocumentTechnicalException(String.format("Teknisk feil mot AmazonS3 ved henting på key=%s. Feilmelding=%s", key,
					e.getMessage()), e);
		} catch (SecurityException e) {
			throw new AWSS3FailedToGetDocumentTechnicalException(String.format("Objektet som ble forsøkt hentet fra AmazonS3 på key=%s var ikke kryptert.", key), e);
		}
	}
}
