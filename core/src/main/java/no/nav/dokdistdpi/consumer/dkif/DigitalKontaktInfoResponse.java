package no.nav.dokdistdpi.consumer.dkif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigitalKontaktInfoResponse {

	private Map<String, String> feil;
	private Map<String, DigitalKontaktinfo> personer;

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
