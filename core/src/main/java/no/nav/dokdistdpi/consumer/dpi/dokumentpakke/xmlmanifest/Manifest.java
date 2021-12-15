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
import java.util.List;

@Data
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
		name = "Manifest",
		propOrder = {"mottaker", "avsender", "hoveddokument", "vedleggs"}
)
@XmlRootElement(
		name = "manifest"
)
@NoArgsConstructor
@AllArgsConstructor
public class Manifest {
	@XmlElement(required = true)
	private Mottaker mottaker;
	@XmlElement(required = true)
	private Avsender avsender;
	@XmlElement(required = true)
	private Dokument hoveddokument;
	@XmlElement
	private List<Dokument>  vedleggs;
}
