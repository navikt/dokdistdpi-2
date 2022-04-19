package no.nav.dokdistdpi.config.prop;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;

@Getter
@Setter
@ToString
@ConfigurationProperties("mqgateway01")
@Validated
public class MqGatewayProperties {
	@NotEmpty
	private String hostname;
	@NotEmpty
	private String name;
	@Positive
	private int port;
	@NotEmpty
	private String channelname;
}
