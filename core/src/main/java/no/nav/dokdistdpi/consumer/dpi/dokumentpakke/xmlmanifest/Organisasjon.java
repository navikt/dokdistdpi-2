package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@Data
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Organisasjon")
@XmlRootElement(name = "organisasjon")
@NoArgsConstructor
@AllArgsConstructor
public class Organisasjon {
	@XmlAttribute
	private String authority;
	@XmlValue
	private String orgNummer;
}
