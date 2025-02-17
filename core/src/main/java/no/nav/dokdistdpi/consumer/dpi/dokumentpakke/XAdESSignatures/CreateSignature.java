package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.exception.technical.KonfigurasjonException;
import no.nav.dokdistdpi.exception.technical.XMLXAdESSignaturesException;
import no.nav.dokdistdpi.exception.technical.XmlKonfigurasjonException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.xml.validation.SchemaLoaderUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha256;
import static org.springframework.xml.validation.XmlValidatorFactory.SCHEMA_W3C_XML;

@Component
public class CreateSignature {

	private static final String C14V1 = CanonicalizationMethod.INCLUSIVE;
	private static final String ASIC_NAMESPACE = "http://uri.etsi.org/2918/v1.2.1#";
	private static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";
	private static final String ASICE_SCHEMA_LOCATION = "asic-e/ts_102918v010201.xsd";

	private final DigestMethod sha256DigestMethod;
	private final CanonicalizationMethod canonicalizationMethod;
	private final Transform canonicalXmlTransform;

	private final DomUtils domUtils;
	private final CreateXAdESArtifacts createXAdESProperties;
	private final Schema schema;

	public CreateSignature() {
		this(new CreateXAdESArtifacts());
	}

	public CreateSignature(CreateXAdESArtifacts createXAdESProperties) {
		this.domUtils = new DomUtils();
		this.createXAdESProperties = createXAdESProperties;
		try {
			XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
			this.sha256DigestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
			this.canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(C14V1, (C14NMethodParameterSpec) null);
			this.canonicalXmlTransform = xmlSignatureFactory.newTransform(C14V1, (TransformParameterSpec) null);
		} catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
			throw new KonfigurasjonException("Kunne ikke initialisere xml-signering, " + e.getClass().getSimpleName() + ": '" + e.getMessage() + "'", e);
		}

		this.schema = loadSchema();
	}

	private static Schema loadSchema() {
		try {
			return SchemaLoaderUtils.loadSchema(new Resource[]{new ClassPathResource(ASICE_SCHEMA_LOCATION)}, SCHEMA_W3C_XML);
		} catch (IOException | SAXException e) {
			throw new KonfigurasjonException("Kunne ikke laste schema for validering av signatures, " + e.getClass().getSimpleName() + ": '" + e.getMessage() + "'", e);
		}
	}

	public XAdESSignatures createSignature(final AppCertificate appCertificate, final List<AsicEVedlegg> attachedFiles) throws XmlValideringException {
		XMLSignatureFactory xmlSignatureFactory = getSignatureFactory();
		SignatureMethod signatureMethod = getSignatureMethod(xmlSignatureFactory);

		// Generer XAdES-dokument som skal signeres, informasjon om sertifikat brukt til signering og informasjon om hva som er signert
		XAdESArtifacts xadesArtifacts = createXAdESProperties.createArtifactsToSign(attachedFiles, appCertificate);

		// Lag signatur-referanse for alle filer
		List<Reference> references = references(xmlSignatureFactory, attachedFiles);

		// Lag signatur-referanse for XaDES properties
		references.add(xmlSignatureFactory.newReference(
				xadesArtifacts.signablePropertiesReferenceUri,
				sha256DigestMethod,
				singletonList(canonicalXmlTransform),
				SIGNED_PROPERTIES_TYPE,
				null
		));

		KeyInfo keyInfo = keyInfo(xmlSignatureFactory, appCertificate.getCertificateList());
		SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

		// Definer signatur over XAdES-dokument
		XMLObject xmlObject = xmlSignatureFactory.newXMLObject(singletonList(new DOMStructure(xadesArtifacts.document.getDocumentElement())), null, null, null);
		XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, singletonList(xmlObject), "Signature", null);

		Document signedDocument = domUtils.newEmptyXmlDocument();
		DOMSignContext signContext = new DOMSignContext(appCertificate.getKeyPair().getPrivate(), addXAdESSignaturesElement(signedDocument));
		signContext.setURIDereferencer(signedPropertiesURIDereferencer(xadesArtifacts, xmlSignatureFactory));

		try {
			xmlSignature.sign(signContext);
		} catch (MarshalException e) {
			throw new XmlKonfigurasjonException("Klarte ikke å lese ASiC-E XML for signering", e);
		} catch (XMLSignatureException e) {
			throw new XMLXAdESSignaturesException("Klarte ikke å signere ASiC-E element.", e);
		}

		try {
			schema.newValidator().validate(new DOMSource(signedDocument));
		} catch (SAXException | IOException e) {
			throw new XmlValideringException(
					"Feilet til å validere signature.xml " + e.getClass().getSimpleName() + ": '" + e.getMessage(), e);
		}
		return new XAdESSignatures(domUtils.serializeToXml(signedDocument));
	}

	private URIDereferencer signedPropertiesURIDereferencer(XAdESArtifacts xadesArtifacts, XMLSignatureFactory signatureFactory) {
		return (uriReference, context) -> {
			if (xadesArtifacts.signablePropertiesReferenceUri.equals(uriReference.getURI())) {
				return (NodeSetData) domUtils.allNodesBelow(xadesArtifacts.signableProperties)::iterator;
			}
			return signatureFactory.getURIDereferencer().dereference(uriReference, context);
		};
	}

	private static Element addXAdESSignaturesElement(Document doc) {
		return (Element) doc.appendChild(doc.createElementNS(ASIC_NAMESPACE, "XAdESSignatures"));
	}

	private static SignatureMethod getSignatureMethod(final XMLSignatureFactory xmlSignatureFactory) {
		try {
			return xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
		} catch (NoSuchAlgorithmException e) {
			throw new KonfigurasjonException("Kunne ikke initialisere xml-signering", e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new XMLXAdESSignaturesException("Kunne ikke initialisere xml-signering", e);
		}
	}

	private List<Reference> references(final XMLSignatureFactory xmlSignatureFactory, final List<AsicEVedlegg> files) {
		AtomicInteger count = new AtomicInteger(0);
		return files.stream()
				.map(file -> {
					String signatureElementId = "ID_" + count.getAndIncrement();
					String uri = URLEncoder.encode(file.getFileName(), UTF_8);
					return xmlSignatureFactory.newReference(uri, sha256DigestMethod, null, null, signatureElementId, sha256(file.getBytes()));
				}).collect(toList());
	}

	private static KeyInfo keyInfo(final XMLSignatureFactory xmlSignatureFactory, final Certificate[] sertifikater) {
		KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
		X509Data x509Data = keyInfoFactory.newX509Data(asList(sertifikater));
		return keyInfoFactory.newKeyInfo(singletonList(x509Data));
	}

	private static XMLSignatureFactory getSignatureFactory() {
		try {
			return XMLSignatureFactory.getInstance("DOM", "XMLDSig");
		} catch (NoSuchProviderException e) {
			throw new KonfigurasjonException("Fant ikke XML Digital Signature-provider. Biblioteket avhenger av default Java-provider.");
		}
	}
}
