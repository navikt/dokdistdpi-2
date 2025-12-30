package no.nav.dokdistdpi.consumer.dkif;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistdpi.utils.ForsendelseData.makePreferertKanalSet;
import static no.nav.dokdistdpi.utils.ForsendelseData.varslingsTekster;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DigitalKontaktInformasjonValidatorTest {

	private static final String VARSELTYPE_ID = "varseltypeId";
	private static final boolean STOPP_REPETERENDE_VARSEL = false;
	private static final String EPOST_VARSLINGSTEKST = "epostVarslingstekst";
	private static final String SMS_VARSLINGSTEKST = "smsVarslingstekst";
	private static final List<Integer> ANTALL_DAGER_LISTE = Arrays.asList(1, 2, 3);
	private static final String PREFERERT_KANAL_SMS = "SMS";
	private static final String PREFERERT_KANAL_EPOST = "EPOST";

	private static final String EPOSTADRESSE = "epostValue";
	private static final String MOBILNUMMER = "mobilValue";
	private static final boolean IKKE_RESERVERT = false;
	private static final String LEVERANDOERADRESSE = "leverandoeradresse";
	private static final String LEVERANDOERSERTIFIKAT = "testsertifikat";
	private static final String BRUKERADRESSE = "brukeradresse";
	private static final boolean HAR_SERTIFIKAT = true;

	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();

	@Test
	public void skalValidereOk() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder().build();

		assertDoesNotThrow(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()));
	}

	@Test
	public void skalValidereOkHvisVarselinfoErNull() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder().build();

		assertDoesNotThrow(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, null));
	}

	@Test
	public void skalFeileHvisBrukerErReservert() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.reservasjon(true)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()))
				.withMessage("Bruker er reservert mot digital kommunikasjon");
	}

	@Test
	public void skalFeileHvisLeverandoersertifikatMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.leverandoerSertifikat(null)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@Test
	public void skalFeileHvisLeverandoeradresseMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.leverandoerAdresse(null)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@Test
	public void skalFeileHvisBrukeradresseMangler() {
		var sikkerDigitalKontaktinfo = lagSikkerDigitalKontaktInfoBuilder()
				.brukerAdresse(null)
				.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()))
				.withMessage("Leverandoersertifikat, leverandoeradresse eller brukeradresse mangler");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	@NullSource
	public void skalFeileHvisBaadeEpostOgMobiltelefonnummerMangler(String epostOgMobil) {
		var sikkerDigitalKontaktinfo =
				lagSikkerDigitalKontaktInfoBuilder()
						.epostadresse(epostOgMobil)
						.mobiltelefonnummer(epostOgMobil)
						.build();

		assertThatExceptionOfType(IllegalKontaktInformasjonFunctionalException.class)
				.isThrownBy(() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktinfo, lagVarselinfo()))
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

	private VarselInfo lagVarselinfo() {
		return VarselInfo.builder()
				.varselTypeId(VARSELTYPE_ID)
				.stoppRepeterendeVarsel(STOPP_REPETERENDE_VARSEL)
				.varslingsTekst(varslingsTekster(EPOST_VARSLINGSTEKST, SMS_VARSLINGSTEKST))
				.antallDagerListe(ANTALL_DAGER_LISTE)
				.preferertKanal(makePreferertKanalSet(PREFERERT_KANAL_EPOST, PREFERERT_KANAL_SMS))
				.build();
	}
}