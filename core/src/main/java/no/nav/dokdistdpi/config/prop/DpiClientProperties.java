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
@ConfigurationProperties("dpi")
@Validated
public class DpiClientProperties {
	@NonNull
	private String url;
	@NonNull
	private String mpckanal;
}
