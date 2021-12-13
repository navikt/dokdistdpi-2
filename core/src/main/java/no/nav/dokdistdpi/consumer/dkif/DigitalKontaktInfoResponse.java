package no.nav.dokdistdpi.consumer.dkif;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DigitalKontaktInfoResponse {

	private Map<String, Melding> feil;
	private Map<String, DigitalKontaktinfo> kontaktinfo;

	@Data
	public static class Melding {
		private String melding;
	}

	@Data
	@Builder
	public static class DigitalKontaktinfo {
		private String epostadresse;
		private boolean kanVarsles;
		private String mobiltelefonnummer;
		private boolean reservert;
		private SikkerDigitalPostkasse sikkerDigitalPostkasse;
	}

	@Data
	@Builder
	public static class SikkerDigitalPostkasse {
		private String adresse;
		private String leverandoerAdresse;
		private String leverandoerSertifikat;
	}
}
