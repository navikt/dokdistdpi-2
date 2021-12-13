package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestItem {
	protected String mimeTypeQualifierCode;
	protected String uniformResourceIdentifier;
	protected String description;
	protected String languageCode;
}
