package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.difi.begrep.sdp.schema_v10.Manifest;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDistribuereForsendelseException;

import java.io.OutputStream;

@Slf4j
@UtilityClass
final class MarshalManifest {
	private static final JAXBContext jaxbContext;

	static {
		try {
			// JAXBContext implementasjoner skal være trådsikre
			jaxbContext = JAXBContext.newInstance(Manifest.class);
		} catch (JAXBException e) {
			throw new IllegalStateException("Klarte ikke sette opp JAXBContext", e);
		}
	}

	static void marshal(Manifest doc, OutputStream os) {
		try {
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(doc, os);
		} catch (JAXBException e) {
			throw new KunneIkkeDistribuereForsendelseException("Klarte ikke marshalle Manifest", e);
		}
	}
}