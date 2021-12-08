package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;

public class ManifestCreator {

	private static final String DOKUMENT_LANG = "no";

	public String createManifest(Dokumentpakke dokumentpakke, final SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo) {
		Avsender avsender = Avsender.builder()
				.virksomhetsidentifikator(asIso6523(NAV_ORGNUMMER))
				.avsenderindentifikator(NAV_ORGNUMMER)
				.build();
		Mottaker mottaker = Mottaker.builder()
				.person(Person.builder()
						.personidentifikator(sikkerDigitalKontaktInfo.getPersonident())
						.postkasseadresse(sikkerDigitalKontaktInfo.getBrukerAdresse())
						.build())
				.build();
		Dokument hoveddokument = Dokument.builder()
				.href(dokumentpakke.getHoveddokument().getFilnavn())
				.data(DokumentData.builder()
						.mime(dokumentpakke.getHoveddokument().getMimeType())
						.build())
				.tittel(Dokument.Tittel.builder()
						.tittel(dokumentpakke.getHoveddokument().getTitle())
						.lang(DOKUMENT_LANG)
						.build())
				.mime(dokumentpakke.getHoveddokument().getMimeType())
				.build();
		List<Dokument> vedleggs = dokumentpakke.getVedlegg().stream().map(vedlegg ->
				Dokument.builder()
						.data(DokumentData.builder()
								.build())
						.tittel(Dokument.Tittel.builder()
								.lang(DOKUMENT_LANG)
								.tittel(vedlegg.getTitle())
								.build())
						.mime(vedlegg.getMimeType())
						.href(vedlegg.getFilnavn())
						.build()
		).collect(Collectors.toList());
		Manifest xmlManifest = Manifest.builder()
				.avsender(avsender)
				.mottaker(mottaker)
				.hoveddokument(hoveddokument)
				.vedleggs(vedleggs)
				.build();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(xmlManifest, os);
		return new String(os.toByteArray(), StandardCharsets.UTF_8);
	}
}
