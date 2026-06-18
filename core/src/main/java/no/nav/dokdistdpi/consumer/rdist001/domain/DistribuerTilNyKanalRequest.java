package no.nav.dokdistdpi.consumer.rdist001.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DistribuerTilNyKanalRequest(
		long forsendelseId,
		String arsak,
		String arsakBeskrivelse) {

	public static final String MELDINGSFEIL = "MELDINGSFEIL";
	public static final String ARSAK_MOTTAKER_HAR_IKKE_DIGITAL_POSTKASSE = "Mottaker har ikke Digital postkasse til innbygger";
	private static final String PRINT = "PRINT";

	@JsonProperty
	public String kanal() {
		return PRINT;
	}

	public static DistribuerTilNyKanalRequest arsakManglendeDigitalPostkasse(long forsendelseId) {
		return DistribuerTilNyKanalRequest.builder()
				.forsendelseId(forsendelseId)
				.arsak(MELDINGSFEIL)
				.arsakBeskrivelse(ARSAK_MOTTAKER_HAR_IKKE_DIGITAL_POSTKASSE)
				.build();
	}
}
