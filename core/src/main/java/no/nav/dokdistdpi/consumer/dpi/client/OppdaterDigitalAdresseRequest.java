package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppdaterDigitalAdresseRequest {
	private StatusType status;
	private String forsendelseId;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
