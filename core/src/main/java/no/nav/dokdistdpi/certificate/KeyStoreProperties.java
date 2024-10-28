package no.nav.dokdistdpi.certificate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.security.KeyStore;

@Data
@ConfigurationProperties("virksomhetssertifikat")
@ToString(exclude = "password")
@Validated
@NoArgsConstructor
public class KeyStoreProperties {
	@NotNull
	private String type = KeyStore.getDefaultType();
	@NotNull
	private String alias;
	@NotNull
	private Resource path;
	@NotNull
	private String password;
	private Boolean lockProvider = false;
}
