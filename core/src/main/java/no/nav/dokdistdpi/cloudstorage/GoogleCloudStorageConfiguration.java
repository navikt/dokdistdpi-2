package no.nav.dokdistdpi.cloudstorage;

import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplate;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.KmsEnvelopeAeadKeyManager;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistmellomlagerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Configuration
@Profile("nais")
public class GoogleCloudStorageConfiguration {

	public static final String KEYTEMPLATE = "AES128_GCM";

	@Bean
	@Lazy
	public BucketStorage storage(
			DokdistmellomlagerProperties dokdistmellomlagerProperties
	) throws Exception {
		final String kekUri = dokdistmellomlagerProperties.gcpKekUri();

		AeadConfig.register();
		GcpKmsClient.register(Optional.of(kekUri), Optional.empty());
		KeyTemplate keyTemplate = KmsEnvelopeAeadKeyManager.createKeyTemplate(kekUri, KeyTemplates.get(KEYTEMPLATE));
		KeysetHandle handle = KeysetHandle.generateNew(keyTemplate);
		Aead aead = handle.getPrimitive(Aead.class);
		log.info("dokdistdpi-2 oppstart. Henter aead kryptering nøkkel. primaryKeyId={}", handle.getKeysetInfo().getPrimaryKeyId());
		Storage storage = StorageOptions.newBuilder()
				.setProjectId(dokdistmellomlagerProperties.getProjectid())
				.setTransportOptions(StorageOptions.getDefaultHttpTransportOptions().toBuilder()
						.setConnectTimeout((int) SECONDS.toMillis(5))
						.setReadTimeout((int) SECONDS.toMillis(20))
						.setHttpTransportFactory(ApacheHttpTransport::new)
						.build())
				.build().getService();
		return new GoogleCloudBucketStorage(storage, dokdistmellomlagerProperties.getBucket(), aead);
	}
}
