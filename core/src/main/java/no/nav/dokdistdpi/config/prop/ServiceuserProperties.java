package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("serviceuser")
@Validated
public class ServiceuserProperties {
	@NotEmpty
	private String username;
	@NotEmpty
	@ToString.Exclude
	private String password;
}
