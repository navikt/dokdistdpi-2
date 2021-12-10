package no.nav.dokdistdpi.certificate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.security.KeyStore;

@Data
@ConfigurationProperties("virksomhetssertifikat")
@ToString(exclude = "password")
@Validated
@NoArgsConstructor
public class KeyStoreProperties {

	/**
	 * Type of KeyStore
	 * <p>
	 * Examples: JKS, Windows-MY
	 */
	@NonNull
	private String type = KeyStore.getDefaultType();

	/**
	 * Keystore alias for key.
	 */
	@NonNull
	private String alias;

	/**
	 * Path of jks file.
	 * <p>
	 * May be empty if type = Windows-MY
	 */
	@NonNull
	private Resource path;

	/**
	 * Password of keystore and entry.
	 */
	@NonNull
	private String password = "";

	/**
	 * True if the application should only use the Provider from the
	 * keyStore for crypto operations on the keys from the keystore.
	 */
	private Boolean lockProvider = false;

}
