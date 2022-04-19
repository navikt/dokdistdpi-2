package no.nav.dokdistdpi.config.prop;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URL;

@Getter
@Setter
@ToString
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
