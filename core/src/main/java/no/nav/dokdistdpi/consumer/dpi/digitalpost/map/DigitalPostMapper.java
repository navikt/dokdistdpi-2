package no.nav.dokdistdpi.consumer.dpi.digitalpost.map;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Avsender;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPostInfo;
import no.nav.dokdistdpi.consumer.rdist001.HentForsendelseResponse;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;

public class DigitalPostMapper {

	public DigitalPostInfo map(SikkerDigitalKontaktInfo digitalKontaktInfo, String maskinportenToken,
							   HentForsendelseResponse hentForsendelseResponse)
	{
		return DigitalPostInfo.builder()
				.avsender(Avsender.builder()
						.avsenderindentifikator(Organisasjonsnummer.asIso6523(NAV_ORGNUMMER))
						.avsenderindentifikator(NAV_ORGNUMMER)
						.build())
				.personmottaker(DigitalPostInfo.Personmottaker.builder()
						.postkasseadresse(digitalKontaktInfo.getBrukerAdresse())
						.build())
				.motakeridentifikator(digitalKontaktInfo.getLeverandoerAdresse())
				.maskinportentoken(maskinportenToken)
				.build();
	}
}
