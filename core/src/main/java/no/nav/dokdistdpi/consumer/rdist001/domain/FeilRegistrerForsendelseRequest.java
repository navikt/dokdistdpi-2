package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeilRegistrerForsendelseRequest {
	private String forsendelseId;
	private String type;
	private String part;
	private LocalDateTime tidspunkt;
	private String detaljer;
	private String resendingDistribusjonId;
}
