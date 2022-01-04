package no.nav.dokdistdpi.consumer.dkif;

import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfoTo;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DigitalKontaktInformasjonValidator {

	public void validateKontaktinfo(SikkerDigitalKontaktInfo digitalKontaktInfo, VarselInfoTo varselInfoTo) {
		if (digitalKontaktInfo != null && digitalKontaktInfo.isReservasjon()) {
			throw new IllegalKontaktInformasjonFunctionalException("Bruker er reservert mot digital kommunikasjon");
		}
		if (!hasValidSertifikatAndAdresses(digitalKontaktInfo)) {
			throw new IllegalKontaktInformasjonFunctionalException("Manglende sertifikat, leverandoerAdresse eller brukerAdresse");
		}
		if (varselInfoTo != null) {
			verifyEmailAndPhone(digitalKontaktInfo);
		}
	}

	private boolean hasValidSertifikatAndAdresses(SikkerDigitalKontaktInfo digitalKontaktInfo) {
		return isNotBlank(digitalKontaktInfo.getLeverandoerSertifikat()) && isNotBlank(digitalKontaktInfo.getLeverandoerAdresse())
				&& isNotBlank(digitalKontaktInfo.getBrukerAdresse());
	}

	private void verifyEmailAndPhone(SikkerDigitalKontaktInfo digitalKontaktInfo) {
		if (isBlank(digitalKontaktInfo.getMobiltelefonnummer()) && isBlank(digitalKontaktInfo.getEpostadresse())) {
			throw new IllegalKontaktInformasjonFunctionalException("Både epostadresse og mobiltelefonnummer kan ikke være null");
		}
	}
}
