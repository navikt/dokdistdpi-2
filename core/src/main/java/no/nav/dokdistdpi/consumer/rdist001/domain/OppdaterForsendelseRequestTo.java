package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OppdaterForsendelseRequestTo {
	private String forsendelseId;
	private String forsendelseStatus;
	private  String konversasjonId;
	private String varselStatus;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
