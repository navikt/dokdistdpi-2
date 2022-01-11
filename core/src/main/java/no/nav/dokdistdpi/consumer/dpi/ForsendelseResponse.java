package no.nav.dokdistdpi.consumer.dpi;

import lombok.Builder;
import lombok.Value;


@Value
@Builder
public class ForsendelseResponse {
	private String type;
	private String title;
	private int status;
	private String detail;
	private String instance;
}
