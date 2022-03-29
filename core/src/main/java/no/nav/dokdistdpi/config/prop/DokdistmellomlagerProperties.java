package no.nav.dokdistdpi.config.prop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Getter
@Setter
@ConfigurationProperties("dokdistmellomlager")
@Validated
public class DokdistmellomlagerProperties {
	@NonNull
	private String projectid;
	@NonNull
	private String bucket;
	@NonNull
	private String keyring;
	@NonNull
	private String keyid;

	public String gcpKekUri() {
		return "gcp-kms://projects/" + projectid + "/locations/europe-north1/keyRings/" + keyring + "/cryptoKeys/" + keyid;
	}
}
