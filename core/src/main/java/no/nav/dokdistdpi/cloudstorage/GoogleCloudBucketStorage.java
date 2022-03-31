package no.nav.dokdistdpi.cloudstorage;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.crypto.tink.Aead;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.exception.technical.BucketFailedToDownloadTechnicalException;

import java.security.GeneralSecurityException;

@Slf4j
public class GoogleCloudBucketStorage implements BucketStorage {

	private final String bucket;
	private final Storage storage;
	private final Aead aead;

	public GoogleCloudBucketStorage(Storage storage, String bucket, Aead aead) {
		this.storage = storage;
		this.bucket = bucket;
		this.aead = aead;
	}

	@Override
	public String downloadObject(String objectName, String associatedData) {
		try {
			byte[] cipherText = storage.readAllBytes(bucket, objectName);
			byte[] plainText = aead.decrypt(cipherText, associatedData.getBytes());

			return new String(plainText);
		} catch (GeneralSecurityException | StorageException e) {
			throw new BucketFailedToDownloadTechnicalException(String.format("Teknisk feil mot Google Cloud Storage ved henting på objectName=%s. Feilmelding=%s",
					objectName, e.getMessage()), e);
		}
	}
}
