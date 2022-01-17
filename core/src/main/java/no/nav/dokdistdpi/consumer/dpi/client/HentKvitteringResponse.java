package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HentKvitteringResponse {
	private String forretningsmelding;
	private String downloadurl;
}
