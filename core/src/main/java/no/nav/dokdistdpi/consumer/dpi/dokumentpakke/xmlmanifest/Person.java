package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

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
@XmlType( name = "person",
		propOrder = {"personidentifikator", "postkasseadresse"})
@XmlRootElement(name = "person")
@NoArgsConstructor
@AllArgsConstructor
public class Person {
	@XmlElement
	protected String personidentifikator;
	@XmlElement(required = true)
	protected String postkasseadresse;

}
