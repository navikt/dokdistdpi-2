package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@Data
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Avsender",
		propOrder = {"organisasjon", "avsenderidentifikator", "fakturaReferanse"})
@XmlRootElement(name = "avsender")
@NoArgsConstructor
@AllArgsConstructor
public class Avsender {
	@XmlElement
	private Organisasjon organisasjon;
	@XmlElement
	private String avsenderidentifikator;
	@XmlElement
	private String fakturaReferanse;
}
