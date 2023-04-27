package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.rdist001.kodeverk.VarselStatusCode;

@Data
@Builder
public class OppdaterForsendelseRequest {
	private Long forsendelseId;
	private String forsendelseStatus;
	private String konversasjonId;
	private VarselStatusCode varselStatus;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
