package no.nav.dokdistdpi.azure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/**
 * Konfigurert av naiserator. https://doc.nais.io/security/auth/azure-ad/#runtime-variables-credentials
 */
@Data
@ConfigurationProperties("azure")
@Validated
public class AzureProperties {
	@NotEmpty
	private String appClientId;
	@NotEmpty
	private String appClientSecret;
	@NotEmpty
	private String openidConfigTokenEndpoint;
}
