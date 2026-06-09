package no.nav.dokdistdpi.consumer.rdist001.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DistribuerTilNyKanalRequest(
		long forsendelseId,
		String arsak,
		String arsakBeskrivelse) {

	public static final String MELDINGSFEIL = "MELDINGSFEIL";
	public static final String ARSAK_PUBLISERING_FEILET = "Mottaker har ikke sikker digital postkasse og kan derfor ikke motta digital post";

	@JsonProperty
	public String kanal() {
		return "PRINT";
	}

	public static DistribuerTilNyKanalRequest arsakMeldingsfeil(long forsendelseId) {
		return DistribuerTilNyKanalRequest.builder()
				.forsendelseId(forsendelseId)
				.arsak(MELDINGSFEIL)
				.arsakBeskrivelse(ARSAK_PUBLISERING_FEILET)
				.build();
	}
}
