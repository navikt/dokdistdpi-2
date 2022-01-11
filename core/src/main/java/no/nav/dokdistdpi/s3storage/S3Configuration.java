package no.nav.dokdistdpi.s3storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.AwsSystemPropertyRegionProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClientV2Builder;
import com.amazonaws.services.s3.model.CryptoConfigurationV2;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import no.nav.dokdistdpi.exception.functional.InvalidS3StorageSecretKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static com.amazonaws.regions.Regions.US_EAST_1;

@Configuration
@Profile({"nais", "local"})
public class S3Configuration {

	public static final String BUCKET_NAME = "dokdistmellomlager";

	private SecretKey secretKey;

	@Value("${dokdistdpi_s3_username}")
	private String credsUsername;

	@Value("${dokdistdpi_s3_password}")
	private String credsPassword;

	@Value("${s3_storage_url}")
	private String s3Endpoint;

	@Value("${dokdistdpi_s3_crypto_password}")
	private String encryptionPassphrase;

	@Bean
	public AwsRegionProvider awsRegionProvider() {
		System.setProperty("aws.region", "us-east-1");
		return new AwsSystemPropertyRegionProvider();
	}

	@Bean
	public Storage awsStorage() {
		secretKey = key(encryptionPassphrase);
		AmazonS3 s3 = s3(secretKey);
		return new AmazonS3Storage(s3);
	}

	private AmazonS3 s3(SecretKey secretKey) {
		awsRegionProvider();
		AWSCredentials credentials = new BasicAWSCredentials(credsUsername, credsPassword);
		return AmazonS3EncryptionClientV2Builder.standard()
				.enablePathStyleAccess()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Endpoint, US_EAST_1.getName()))
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withCryptoConfiguration(new CryptoConfigurationV2().withCryptoMode(CryptoMode.AuthenticatedEncryption))
				.withEncryptionMaterialsProvider(new StaticEncryptionMaterialsProvider(new EncryptionMaterials(secretKey)))
				.build();
	}

	private SecretKey key(String passphrase) {
		if (passphrase.getBytes().length != 32) {
			throw new InvalidS3StorageSecretKeyException("Passordet for s3Storage sin AES må være 256 bit");
		}
		return new SecretKeySpec(passphrase.getBytes(), "AES");
	}

}
