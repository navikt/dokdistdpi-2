package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@Data
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DokumentData")
@XmlRootElement(name = "DokumentData")
@NoArgsConstructor
@AllArgsConstructor
public class DokumentData {
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
}
