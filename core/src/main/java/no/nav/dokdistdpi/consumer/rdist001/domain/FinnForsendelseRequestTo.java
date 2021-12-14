package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinnForsendelseRequestTo {
	private String oppslagsNoekkel;
	private String verdi;
}
