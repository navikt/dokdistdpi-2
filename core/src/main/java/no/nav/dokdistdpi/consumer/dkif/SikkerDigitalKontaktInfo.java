package no.nav.dokdistdpi.consumer.dkif;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SikkerDigitalKontaktInfo {

	private String personident;
	private String epostadresse;
	private boolean kanVarsles;
	private String mobiltelefonnummer;
	private boolean reservasjon;
	private String leverandoerAdresse;
	private String leverandoerSertifikat;
	private String brukerAdresse;
	private boolean sertifikat;

}
