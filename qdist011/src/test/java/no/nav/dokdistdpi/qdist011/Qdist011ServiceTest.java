package no.nav.dokdistdpi.qdist011;

import no.nav.dokdistdpi.cloudstorage.EncryptedBucketStorage;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.Dokumentkatalog;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.DokumentkatalogConsumer;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.exception.functional.IllegalKontaktInformasjonFunctionalException;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import no.nav.dokdistdpi.qdist011.saf.JournalpostQdist011;
import no.nav.dokdistdpi.qdist011.saf.SafJournalpostQueryServiceImplQdist011;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Sikkerhetsnivaa.NIVAA_4;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.qdist011.TestUtil.BESTILLINGS_ID;
import static no.nav.dokdistdpi.qdist011.TestUtil.KONVERSASJON_ID;
import static no.nav.dokdistdpi.qdist011.TestUtil.MASKINPORTEN_TOKEN;
import static no.nav.dokdistdpi.qdist011.TestUtil.MOTTAKER_FNR;
import static no.nav.dokdistdpi.qdist011.TestUtil.MOTTAKER_ORGNO;
import static no.nav.dokdistdpi.qdist011.TestUtil.POSTKASSEADRESSE;
import static no.nav.dokdistdpi.qdist011.TestUtil.TITTEL;
import static no.nav.dokdistdpi.qdist011.TestUtil.buildHentForsendelseResponseWithArkivinformasjon;
import static no.nav.dokdistdpi.qdist011.TestUtil.buildHentForsendelseResponseWithDokument;
import static no.nav.dokdistdpi.qdist011.TestUtil.classpathToString;
import static no.nav.dokdistdpi.qdist011.TestUtil.createDistribuerTilKanal;
import static no.nav.dokdistdpi.qdist011.TestUtil.createDokumenttypeInfoTo;
import static no.nav.dokdistdpi.qdist011.TestUtil.createJournalpostQdist011;
import static no.nav.dokdistdpi.qdist011.TestUtil.createOidcTokenResponse;
import static no.nav.dokdistdpi.qdist011.TestUtil.createSikkerDigitalKontaktInfo;
import static no.nav.dokdistdpi.qdist011.TestUtil.createVarselInfoTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class Qdist011ServiceTest {

	private DokdistadminConsumer dokdistadminConsumer;
	private Qdist011Service qdist011Service;
	private SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService;

	private MaskinportenTokenConsumer maskinportenTokenConsumer;
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private VarselInfo varselInfo;
	private Dokumentkatalog dokumentkatalog;
	private Exchange exchange;

	@BeforeEach
	void setup() {
		dokdistadminConsumer = mock(DokdistadminConsumer.class);
		safJournalpostQueryService = mock(SafJournalpostQueryServiceImplQdist011.class);
		maskinportenTokenConsumer = mock(MaskinportenTokenConsumer.class);
		digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
		varselInfo = mock(VarselInfo.class);
		dokumentkatalog = mock(DokumentkatalogConsumer.class);
		exchange = mock(Exchange.class);
		EncryptedBucketStorage encryptedBucketStorage = mock(EncryptedBucketStorage.class);

		DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();
		DigitalPostService digitalPostService = new DigitalPostService(maskinportenTokenConsumer, digitalKontaktInformasjonValidator,
				digitalKontaktinformasjonConsumer, varselInfo, dokumentkatalog);

		qdist011Service = new Qdist011Service(encryptedBucketStorage, dokdistadminConsumer, digitalPostService, safJournalpostQueryService, "07:00:00", "23:00:00");

		when(encryptedBucketStorage.downloadObject(anyString(), anyString())).thenReturn("{\"pdf\":\"SE9WRURET0tfVEVTVF9DT05URU5U\",\"dokumentObjektReferanse\":null,\"dokumentInfoId\":null}");
	}

	@ParameterizedTest
	@CsvSource(value = {
			"VIKTIG", "ANNET", "NULL"
	}, nullValues = {"NULL"})
	void skalLageForsendelse(String distribusjonstypecode) {
		when(dokdistadminConsumer.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokument(distribusjonstypecode));
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse(MASKINPORTEN_TOKEN));
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(createSikkerDigitalKontaktInfo());
		when(varselInfo.getVarselInfo(anyString())).thenReturn(createVarselInfoTo());
		when(dokumentkatalog.getDokumenttypeInfo(anyString())).thenReturn(createDokumenttypeInfoTo());

		Forsendelse forsendelse = qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange);

		assertEquals(MOTTAKER_FNR, forsendelse.getPersonidentifikator());
		assertEquals(KONVERSASJON_ID, forsendelse.getKonversasjonId());
		assertEquals(MOTTAKER_FNR, forsendelse.getPersonidentifikator());
		assertEquals(MOTTAKER_ORGNO, forsendelse.getDigitalPostLeverandoerAdresse());
		assertEquals(classpathToString("sertifikat/mottakercertificate"), forsendelse.getMottakerSertifikat());
		assertEquals(MOTTAKER_ORGNO, forsendelse.getDigitalPostLeverandoerAdresse());
		assertEquals(BESTILLINGS_ID, forsendelse.getBestillingsId());
		assertDigitalMapping(forsendelse.getDigital());
		assertNotNull(forsendelse.getDokumentpakke());
	}

	@Test
	void skalLageForsendelseWithoutVarslerWhenDistribusjonstypeCodeIsANNET() {
		when(dokdistadminConsumer.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokument(ANNET.toString()));
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse(MASKINPORTEN_TOKEN));
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(createSikkerDigitalKontaktInfo());
		when(varselInfo.getVarselInfo(anyString())).thenReturn(createVarselInfoTo());
		when(dokumentkatalog.getDokumenttypeInfo(anyString())).thenReturn(createDokumenttypeInfoTo());

		Forsendelse forsendelse = qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange);

		assertEquals(MOTTAKER_FNR, forsendelse.getPersonidentifikator());
		assertEquals(KONVERSASJON_ID, forsendelse.getKonversasjonId());
		assertEquals(MOTTAKER_FNR, forsendelse.getPersonidentifikator());
		assertEquals(MOTTAKER_ORGNO, forsendelse.getDigitalPostLeverandoerAdresse());
		assertEquals(classpathToString("sertifikat/mottakercertificate"), forsendelse.getMottakerSertifikat());
		assertEquals(MOTTAKER_ORGNO, forsendelse.getDigitalPostLeverandoerAdresse());
		assertEquals(BESTILLINGS_ID, forsendelse.getBestillingsId());
		assertDigitalMapping(forsendelse.getDigital());
		assertNotNull(forsendelse.getDokumentpakke());
		assertNull(forsendelse.getDigital().getVarsler());
	}

	@Test
	void shoudThrowExceptionIfMaskinportenttokenIsNull() {
		when(dokdistadminConsumer.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokument());
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse(null));

		MaskinportenFunctionalException ex = assertThrows(MaskinportenFunctionalException.class, () -> qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange));

		assertEquals("MaskinportenToken kan ikke være null", ex.getMessage());
	}

	@Test
	void shoudThrowExceptionIfLeverandoerSertifikatIsNull() {
		SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = createSikkerDigitalKontaktInfo();
		sikkerDigitalKontaktInfo.setLeverandoerSertifikat(null);

		when(dokdistadminConsumer.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokument());
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse(MASKINPORTEN_TOKEN));
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(sikkerDigitalKontaktInfo);
		when(varselInfo.getVarselInfo(anyString())).thenReturn(createVarselInfoTo());
		when(dokumentkatalog.getDokumenttypeInfo(anyString())).thenReturn(createDokumenttypeInfoTo());

		IllegalKontaktInformasjonFunctionalException e = assertThrows(IllegalKontaktInformasjonFunctionalException.class, () -> qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange));

		assertEquals("Manglende sertifikat, leverandoerAdresse eller brukerAdresse", e.getMessage());
	}

	@Test
	void shouldReturnNumberedVersionsOfskalNavngiVedleggMedTilleggsnummer() {
		when(dokdistadminConsumer.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithArkivinformasjon(ANNET.toString()));
		when(safJournalpostQueryService.hentJournalpost(anyString())).thenReturn(createJournalpostQdist011());
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse(MASKINPORTEN_TOKEN));
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(createSikkerDigitalKontaktInfo());
		when(dokumentkatalog.getDokumenttypeInfo(anyString())).thenReturn(createDokumenttypeInfoTo());

		Forsendelse forsendelse = qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange);

		List<String> test = forsendelse.getDokumentpakke().getVedlegg().stream().map(DpiDokument::getTittel).toList();
		assertTrue(test.contains("Vedlegget"));
		assertTrue(test.contains("Vedlegg (1)"));
		assertTrue(test.contains("Vedlegg (2)"));
	}

	@Test
	void shouldReturnNumberedVersionsOfDuplicateDokumentTittel() {
		var mapOfDokumenttitler = qdist011Service.mapDokumenttitler(createJournalpostQdist011());
		assertThat(mapOfDokumenttitler.get("1")).isEqualTo("hoveddokument");
		assertThat(mapOfDokumenttitler.get("2")).isEqualTo("Vedlegget (1)");
		assertThat(mapOfDokumenttitler.get("3")).isEqualTo("Vedlegg (1)");
		assertThat(mapOfDokumenttitler.get("4")).isEqualTo("Vedlegg (2)");
		assertThat(mapOfDokumenttitler.get("2")).isEqualTo("Vedlegget (2)");
	}

	private void assertDigitalMapping(DigitalPost digitalPost) {
		assertEquals(MASKINPORTEN_TOKEN, digitalPost.getMaskinportentoken());
		assertEquals(ISO_6523_ACTORID_UPIS.getValue(), digitalPost.getAvsender().getVirksomhetsidentifikator().getAuthority());
		assertEquals(asIso6523(NAV_ORGNUMMER), digitalPost.getAvsender().getVirksomhetsidentifikator().getValue());
		assertEquals(POSTKASSEADRESSE, digitalPost.getMottaker().getPostkasseadresse());
		assertNull(digitalPost.getDokumentpakkefingeravtrykk());
		assertEquals(NIVAA_4.getValue(), digitalPost.getSikkerhetsnivaa());
		assertEquals(TITTEL, digitalPost.getIkkesensitivtittel());
	}

}