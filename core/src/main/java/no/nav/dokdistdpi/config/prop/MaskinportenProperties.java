package no.nav.dokdistdpi.config.prop;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * https://doc.nais.io/security/auth/maskinporten/client/
 */
@Validated
@ConfigurationProperties("maskinporten")
public record MaskinportenProperties(@NotBlank String scopes) {
}
