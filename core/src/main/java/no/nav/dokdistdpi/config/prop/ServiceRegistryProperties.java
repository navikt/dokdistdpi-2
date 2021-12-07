package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

@Data
@ToString
@ConfigurationProperties("serviceregistry")
@Validated
public class ServiceRegistryProperties {
	@NonNull
	private URL url;
}
