package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import no.difi.begrep.sdp.schema_v10.Avsender;
import no.difi.begrep.sdp.schema_v10.Dokument;
import no.difi.begrep.sdp.schema_v10.Iso6523Authority;
import no.difi.begrep.sdp.schema_v10.Manifest;
import no.difi.begrep.sdp.schema_v10.Mottaker;
import no.difi.begrep.sdp.schema_v10.Organisasjon;
import no.difi.begrep.sdp.schema_v10.Person;
import no.difi.begrep.sdp.schema_v10.Tittel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;

public class XmlManifestCreator {

	private static final String DOKUMENT_LANG = "no";

	public DpiManifest createManifest(Forsendelse forsendelse) {
		Dokumentpakke dokumentpakke = forsendelse.getDokumentpakke();
		DigitalPost digitalPostInfo = forsendelse.getDigital();

		Organisasjon organisasjon = new Organisasjon()
				.withAuthority(Iso6523Authority.ISO_6523_ACTORID_UPIS)
				.withValue(asIso6523(NAV_ORGNUMMER));

		Avsender avsender = new Avsender()
				.withOrganisasjon(organisasjon);

		Person person = new Person()
				.withPersonidentifikator(forsendelse.getPersonidentifikator())
				.withPostkasseadresse(digitalPostInfo.getMottaker().getPostkasseadresse());
		Mottaker mottaker = new Mottaker()
				.withPerson(person);

		Dokument hoveddokument = mapDokument(dokumentpakke.getHoveddokument());

		List<Dokument> vedlegg = dokumentpakke.getVedlegg().stream()
				.map(this::mapDokument)
				.toList();

		Manifest manifest = new Manifest()
				.withAvsender(avsender)
				.withMottaker(mottaker)
				.withHoveddokument(hoveddokument)
				.withVedlegg(vedlegg);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(manifest, os);
		return new DpiManifest(os.toByteArray());
	}

	private Dokument mapDokument(DpiDokument dpiDokument) {
		Tittel tittel = new Tittel();
		tittel.setLang(DOKUMENT_LANG);
		tittel.setValue(dpiDokument.getTittel());
		return new Dokument()
				.withTittel(tittel)
				.withMime(dpiDokument.getMimeType())
				.withHref(dpiDokument.getFilnavn());
	}
}
