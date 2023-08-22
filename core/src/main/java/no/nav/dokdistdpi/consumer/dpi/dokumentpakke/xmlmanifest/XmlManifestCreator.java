package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import no.difi.begrep.sdp.schema_v10.Avsender;
import no.difi.begrep.sdp.schema_v10.Dokument;
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

import static no.difi.begrep.sdp.schema_v10.Iso6523Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;

public class XmlManifestCreator {

	private static final String DOKUMENT_LANG = "no";

	public DpiManifest createManifest(Forsendelse forsendelse) {
		Dokumentpakke dokumentpakke = forsendelse.getDokumentpakke();
		DigitalPost digitalPostInfo = forsendelse.getDigital();

		Organisasjon organisasjon = new Organisasjon();
		organisasjon.setAuthority(ISO_6523_ACTORID_UPIS);
		organisasjon.setValue(asIso6523(NAV_ORGNUMMER));

		Avsender avsender = new Avsender();
		avsender.setOrganisasjon(organisasjon);

		Person person = new Person();
		person.setPersonidentifikator(forsendelse.getPersonidentifikator());
		person.setPostkasseadresse(digitalPostInfo.getMottaker().getPostkasseadresse());
		Mottaker mottaker = new Mottaker();
		mottaker.setPerson(person);

		Dokument hoveddokument = mapDokument(dokumentpakke.getHoveddokument());

		Manifest manifest = new Manifest();

		manifest.setAvsender(avsender);
		manifest.setMottaker(mottaker);
		manifest.setHoveddokument(hoveddokument);
		dokumentpakke.getVedlegg().stream().forEach(dokument ->
				manifest.getVedlegg().add(mapDokument(dokument))
		);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MarshalManifest.marshal(manifest, os);
		return new DpiManifest(os.toByteArray());
	}

	private Dokument mapDokument(DpiDokument dpiDokument) {
		Tittel tittel = new Tittel();
		tittel.setLang(DOKUMENT_LANG);
		tittel.setValue(dpiDokument.getTittel());
		Dokument dokument = new Dokument();
		dokument.setTittel(tittel);
		dokument.setMime(dpiDokument.getMimeType());
		dokument.setHref(dpiDokument.getFilnavn());

		return dokument;
	}
}
