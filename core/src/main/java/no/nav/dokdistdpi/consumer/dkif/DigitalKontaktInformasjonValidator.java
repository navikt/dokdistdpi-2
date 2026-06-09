package no.nav.dokdistdpi.consumer.dkif;

import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class DigitalKontaktInformasjonValidator {

	public void validateKontaktinfo(SikkerDigitalKontaktInfo digitalKontaktinfo) {
		if (brukerErReservert(digitalKontaktinfo)) {
			throw new IllegalKontaktInformasjonFunctionalException("Bruker er reservert mot digital kommunikasjon");
		}

		if (leverandoerinfoEllerBrukeradresseMangler(digitalKontaktinfo)) {
			throw new IllegalKontaktInformasjonFunctionalException("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
		}

		if (baadeEpostOgMobilnummerMangler(digitalKontaktinfo)) {
			throw new IllegalKontaktInformasjonFunctionalException("Både epostadresse og mobiltelefonnummer kan ikke være null");
		}
	}

	private boolean brukerErReservert(SikkerDigitalKontaktInfo digitalKontaktinfo) {
		return digitalKontaktinfo != null && digitalKontaktinfo.isReservasjon();
	}

	private boolean leverandoerinfoEllerBrukeradresseMangler(SikkerDigitalKontaktInfo digitalKontaktinfo) {
		return isBlank(digitalKontaktinfo.getLeverandoerSertifikat())
				|| isBlank(digitalKontaktinfo.getLeverandoerAdresse())
				|| isBlank(digitalKontaktinfo.getBrukerAdresse());
	}

	private boolean baadeEpostOgMobilnummerMangler(SikkerDigitalKontaktInfo digitalKontaktinfo) {
		return digitalKontaktinfo.isKanVarsles()
				&& isBlank(digitalKontaktinfo.getMobiltelefonnummer())
				&& isBlank(digitalKontaktinfo.getEpostadresse());
	}

}