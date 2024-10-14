package no.nav.dokdistdpi.config.prop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("mqgateway01")
@Validated
public class MqGatewayProperties {
	@NotEmpty
	private String hostname;
	@NotEmpty
	private String name;
	@Positive
	private int port;
	private MqChannel channel = new MqChannel();

	@Data
	@Validated
	public static class MqChannel {
		@NotBlank
		private String securename;
	}
}
