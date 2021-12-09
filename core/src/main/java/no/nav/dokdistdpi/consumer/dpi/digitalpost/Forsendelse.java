package no.nav.dokdistdpi.consumer.dpi.digitalpost;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;

@Data
@Builder
public class Forsendelse {
	private String mottakerSertifikat;
	private final String conversationId;
	private final String bestillingsId;
	private DigitalPostInfo digitalPostInfo;
	private Dokumentpakke dokumentpakke;
}
