package no.nav.dokdistdpi.config.prop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Getter
@Setter
@ConfigurationProperties("dokdistmellomlager")
@Validated
public class DokdistmellomlagerProperties {
	@NotEmpty
	private String projectid;
	@NotEmpty
	private String bucket;
	@NotEmpty
	private String keyring;
	@NotEmpty
	private String keyid;

	public String gcpKekUri() {
		return "gcp-kms://projects/" + projectid + "/locations/europe-north1/keyRings/" + keyring + "/cryptoKeys/" + keyid;
	}
}
