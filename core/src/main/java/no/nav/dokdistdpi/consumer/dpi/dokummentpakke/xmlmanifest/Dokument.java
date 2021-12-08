package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@Data
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "Dokument",
		propOrder = {"tittel", "data"}
)
@XmlRootElement(name = "dokument")
@NoArgsConstructor
@AllArgsConstructor
public class Dokument {
	@XmlElement
	protected Tittel tittel;
	@XmlElement
	protected DokumentData data;
	@XmlAttribute(
			name = "href",
			required = true
	)
	protected String href;
	@XmlAttribute(
			name = "mime",
			required = true
	)
	protected String mime;

	@Data
	@Builder
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "tittel")
	@XmlRootElement(name = "tittel")
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Tittel {
		@XmlValue
		@SuppressWarnings("squid:S1700")
		private String tittel;
		@XmlAttribute
		private String lang;
	}
}
