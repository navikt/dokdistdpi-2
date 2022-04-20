package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("dpi")
@Validated
public class DpiClientProperties {
	@NotEmpty
	private String url;
	@NotEmpty
	private String mpckanal;
}
