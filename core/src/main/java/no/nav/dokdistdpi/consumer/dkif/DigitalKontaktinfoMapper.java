package no.nav.dokdistdpi.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.exception.functional.BrukerHarIngenDigitalpostkasseException;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class DigitalKontaktinfoMapper {

	public static SikkerDigitalKontaktInfo mapDigitalKontaktinfo(DigitalKontaktInfoResponse.DigitalKontaktinfo digitalKontaktinfo) {
		if (digitalKontaktinfo == null || (digitalKontaktinfo.getSikkerDigitalPostkasse() == null)) {
			throw new BrukerHarIngenDigitalpostkasseException("Brukeren har ingen digital postkasse.");
		} else {
			DigitalKontaktInfoResponse.SikkerDigitalPostkasse sikkerDigitalPostkasse = digitalKontaktinfo.getSikkerDigitalPostkasse();
			validateSikkerDigitalPostKasse(sikkerDigitalPostkasse);
			return SikkerDigitalKontaktInfo.builder()
					.personidentifikator(digitalKontaktinfo.getPersonident())
					.brukerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getAdresse() : null)
					.epostadresse(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getEpostadresse() : null)
					.leverandoerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerAdresse() : null)
					.mobiltelefonnummer(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getMobiltelefonnummer() : null)
					.leverandoerSertifikat(isNotBlank(sikkerDigitalPostkasse.getLeverandoerSertifikat()) ? sikkerDigitalPostkasse.getLeverandoerSertifikat() : null)
					.reservasjon(digitalKontaktinfo.isReservert())
					.sertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && isNotBlank(digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerSertifikat()))
					.kanVarsles(digitalKontaktinfo.isKanVarsles())
					.build();
		}
	}

	private static void validateSikkerDigitalPostKasse(DigitalKontaktInfoResponse.SikkerDigitalPostkasse sikkerDigitalPostkasse) {
		assertNotBlank("Adresse", sikkerDigitalPostkasse.getAdresse());
		assertNotBlank("LeverandoerSertifikat", sikkerDigitalPostkasse.getLeverandoerSertifikat());
		assertNotBlank("LeverandoerAdresse", sikkerDigitalPostkasse.getLeverandoerAdresse());
	}

	private DigitalKontaktinfoMapper() {
	}

}
