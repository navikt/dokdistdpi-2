package no.nav.dokdistdpi.consumer.dpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForsendelseResponse {
	private String type;
	private String title;
	private int status;
	private String detail;
	private String instance;
}
