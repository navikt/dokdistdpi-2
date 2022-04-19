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
@ConfigurationProperties("dpi")
@Validated
public class DpiClientProperties {
	@NotEmpty
	private String url;
	@NotEmpty
	private String mpckanal;
	@Positive
	private long pullinterval;
	private boolean autostartup;
	@Positive
	private int dpischeduler;
}
