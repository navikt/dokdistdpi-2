package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URL;

@Data
@ConfigurationProperties("maskinporten")
@Validated
public class MaskinportenProperties {
	@NotNull
	private URL url;
	@NotEmpty
	private String audience;
	@NotEmpty
	private String clientid;
}
