package no.nav.dokdistdpi.consumer.rdist001;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HentMottakerResponse {

	private String tema;
	private MottakerTo mottaker;

	@Data
	@Builder
	public static class MottakerTo {
		private String mottakerId;
		private String mottakerType;
	}
}
