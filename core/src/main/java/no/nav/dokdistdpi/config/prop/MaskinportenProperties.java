package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

@Data
@ToString
@ConfigurationProperties("maskinporten")
@Validated
public class MaskinportenProperties {
	@NonNull
	private URL url;
	@NonNull
	private String audience;
	@NonNull
	private String clientid;
}
