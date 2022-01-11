package no.nav.dokdistdpi.config.prop;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ToString
@ConfigurationProperties("mqserviceuser")
@Validated
public class MqServiceuserProperties {
	@NonNull
	private String username;
	@NonNull
	private String password;
}
