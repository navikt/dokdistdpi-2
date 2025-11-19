package no.nav.dokdistdpi.certificate;

import jakarta.validation.constraints.NotBlank;
import no.nav.dok.validators.Exists;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("nav.virksomhetssertifikat")
public record KeyStoreProperties(
		@NotBlank @Exists
		String key,
		@NotBlank @Exists
		String credentials
) {
}
