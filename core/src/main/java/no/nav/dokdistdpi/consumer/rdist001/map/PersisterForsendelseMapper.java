package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.PersisterForsendelseRequestTo;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotNull;
import static org.springframework.util.ObjectUtils.isEmpty;

public class PersisterForsendelseMapper {
	private static final String DISTRIBUSJON_KANAL_PRINT = "PRINT";
	private static final String DOKUMENTTYPE_ID = "U000001";
	private static final String HOVEDDOKUMENT = "HOVEDDOKUMENT";

	public PersisterForsendelseRequestTo map(HentForsendelseResponse hentForsendelseResponse, String bestillingsId) {
		assertNotBlank("bestillingsId", bestillingsId);
		if (hentForsendelseResponse == null) {
			throw new IllegalArgumentException("HentForsendelseResponseTo kan ikke være null");
		}
		assertThatAllRequiredFieldsArePresent(hentForsendelseResponse);
		AtomicReference<Integer> rekkefolge = new AtomicReference<>(2);
		return PersisterForsendelseRequestTo.builder()
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
								PersisterForsendelseRequestTo.DokumentTo dok = mapDokument(dokumentTo, rekkefolge.get());
								rekkefolge.getAndSet(rekkefolge.get() + 1);
								return dok;
							}
						})
						.collect(Collectors.toList()))
				.build();
	}

	private PersisterForsendelseRequestTo.DokumentTo mapDokument(HentForsendelseResponse.DokumentTo dokumentTo, Integer rekkefolge) {
		return PersisterForsendelseRequestTo.DokumentTo.builder()
				.tilknyttetSom(dokumentTo.getTilknyttetSom())
				.dokumentObjektReferanse(dokumentTo.getDokumentObjektReferanse())
				.arkivDokumentInfoId(dokumentTo.getArkivDokumentInfoId())
				.rekkefolge(rekkefolge)
				.dokumenttypeId(DOKUMENTTYPE_ID)
				.build();
	}

	private PersisterForsendelseRequestTo.PostadresseTo mapPostadresse(HentForsendelseResponse.PostadresseTo postadresseTo) {
		return isEmpty(postadresseTo) ? null : PersisterForsendelseRequestTo.PostadresseTo.builder()
				.adresselinje1(postadresseTo.getAdresselinje1())
				.adresselinje2(postadresseTo.getAdresselinje2())
				.adresselinje3(postadresseTo.getAdresselinje3())
				.postnummer(postadresseTo.getPostnummer())
				.poststed(postadresseTo.getPoststed())
				.landkode(postadresseTo.getLandkode())
				.build();
	}

	private PersisterForsendelseRequestTo.ArkivInformasjonTo mapArkivInformasjonTo(HentForsendelseResponse.ArkivInformasjonTo arkivInformasjonTo) {
		return PersisterForsendelseRequestTo.ArkivInformasjonTo.builder()
				.arkivSystem(arkivInformasjonTo.getArkivSystem())
				.arkivId(arkivInformasjonTo.getArkivId())
				.build();
	}

	private PersisterForsendelseRequestTo.MottakerTo mapMottakerTo(HentForsendelseResponse.MottakerTo mottakerTo) {
		assertNotNull("Mottaker", mottakerTo);
		return PersisterForsendelseRequestTo.MottakerTo.builder()
				.mottakerId(mottakerTo.getMottakerId())
				.mottakerNavn(mottakerTo.getMottakerNavn())
				.mottakerType(mottakerTo.getMottakerType())
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

	private void assertDokument(HentForsendelseResponse.DokumentTo dokumentTo, HentForsendelseResponse.ArkivInformasjonTo arkivInformasjonTo) {
		assertNotNull("dokumenter.dokument.tilknyttetSom", dokumentTo.getTilknyttetSom());
		assertNotNull("dokumenter.dokument.dokumentObjektReferanse", dokumentTo.getDokumentObjektReferanse());
		assertNotNull("dokumenter.dokument.dokumenttypeId", dokumentTo.getDokumenttypeId());
		if (arkivInformasjonTo != null) {
			assertNotNull("dokumenter.dokument.arkivdokumentInfoId", dokumentTo.getArkivDokumentInfoId());
		}
	}

	private void assertThatAtLeastOneDocumentIsPresent(List<HentForsendelseResponse.DokumentTo> dokumentToList) {
		if (dokumentToList == null || dokumentToList.isEmpty()) {
			throw new IllegalArgumentException("Ugyldig input: Feltet dokumenter må være en liste som inneholder minst ett dokumnet");
		}
	}
}
