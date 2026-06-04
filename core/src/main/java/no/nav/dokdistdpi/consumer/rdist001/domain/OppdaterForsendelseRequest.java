package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OppdaterForsendelseRequest {
	private Long forsendelseId;
	private String forsendelseStatus;
	LocalDateTime ekspedertDato;
	private String konversasjonId;
	private String digitalLeverandoeradresse;
	private String digitalPostkasseadresse;
}
