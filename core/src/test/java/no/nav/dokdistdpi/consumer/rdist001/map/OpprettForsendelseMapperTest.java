package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.ArkivInformasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Dokument;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Postadresse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class OpprettForsendelseMapperTest {

	private static final String BESTILLINGS_ID = UUID.randomUUID().toString();
	private static final String OLD_BESTILLINGS_ID = UUID.randomUUID().toString();
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String TEMA = "FS22";
	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String ARKIV_SYSTEM = "JOARK";
	private static final String ARKIV_ID = "arkivId";
	private static final String MOTTAKER_ID_NAVN = "mottakerIdNavn";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND = "land";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "U000001";
	private static final String DOKUMENTTYPE_ID_2 = "U000001";
	private static final String OBJEKT_REFERANSE_1 = "objektReferanse1";
	private static final String OBJEKT_REFERANSE_2 = "objektReferanse2";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";

	private final OpprettForsendelseMapper mapper = new OpprettForsendelseMapper();

	@Test
	public void shouldMapForsendelser() {
		OpprettForsendelseRequestTo request = mapper.map(createHentForsendelseResponse(), BESTILLINGS_ID);

		assertThat(request.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
		assertThat(request.getForsendelseTittel()).isEqualTo(FORSENDELSE_TITTEL);
		assertThat(request.getBatchId()).isEqualTo(BATCH_ID);
		assertThat(request.getDokumentProdApp()).isEqualTo(DOKUMENT_PROD_APP);
		assertThat(request.getBestillendeFagsystem()).isEqualTo(BESTILLENDE_FAGSYSTEM);
		assertThat(request.getArkivInformasjon().getArkivId()).isEqualTo(ARKIV_ID);
		assertThat(request.getMottaker().getMottakerId()).isEqualTo(MOTTAKER_ID);
		assertThat(request.getMottaker().getMottakerNavn()).isEqualTo(MOTTAKER_ID_NAVN);
		assertThat(request.getOriginalDistribusjonId()).isEqualTo(OLD_BESTILLINGS_ID);

		var postadresse = request.getPostadresse();
		assertThat(postadresse.getAdresselinje1()).isEqualTo(ADRESSELINJE_1);
		assertThat(postadresse.getAdresselinje2()).isEqualTo(ADRESSELINJE_2);
		assertThat(postadresse.getAdresselinje3()).isEqualTo(ADRESSELINJE_3);
		assertThat(postadresse.getPostnummer()).isEqualTo(POSTNUMMER);
		assertThat(postadresse.getPoststed()).isEqualTo(POSTSTED);
		assertThat(postadresse.getLandkode()).isEqualTo(LAND);

		var vedleggNr1 = request.getDokumenter().get(1);
		assertThat(vedleggNr1.getDokumenttypeId()).isEqualTo(DOKUMENTTYPE_ID_2);
		assertThat(vedleggNr1.getDokumentObjektReferanse()).isEqualTo(OBJEKT_REFERANSE_2);
		assertThat(vedleggNr1.getTilknyttetSom()).isEqualTo(TILKNYTTET_SOM_VEDLEGG);
		assertThat(vedleggNr1.getRekkefolge()).isEqualTo(2);
		assertThat(vedleggNr1.getArkivDokumentInfoId()).isEqualTo(ARKIV_DOKUMENTINFO_ID_2);
	}

	@Test
	public void shouldMapForsendelseWhenAdresseErNull() {
		HentForsendelseResponse hentForsendelseResponse = createHentForsendelseResponseWithPostadresseNull();
		OpprettForsendelseRequestTo request = mapper.map(hentForsendelseResponse, BESTILLINGS_ID);

		assertThat(request.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
		assertThat(request.getForsendelseTittel()).isEqualTo(FORSENDELSE_TITTEL);
		assertThat(request.getBatchId()).isEqualTo(BATCH_ID);
		assertThat(request.getDokumentProdApp()).isEqualTo(DOKUMENT_PROD_APP);
		assertThat(request.getBestillendeFagsystem()).isEqualTo(BESTILLENDE_FAGSYSTEM);
		assertThat(request.getArkivInformasjon().getArkivId()).isEqualTo(ARKIV_ID);
		assertThat(request.getMottaker().getMottakerId()).isEqualTo(MOTTAKER_ID);
		assertThat(request.getMottaker().getMottakerNavn()).isEqualTo(MOTTAKER_ID_NAVN);
		assertThat(request.getOriginalDistribusjonId()).isEqualTo(OLD_BESTILLINGS_ID);

		assertThat(request.getPostadresse()).isNull();

		var vedleggNr1 = request.getDokumenter().get(1);
		assertThat(vedleggNr1.getDokumenttypeId()).isEqualTo(DOKUMENTTYPE_ID_2);
		assertThat(vedleggNr1.getDokumentObjektReferanse()).isEqualTo(OBJEKT_REFERANSE_2);
		assertThat(vedleggNr1.getTilknyttetSom()).isEqualTo(TILKNYTTET_SOM_VEDLEGG);
		assertThat(vedleggNr1.getRekkefolge()).isEqualTo(2);
		assertThat(vedleggNr1.getArkivDokumentInfoId()).isEqualTo(ARKIV_DOKUMENTINFO_ID_2);
	}

	@Test
	public void shouldThrowExceptionIfHentForsendelseResponseIsNull() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> mapper.map(null, BESTILLINGS_ID))
				.withMessage("HentForsendelseResponseTo kan ikke være null");
	}

	@Test
	public void shouldThrowExceptionIfBestillingIdIsBlank() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> mapper.map(createHentForsendelseResponse(), null))
				.withMessage("bestillingsId kan ikke være null");
	}

	@Test
	public void shouldThrowExceptionIfMottakerIsNull() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> mapper.map(createHentForsendelseResponseWithMottakerNull(), BESTILLINGS_ID))
				.withMessage("Mottaker kan ikke være null");
	}

	private HentForsendelseResponse createHentForsendelseResponse() {
		return HentForsendelseResponse.builder()
				.bestillingsId(OLD_BESTILLINGS_ID)
				.tema(TEMA)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.batchId(BATCH_ID)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.arkivInformasjon(ArkivInformasjon.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID)
						.build())
				.mottaker(createMottaker())
				.postadresse(createPostadresse())
				.dokumenter(createDokument())
				.build();
	}

	private HentForsendelseResponse createHentForsendelseResponseWithMottakerNull() {
		return HentForsendelseResponse.builder()
				.bestillingsId(OLD_BESTILLINGS_ID)
				.tema(TEMA)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.batchId(BATCH_ID)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.arkivInformasjon(ArkivInformasjon.builder()
						.arkivId(ARKIV_ID)
						.build())
				.mottaker(null)
				.postadresse(createPostadresse())
				.dokumenter(createDokument())
				.build();
	}

	private HentForsendelseResponse createHentForsendelseResponseWithPostadresseNull() {
		return HentForsendelseResponse.builder()
				.bestillingsId(OLD_BESTILLINGS_ID)
				.tema(TEMA)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.batchId(BATCH_ID)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.arkivInformasjon(ArkivInformasjon.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID).build())
				.mottaker(createMottaker())
				.postadresse(null)
				.dokumenter(createDokument())
				.build();
	}

	private List<Dokument> createDokument() {
		return Arrays.asList(
				Dokument.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_1)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
						.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
						.build(),
				Dokument.builder()
						.dokumenttypeId(DOKUMENTTYPE_ID_2)
						.dokumentObjektReferanse(OBJEKT_REFERANSE_2)
						.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
						.build(),
				Dokument.builder()
						.dokumenttypeId("1234")
						.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
						.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
						.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
						.build());
	}

	private Postadresse createPostadresse() {
		return Postadresse.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.landkode(LAND)
				.build();
	}

	private Mottaker createMottaker() {
		return Mottaker.builder()
				.mottakerNavn(MOTTAKER_ID_NAVN)
				.mottakerId(MOTTAKER_ID)
				.mottakerType("PERSON")
				.build();
	}
}