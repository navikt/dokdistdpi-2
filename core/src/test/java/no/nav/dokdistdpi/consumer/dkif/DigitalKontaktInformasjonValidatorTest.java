package no.nav.dokdistdpi.consumer.dkif;

import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistdpi.utils.ForsendelseData.makePreferertKanalSet;
import static no.nav.dokdistdpi.utils.ForsendelseData.varslingsTekster;
import static org.junit.jupiter.api.Assertions.*;

class DigitalKontaktInformasjonValidatorTest {

	private static final String VARSEL_TYPE_ID = "varselTypeId";
	private static final boolean STOPP_REPETERENDE_VARSEL = false;
	private static final String EPOST_VARSLINGS_TEKST = "epostVarslingsTekst";
	private static final String SMS_VARSLINGS_TEKST = "smsVarslingsTekst";
	private static final List<Integer> ANTALL_DAGER_LISTE = Arrays.asList(1, 2, 3);
	private static final String PREFERERT_KANAL_SMS = "SMS";
	private static final String PREFERERT_KANAL_EPOST = "EPOST";
	private static final boolean HAS_SERTIFIKAT = true;
	private static final String LEVERANDOER_SERTIFIKAT = "testSertifikat";
	private static final boolean RESERVASJON = false;
	private static final String EPOST_VALUE = "epostValue";
	private static final String MOBIL_VALUE = "mobilValue";
	private static final String LEVERANDOER_ADRESSE = "leverandoerAdresse";
	private static final String BRUKER_ADRESSE = "brukerAdresse";


	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();

	@Test
	public void shouldValidateOk() {
		digitalKontaktInformasjonValidator.validateKontaktinfo(createSikkerDigitalKontaktInformasjonToBuilder()
						.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
						.brukerAdresse(BRUKER_ADRESSE).build(),
				createVarselInfoToBuilder().build());
	}

	@Test
	public void shouldValidateOkWitoutVarselInfoTo() {
		digitalKontaktInformasjonValidator.validateKontaktinfo(createSikkerDigitalKontaktInformasjonToBuilder().leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
				.brukerAdresse(BRUKER_ADRESSE).build(), null);
	}


	@Test
	public void shouldFailWithoutEpostAndSMS() {

		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder SikkerDigitalKontaktInfoBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		SikkerDigitalKontaktInfoBuilder
				.reservasjon(RESERVASJON)
				.brukerAdresse(BRUKER_ADRESSE)
				.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
				.reservasjon(RESERVASJON)
				.epostadresse(null)
				.mobiltelefonnummer(null);

		VarselInfo varselInfo = createVarselInfoToBuilder().build();

		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(SikkerDigitalKontaktInfoBuilder.build(), varselInfo));
		assertEquals(exception.getMessage(), "Både epostadresse og mobiltelefonnummer kan ikke være null");
	}

	@Test
	public void shouldFailWithUgyldigEpostAndSMS() {

		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder sikkerDigitalKontaktInformasjonToBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		sikkerDigitalKontaktInformasjonToBuilder
				.reservasjon(RESERVASJON)
				.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
				.epostadresse(null)
				.mobiltelefonnummer(null);

		VarselInfo varselInfo = createVarselInfoToBuilder().build();
		assertNotNull(varselInfo);

		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInformasjonToBuilder.build(), varselInfo));
		assertEquals(exception.getMessage(), "Manglende sertifikat, leverandoerAdresse eller brukerAdresse");
	}

	@Test
	public void shouldFailWithReservasjon() {

		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder sikkerDigitalKontaktInformasjonToBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		sikkerDigitalKontaktInformasjonToBuilder
				.reservasjon(true)
				.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
				.epostadresse(EPOST_VALUE)
				.mobiltelefonnummer(MOBIL_VALUE);

		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInformasjonToBuilder.build(),
						createVarselInfoToBuilder().build()));
		assertEquals(exception.getMessage(), "Bruker er reservert mot digital kommunikasjon");

	}

	@Test
	public void shouldFailNoSertifikat() {
		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder sikkerDigitalKontaktInformasjonToBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		sikkerDigitalKontaktInformasjonToBuilder
				.leverandoerSertifikat(null);

		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInformasjonToBuilder.build(),
						createVarselInfoToBuilder().build()));
		assertEquals(exception.getMessage(), "Manglende sertifikat, leverandoerAdresse eller brukerAdresse");
	}

	@Test
	public void shouldFailNoLeverandoerAdresse() {
		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder sikkerDigitalKontaktInformasjonToBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		sikkerDigitalKontaktInformasjonToBuilder
				.leverandoerSertifikat(null)
				.brukerAdresse(BRUKER_ADRESSE);


		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInformasjonToBuilder.build(),
						createVarselInfoToBuilder().build()));
		assertEquals(exception.getMessage(), "Manglende sertifikat, leverandoerAdresse eller brukerAdresse");
	}

	@Test
	public void shouldFailNoBrukerAdresse() {
		SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder sikkerDigitalKontaktInformasjonToBuilder =
				createSikkerDigitalKontaktInformasjonToBuilder();
		sikkerDigitalKontaktInformasjonToBuilder
				.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
				.brukerAdresse(null)
				.kanVarsles(true);


		IllegalKontaktInformasjonFunctionalException exception = assertThrows(IllegalKontaktInformasjonFunctionalException.class,
				() -> digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInformasjonToBuilder.build(),
						createVarselInfoToBuilder().build()));
		assertEquals(exception.getMessage(), "Manglende sertifikat, leverandoerAdresse eller brukerAdresse");
	}

	private SikkerDigitalKontaktInfo.SikkerDigitalKontaktInfoBuilder createSikkerDigitalKontaktInformasjonToBuilder() {
		return SikkerDigitalKontaktInfo.builder()
				.reservasjon(RESERVASJON)
				.epostadresse(EPOST_VALUE)
				.mobiltelefonnummer(MOBIL_VALUE)
				.leverandoerAdresse(LEVERANDOER_ADRESSE)
				.sertifikat(HAS_SERTIFIKAT);
	}

	private VarselInfo.VarselInfoBuilder createVarselInfoToBuilder() {
		return VarselInfo.builder()
				.varselTypeId(VARSEL_TYPE_ID)
				.stoppRepeterendeVarsel(STOPP_REPETERENDE_VARSEL)
				.varslingsTekst(varslingsTekster(EPOST_VARSLINGS_TEKST, SMS_VARSLINGS_TEKST))
				.antallDagerListe(ANTALL_DAGER_LISTE)
				.preferertKanal(makePreferertKanalSet(PREFERERT_KANAL_EPOST, PREFERERT_KANAL_SMS));
	}
}