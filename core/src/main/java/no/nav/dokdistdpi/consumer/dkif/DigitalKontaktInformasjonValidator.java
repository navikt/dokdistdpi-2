package no.nav.dokdistdpi.consumer.dkif;

import io.micrometer.core.instrument.util.StringUtils;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfoTo;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.isBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.notBlank;
import static org.springframework.util.StringUtils.hasText;

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
		boolean hasSertifikat = (digitalKontaktInfo.getLeverandoerSertifikat() != null) &&
				(digitalKontaktInfo.getLeverandoerSertifikat().length() > 0);

		boolean hasLeverandorAdresse =
				notBlank(digitalKontaktInfo.getLeverandoerAdresse());
		boolean hasBrukerAdresse = StringUtils.isNotBlank(digitalKontaktInfo.getBrukerAdresse());

		return (hasSertifikat && hasLeverandorAdresse && hasBrukerAdresse);
	}

	private void verifyEmailAndPhone(SikkerDigitalKontaktInfo digitalKontaktInfo) {
		if (isBlank(digitalKontaktInfo.getMobiltelefonnummer()) && isBlank(digitalKontaktInfo.getEpostadresse())) {
			throw new IllegalKontaktInformasjonFunctionalException("Både epostadresse og mobiltelefonnummer kan ikke være null");
		}
	}
}
