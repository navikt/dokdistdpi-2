package no.nav.dokdistdpi.consumer.dkif;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotNull;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.notBlank;

public class DigitalKontaktinfoMapper {

	public SikkerDigitalKontaktInfo mapDigitalKontaktinfo(DigitalKontaktInfoResponse.DigitalKontaktinfo digitalKontaktinfo, String personident) {
		if (digitalKontaktinfo == null) {
			return null;
		} else {
			DigitalKontaktInfoResponse.SikkerDigitalPostkasse sikkerDigitalPostkasse = digitalKontaktinfo.getSikkerDigitalPostkasse();
			validateSikkerDigitalPostKasse(sikkerDigitalPostkasse);
			return SikkerDigitalKontaktInfo.builder()
					.personident(personident)
					.brukerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getAdresse() : null)
					.epostadresse(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getEpostadresse() : null)
					.leverandoerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerAdresse() : null)
					.mobiltelefonnummer(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getMobiltelefonnummer() : null)
					.leverandoerSertifikat(notBlank(sikkerDigitalPostkasse.getLeverandoerSertifikat()) ? sikkerDigitalPostkasse.getLeverandoerSertifikat() : null)
					.reservasjon(digitalKontaktinfo.isReservert())
					.sertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && notBlank(digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerSertifikat()))
					.kanVarsles(digitalKontaktinfo.isKanVarsles())
					.build();
		}
	}

	private void validateSikkerDigitalPostKasse(DigitalKontaktInfoResponse.SikkerDigitalPostkasse sikkerDigitalPostkasse) {
		assertNotNull("SikkerDigitalPostkasse", sikkerDigitalPostkasse);
		assertNotBlank("Adresse", sikkerDigitalPostkasse.getAdresse());
		assertNotBlank("LeverandoerSertifikat", sikkerDigitalPostkasse.getLeverandoerSertifikat());
		assertNotBlank("LeverandoerAdresse", sikkerDigitalPostkasse.getLeverandoerAdresse());
	}
}
