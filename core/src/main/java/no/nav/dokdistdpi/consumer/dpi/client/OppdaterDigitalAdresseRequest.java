package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;

@Data
@Builder
public class OppdaterDigitalAdresseRequest {
	private StatusType status;
	private String forsendelseId;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
	private Varsler varsler;
	private DistribusjonsTypeKode distribusjonsTypeKode;
}
