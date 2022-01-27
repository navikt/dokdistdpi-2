package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import no.difi.begrep.sdp.schema_v10.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

@Slf4j
@UtilityClass
final class MarshalManifest {
	static void marshal(Manifest doc, OutputStream os) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Manifest.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(doc, os);
		} catch (JAXBException e) {
			log.error("Marshalling failed", e);
		}
	}
}