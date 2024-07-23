package no.nav.dokdistdpi.sdist003;

import lombok.SneakyThrows;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DokumenttypeInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.ArkivInformasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Dokument;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Sikkerhetsnivaa.NIVAA_4;
import static no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.ARKIV_SYSTEM_JOARK;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.EPOST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.SMS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.VEDLEGG;

public final class TestUtil {
	public static final String HENT_FORSENDELSE_RESPONSE_BESTILLINGS_ID = "123456789012341";
	public static final String ARKIV_INFORMASJON_ARKIV_ID = "123456789012341";
	public static final String ARKIV_INFORMASJON_ARKIV_SYSTEM_JOARK = "JOARK";

	public static final String HOVED_DOKUMENT_INFO_ID = "1";
	public static final String HOVED_DOKUMENT_REF = "ref-1";

	public static final String VEDLEGG_1_DOKUMENT_INFO_ID = "2";
	public static final String VEDLEGG_1_DOKUMENT_REF = "ref-2";

	public static final String VEDLEGG_2_DOKUMENT_INFO_ID = "3";
	public static final String VEDLEGG_2_DOKUMENT_REF = "ref-3";

	public static final String TITTEL = "Tittel";
	public static final String DOKUMENTINFO_ID = "4";

	public static final String MASKINPORTEN_TOKEN = "aølkdsølkdsj==";
	public static final String MASKINPORTEN_SCOPE = "digitalpostinnbygger:send";

	private static final boolean HAS_SERTIFIKAT = true;
	private static final boolean RESERVASJON = false;
	private static final String EPOST_VALUE = "epostValue";
	public static final String MOBIL_VALUE = "mobilValue";
	private static final String LEVERANDOER_ADRESSE = "leverandoerAdresse";

	private static final String VARSEL_TYPE_ID = "varselTypeId";
	private static final boolean STOPP_REPETERENDE_VARSEL = false;
	private static final String EPOST_VARSLINGS_TEKST = "epostVarslingsTekst";
	private static final String SMS_VARSLINGS_TEKST = "smsVarslingsTekst";
	private static final List<Integer> ANTALL_DAGER_LISTE = Arrays.asList(1, 2, 3);
	private static final String PREFERERT_KANAL_SMS = "SMS";
	private static final String PREFERERT_KANAL_EPOST = "EPOST";

	public static final String DOKUMENTTYPE_ID = "DokumenttypeId";

	public static final String BESTILLINGS_ID = UUID.randomUUID().toString();
	public static final String MOTTAKER_FNR = "04036125433";
	private static final String MOTTAKER_TYPE = "Person";
	public static final String POSTKASSEADRESSE = "ove.jonsen#6K5A";
	private static final String EPOSTADRESSE = "example@email.org";
	private static final String MOBILTELEFONNUMMER = "4799999999";
	private static final String VARSLINGSTEKST = "Du har mottatt brev i din digitale postkasse";
	public static final String VIRKSOMHETMOTTAKER = "984661185";
	public static final String MOTTAKER_ORGNO = "988015814";
	public static final String KONVERSASJON_ID = UUID.randomUUID().toString();

	public static OidcTokenResponse createOidcTokenResponse(String accessToken) {
		OidcTokenResponse oidcTokenResponse = new OidcTokenResponse();
		oidcTokenResponse.setAccessToken(accessToken);
		oidcTokenResponse.setScope(MASKINPORTEN_SCOPE);
		oidcTokenResponse.setExpiresIn(30);
		return oidcTokenResponse;
	}


	public static SikkerDigitalKontaktInfo createSikkerDigitalKontaktInfo() {
		return SikkerDigitalKontaktInfo.builder()
				.personidentifikator(MOTTAKER_FNR)
				.brukerAdresse(POSTKASSEADRESSE)
				.reservasjon(RESERVASJON)
				.epostadresse(EPOST_VALUE)
				.mobiltelefonnummer(MOBIL_VALUE)
				.leverandoerAdresse(MOTTAKER_ORGNO)
				.leverandoerSertifikat(classpathToString("sertifikat/mottakercertificate"))
				.sertifikat(HAS_SERTIFIKAT)
				.build();
	}

