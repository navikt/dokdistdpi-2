package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class Identifikator {
	private Authority authority;
	private String value;

	@Getter
	@AllArgsConstructor
	public enum Authority {
		ISO_6523_ACTORID_UPIS("iso6523-actorid-upis"),
		ISO_3166_1_ALFA2("iso3166-1-alfa2");
		private String value;
	}
}
