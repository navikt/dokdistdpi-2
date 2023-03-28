package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.ArkivInformasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Dokument;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Postadresse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo.ArkivInformasjonTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo.DokumentTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo.MottakerTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo.PostadresseTo;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotNull;
import static org.springframework.util.ObjectUtils.isEmpty;

public class OpprettForsendelseMapper {

	private static final String DISTRIBUSJON_KANAL_PRINT = "PRINT";
	private static final String DOKUMENTTYPE_ID = "U000001";
	private static final String HOVEDDOKUMENT = "HOVEDDOKUMENT";

	public OpprettForsendelseRequestTo map(HentForsendelseResponse hentForsendelseResponse, String bestillingsId) {
		assertNotBlank("bestillingsId", bestillingsId);

		if (hentForsendelseResponse == null) {
			throw new IllegalArgumentException("HentForsendelseResponseTo kan ikke være null");
		}

		assertThatAllRequiredFieldsArePresent(hentForsendelseResponse);
		AtomicReference<Integer> rekkefolge = new AtomicReference<>(2);

		return OpprettForsendelseRequestTo.builder()
				.bestillingsId(bestillingsId)
				.distribusjonsKanal(DISTRIBUSJON_KANAL_PRINT)
				.tema(hentForsendelseResponse.getTema())
				.forsendelseTittel(hentForsendelseResponse.getForsendelseTittel())
				.bestillendeFagsystem(hentForsendelseResponse.getBestillendeFagsystem())
				.batchId(hentForsendelseResponse.getBatchId())
				.dokumentProdApp(hentForsendelseResponse.getDokumentProdApp())
				.originalDistribusjonId(hentForsendelseResponse.getBestillingsId())
				.mottaker(mapMottakerTo(hentForsendelseResponse.getMottaker()))
				.arkivInformasjon(mapArkivInformasjonTo(hentForsendelseResponse.getArkivInformasjon()))
				.postadresse(mapPostadresse(hentForsendelseResponse.getPostadresse()))
				.dokumenter(hentForsendelseResponse.getDokumenter().stream()
						.map(dokumentTo -> {
							if (isHoveddokument(dokumentTo.getTilknyttetSom())) {
								return mapDokument(dokumentTo, 1);
							} else {
								DokumentTo dok = mapDokument(dokumentTo, rekkefolge.get());
								rekkefolge.getAndSet(rekkefolge.get() + 1);
								return dok;
							}
						})
						.collect(Collectors.toList()))
				.build();
	}

	private DokumentTo mapDokument(Dokument dokument, Integer rekkefolge) {
		return DokumentTo.builder()
				.tilknyttetSom(dokument.getTilknyttetSom())
				.dokumentObjektReferanse(dokument.getDokumentObjektReferanse())
				.arkivDokumentInfoId(dokument.getArkivDokumentInfoId())
				.rekkefolge(rekkefolge)
				.dokumenttypeId(DOKUMENTTYPE_ID)
				.build();
	}

	private PostadresseTo mapPostadresse(Postadresse postadresse) {
		return isEmpty(postadresse) ? null : PostadresseTo.builder()
				.adresselinje1(postadresse.getAdresselinje1())
				.adresselinje2(postadresse.getAdresselinje2())
				.adresselinje3(postadresse.getAdresselinje3())
				.postnummer(postadresse.getPostnummer())
				.poststed(postadresse.getPoststed())
				.landkode(postadresse.getLandkode())
				.build();
	}

	private ArkivInformasjonTo mapArkivInformasjonTo(ArkivInformasjon arkivInformasjon) {
		return ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjon.getArkivSystem())
				.arkivId(arkivInformasjon.getArkivId())
				.build();
	}

	private MottakerTo mapMottakerTo(Mottaker mottaker) {
		assertNotNull("Mottaker", mottaker);
		return MottakerTo.builder()
				.mottakerId(mottaker.getMottakerId())
				.mottakerNavn(mottaker.getMottakerNavn())
				.mottakerType(mottaker.getMottakerType())
				.build();
	}

	private boolean isHoveddokument(String tilknyttetSom) {
		return HOVEDDOKUMENT.equals(tilknyttetSom);
	}

	private void assertThatAllRequiredFieldsArePresent(HentForsendelseResponse to) {
		assertNotNull("bestillingsId", to.getBestillingsId());
		assertNotNull("bestillendeFagsystem", to.getBestillendeFagsystem());
		assertNotNull("tema", to.getTema());
		assertNotNull("forsendelsetittel", to.getForsendelseTittel());
		assertNotNull("dokumentProdApp", to.getDokumentProdApp());
		assertNotNull("Mottaker", to.getMottaker());
		assertNotNull("mottaker.mottakerId", to.getMottaker().getMottakerId());
		assertNotNull("mottaker.mottakerNavn", to.getMottaker().getMottakerNavn());
		assertNotNull("mottaker.mottakerType", to.getMottaker().getMottakerType());

		if (to.getArkivInformasjon() != null) {
			assertNotNull("arkivinformasjon.arkivSystem", to.getArkivInformasjon().getArkivSystem());
			assertNotNull("arkivinformasjon.arkivId", to.getArkivInformasjon().getArkivId());
		}

		if (to.getPostadresse() != null) {
			assertNotNull("postadresse.landkode", to.getPostadresse().getLandkode());
		}

		assertThatAtLeastOneDocumentIsPresent(to.getDokumenter());
		to.getDokumenter().forEach(dokumentTo ->
				assertDokument(dokumentTo, to.getArkivInformasjon()));
	}

	private void assertDokument(Dokument dokument, ArkivInformasjon arkivInformasjon) {
		assertNotNull("dokumenter.dokument.tilknyttetSom", dokument.getTilknyttetSom());
		assertNotNull("dokumenter.dokument.dokumentObjektReferanse", dokument.getDokumentObjektReferanse());
		assertNotNull("dokumenter.dokument.dokumenttypeId", dokument.getDokumenttypeId());

		if (arkivInformasjon != null) {
			assertNotNull("dokumenter.dokument.arkivdokumentInfoId", dokument.getArkivDokumentInfoId());
		}
	}

	private void assertThatAtLeastOneDocumentIsPresent(List<Dokument> dokumentList) {
		if (dokumentList == null || dokumentList.isEmpty()) {
			throw new IllegalArgumentException("Ugyldig input: Feltet dokumenter må være en liste som inneholder minst ett dokumnet");
		}
	}
}
