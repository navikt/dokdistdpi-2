package no.nav.dokdistdpi.consumer.dkif;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SikkerDigitalKontaktInfo {

	private String personidentifikator;
	private String epostadresse;
	private boolean kanVarsles;
	private String mobiltelefonnummer;
	private boolean reservasjon;
	private String leverandoerAdresse;
	private String leverandoerSertifikat;
	private String brukerAdresse;
	private boolean sertifikat;

}
