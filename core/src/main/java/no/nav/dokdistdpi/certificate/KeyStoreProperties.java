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
	@NonNull
	private String type = KeyStore.getDefaultType();
	@NonNull
	private String alias;
	@NonNull
	private Resource path;
	@NonNull
	private String password;
	private Boolean lockProvider = false;
}
