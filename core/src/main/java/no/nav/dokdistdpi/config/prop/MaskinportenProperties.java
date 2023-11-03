package no.nav.dokdistdpi.config.prop;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("maskinporten")
public class MaskinportenProperties {
	@NotEmpty
	private String issuer;
	@NotEmpty
	private String clientId;
	@NotEmpty
	private String scopes;
	@NotEmpty
	private String tokenEndpoint;
	@NotEmpty
	private String clientJwk;
	private String wellKnownUrl;
}
