package no.nav.dokdistdpi.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

/**
 * Konfigurasjon for Google Cloud Storage bucket
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Data
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
