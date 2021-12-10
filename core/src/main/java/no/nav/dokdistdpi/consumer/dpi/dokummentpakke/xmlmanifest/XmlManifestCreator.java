package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;

public class XmlManifestCreator {

	private static final String DOKUMENT_LANG = "no";

	public String createManifest(Forsendelse forsendelse) {
		Dokumentpakke dokumentpakke = forsendelse.getDokumentpakke();
		DigitalPost digitalPostInfo = forsendelse.getDigital();

		Avsender avsender = Avsender.builder()
				.virksomhetsidentifikator(asIso6523(NAV_ORGNUMMER))
				.avsenderindentifikator(NAV_ORGNUMMER)
				.build();
		Mottaker mottaker = Mottaker.builder()
				.person(Person.builder()
						.personidentifikator(forsendelse.getPersonidentifikator())
						.postkasseadresse(digitalPostInfo.getMottaker().getPostkasseadresse())
						.build())
				.build();
		Dokument hoveddokument = Dokument.builder()
				.href(dokumentpakke.getHoveddokument().getFilnavn())
				.data(DokumentData.builder()
						.mime(dokumentpakke.getHoveddokument().getMimeType())
						.build())
				.tittel(Dokument.Tittel.builder()
						.tittel(dokumentpakke.getHoveddokument().getTittle())
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
								.tittel(vedlegg.getTittle())
								.build())
						.mime(vedlegg.getMimeType())
						.href(vedlegg.getFilnavn())
						.build()
		).toList();
		Manifest xmlManifest = Manifest.builder()
				.avsender(avsender)
				.mottaker(mottaker)
				.hoveddokument(hoveddokument)
				.vedleggs(vedleggs)
				.build();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(xmlManifest, os);
		return os.toString(StandardCharsets.UTF_8);
	}
}
