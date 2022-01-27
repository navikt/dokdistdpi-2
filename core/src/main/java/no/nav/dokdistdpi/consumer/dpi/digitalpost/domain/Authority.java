package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Authority {
	/*For orgnr*/
	ISO_6523_ACTORID_UPIS("iso6523-actorid-upis"),
	/*for personnummer*/
	ISO_3166_1_ALFA2("iso3166-1-alfa2");
	private String value;
}
