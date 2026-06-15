package no.nav.dokdistdpi.consumer.dkif;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder;
import no.nav.dokdistdpi.exception.functional.BrukerReservertMotDigitalpostkasseException;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DigitalKontaktInformasjonValidatorTest {

	private static final String EPOSTADRESSE = "epostValue";
	private static final String MOBILNUMMER = "mobilValue";
	private static final boolean IKKE_RESERVERT = false;
	private static final String LEVERANDOERADRESSE = "leverandoeradresse";
	private static final String LEVERANDOERSERTIFIKAT = "testsertifikat";
	private static final String BRUKERADRESSE = "brukeradresse";
	private static final boolean HAR_SERTIFIKAT = true;

	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();

	@Test
	void skalValidereOk() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder().kanVarsles(true).build();

		assertDoesNotThrow(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo));
	}

	@Test
	void skalValidereOkHvisVarselinfoErNull() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.kanVarsles(true)
				.build();

		assertDoesNotThrow(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo));
	}

	@Test
	void skalFeileHvisBrukerErReservert() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.reservasjon(true)
				.kanVarsles(false)
				.build();

		assertThatExceptionOfType(BrukerReservertMotDigitalpostkasseException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo))
				.withMessage("Bruker er reservert mot digital kommunikasjon");
	}

	@Test
	void skalFeileHvisLeverandoersertifikatMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.leverandoerSertifikat(null)
				.kanVarsles(false)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@Test
	void skalFeileHvisLeverandoeradresseMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.leverandoerAdresse(null)
				.kanVarsles(true)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@Test
	void skalFeileHvisBrukeradresseMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.brukerAdresse(null)
				.kanVarsles(true).build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	@NullSource
	void skalFeileHvisBaadeEpostOgMobiltelefonnummerMangler(String epostOgMobil) {
		var sikkerDigitalKontaktinfo =
				lagSikkerDigitalKontaktInfoBuilder()
						.epostadresse(epostOgMobil)
						.mobiltelefonnummer(epostOgMobil)
						.kanVarsles(true).build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo))
				.withMessage("Både epostadresse og mobiltelefonnummer kan ikke være null");
	}

	private SikkerDigitalKontaktInfoBuilder lagSikkerDigitalKontaktInfoBuilder() {
		return SikkerDigitalKontaktInfo.builder()
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBILNUMMER)
				.reservasjon(IKKE_RESERVERT)
				.leverandoerAdresse(LEVERANDOERADRESSE)
				.leverandoerSertifikat(LEVERANDOERSERTIFIKAT)
				.brukerAdresse(BRUKERADRESSE)
				.sertifikat(HAR_SERTIFIKAT);
	}

	@Test
	void skalIkkeKreveVarslingskanalerNaarKanVarslesErFalse() {
		var info = lagSikkerDigitalKontaktInfoBuilder()
				.kanVarsles(false)
				.epostadresse(null)
				.mobiltelefonnummer(null)
				.build();

		assertDoesNotThrow(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(info));
	}
}