	public static VarselInfo createVarselInfoTo() {
		return VarselInfo.builder()
				.varselTypeId(VARSEL_TYPE_ID)
				.stoppRepeterendeVarsel(STOPP_REPETERENDE_VARSEL)
				.varslingsTekst(varslingsTekster(EPOST_VARSLINGS_TEKST, SMS_VARSLINGS_TEKST))
				.antallDagerListe(ANTALL_DAGER_LISTE)
				.preferertKanal(makePreferertKanalSet(PREFERERT_KANAL_EPOST, PREFERERT_KANAL_SMS))
				.build();
	}

	public static DokumenttypeInfo createDokumenttypeInfoTo() {
		return DokumenttypeInfo.builder()
				.sikkerhetsnivaa(NIVAA_4.getValue())
				.varselTypeId(VARSEL_TYPE_ID)
				.build();
	}

	public static DistribuerTilKanal createDistribuerTilKanal() {
		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
		distribuerTilKanal.setForsendelseId("1");
		return distribuerTilKanal;
	}


	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsJoark() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), ARKIV_SYSTEM_JOARK);
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsNotJoark() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), "NOT_JOARK");
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndWithoutArkivInformasjon() {
		return HentForsendelseResponse.builder()
				.bestillingsId(BESTILLINGS_ID)
				.konversasjonId(KONVERSASJON_ID)
				.mottaker(createMottakerTo())
				.forsendelseTittel(TITTEL)
				.dokumenter(buildHovedDokumentWithVedlegg())
				.build();
	}

	public static Mottaker createMottakerTo() {
		return Mottaker.builder()
				.mottakerId(MOTTAKER_FNR)
				.mottakerNavn(MOTTAKER_TYPE)
				.mottakerType(MOTTAKER_TYPE)
				.build();
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsNull() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), null);
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithEmptyDokumentList() {
		return buildHentForsendelseResponse(new ArrayList<>(), ARKIV_INFORMASJON_ARKIV_SYSTEM_JOARK);
	}

	public static HentForsendelseResponse buildHentForsendelseResponse(
			List<Dokument> dokumenter,
			String arkivSystem
	) {
		return HentForsendelseResponse
				.builder()
				.dokumenter(dokumenter)
				.bestillingsId(HENT_FORSENDELSE_RESPONSE_BESTILLINGS_ID)
				.arkivInformasjon(
						ArkivInformasjon
								.builder()
								.arkivId(ARKIV_INFORMASJON_ARKIV_ID)
								.arkivSystem(arkivSystem)
								.build()
				)
				.build();
	}

	public static List<Dokument> buildHovedDokumentWithVedlegg() {
		List<Dokument> dokumenter = new ArrayList<>();
		dokumenter.add(
				Dokument.builder()
						.tilknyttetSom(HOVEDDOKUMENT)
						.arkivDokumentInfoId(HOVED_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(HOVED_DOKUMENT_REF)
						.dokumenttypeId(DOKUMENTTYPE_ID)
						.build()
		);
		dokumenter.add(
				Dokument.builder()
						.tilknyttetSom(VEDLEGG)
						.arkivDokumentInfoId(VEDLEGG_1_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(VEDLEGG_1_DOKUMENT_REF)
						.build()
		);
		dokumenter.add(
				Dokument.builder()
						.tilknyttetSom(VEDLEGG)
						.arkivDokumentInfoId(VEDLEGG_2_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(VEDLEGG_2_DOKUMENT_REF)
						.build()
		);
		return dokumenter;
	}

	public static Set<String> makePreferertKanalSet(String... preferertKanal) {
		return Arrays.stream(preferertKanal).collect(Collectors.toSet());
	}

	public static Map<String, String> varslingsTekster(String epostVarslingsTekst, String smsVarslingsTekst) {
		Map<String, String> varslingsMap = new HashMap<>();
		varslingsMap.put(EPOST, epostVarslingsTekst);
		varslingsMap.put(SMS, smsVarslingsTekst);
		return varslingsMap;
	}

	@SneakyThrows
	public static String classpathFilesToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource("__files/" + classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}

	@SneakyThrows
	public static String classpathToString(String classpathResource) {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
		return message;
	}
}
