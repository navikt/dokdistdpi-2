package no.nav.dokdistdpi;

import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.saf.JournalpostQdist011;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
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

	public static List<JournalpostQdist011.DokumentInfo> buildDokumentInfo() {
		List<JournalpostQdist011.DokumentInfo> dokumenter = new ArrayList<>();
		dokumenter.add(
				JournalpostQdist011
						.DokumentInfo
						.builder()
						.dokumentInfoId("2")
						.tittel("Joark vedlegg tittel 1")
						.build()
		);
		dokumenter.add(
				JournalpostQdist011
						.DokumentInfo
						.builder()
						.dokumentInfoId("3")
						.tittel("Joark vedlegg tittel 2")
						.build()
		);
		return dokumenter;
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsJoark() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), ARKIV_SYSTEM_JOARK);
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsNotJoark() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), "NOT_JOARK");
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndWithoutArkivInformasjon() {
		return HentForsendelseResponse
				.builder()
				.dokumenter(buildHovedDokumentWithVedlegg())
				.bestillingsId(HENT_FORSENDELSE_RESPONSE_BESTILLINGS_ID)
				.build();
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithDokumentAndArkivSystemAsNull() {
		return buildHentForsendelseResponse(buildHovedDokumentWithVedlegg(), null);
	}

	public static HentForsendelseResponse buildHentForsendelseResponseWithEmptyDokumentList() {
		return buildHentForsendelseResponse(new ArrayList<>(), ARKIV_INFORMASJON_ARKIV_SYSTEM_JOARK);
	}

	public static HentForsendelseResponse buildHentForsendelseResponse(
			List<HentForsendelseResponse.DokumentTo> dokumenter,
			String arkivSystem
	) {
		return HentForsendelseResponse
				.builder()
				.dokumenter(dokumenter)
				.bestillingsId(HENT_FORSENDELSE_RESPONSE_BESTILLINGS_ID)
				.arkivInformasjon(
						HentForsendelseResponse.ArkivInformasjonTo
								.builder()
								.arkivId(ARKIV_INFORMASJON_ARKIV_ID)
								.arkivSystem(arkivSystem)
								.build()
				)
				.build();
	}

	public static List<HentForsendelseResponse.DokumentTo> buildHovedDokumentWithVedlegg() {
		List<HentForsendelseResponse.DokumentTo> dokumenter = new ArrayList<>();
		dokumenter.add(
				HentForsendelseResponse.DokumentTo.builder()
						.tilknyttetSom(HOVEDDOKUMENT)
						.arkivDokumentInfoId(HOVED_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(HOVED_DOKUMENT_REF)
						.build()
		);
		dokumenter.add(
				HentForsendelseResponse.DokumentTo.builder()
						.tilknyttetSom(VEDLEGG)
						.arkivDokumentInfoId(VEDLEGG_1_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(VEDLEGG_1_DOKUMENT_REF)
						.build()
		);
		dokumenter.add(
				HentForsendelseResponse.DokumentTo.builder()
						.tilknyttetSom(VEDLEGG)
						.arkivDokumentInfoId(VEDLEGG_2_DOKUMENT_INFO_ID)
						.dokumentObjektReferanse(VEDLEGG_2_DOKUMENT_REF)
						.build()
		);
		return dokumenter;
	}

	public static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);
	}

	public static Set<String> makePreferertKanalSet(String... preferertKanal) {
		Set<String> set = new HashSet<String>();

		for (String kanal : preferertKanal) {
			set.add(kanal);
		}
		return set;
	}

	public static Map<String, String> varslingsTekster(String epostVarslingsTekst, String smsVarslingsTekst) {
		Map<String, String> varslingsMap = new HashMap<String, String>();
		varslingsMap.put(EPOST, epostVarslingsTekst);
		varslingsMap.put(SMS, smsVarslingsTekst);
		return varslingsMap;
	}

	public static JournalpostQdist011 createJournalpostQdist011(){
		return JournalpostQdist011.builder()
				.dokumenter(Arrays.asList(JournalpostQdist011.DokumentInfo.builder().dokumentInfoId(HOVED_DOKUMENT_INFO_ID).tittel(TITTEL).build()))
				.build();
	}
}
