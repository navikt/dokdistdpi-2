package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.ISO6523_AUTHORITY;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.ISO6523_PREFIX;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Organisasjon")
@XmlRootElement(name = "organisasjon")
public class Organisasjon {
	@XmlAttribute
	private String authority;
	@XmlValue
	private String orgNummer;

	public Organisasjon(String orgNummer) {
		super();
		this.authority = ISO6523_AUTHORITY;
		this.orgNummer = ISO6523_PREFIX + orgNummer;
	}
}
