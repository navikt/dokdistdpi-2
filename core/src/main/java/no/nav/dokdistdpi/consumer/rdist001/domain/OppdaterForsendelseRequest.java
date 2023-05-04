package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppdaterForsendelseRequest {
	private Long forsendelseId;
	private String forsendelseStatus;
	private String konversasjonId;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
