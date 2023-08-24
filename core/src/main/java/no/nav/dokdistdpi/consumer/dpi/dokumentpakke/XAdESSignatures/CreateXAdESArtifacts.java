package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.etsi.uri._01903.v1_3.CertIDType;
import org.etsi.uri._01903.v1_3.DataObjectFormatType;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingPropertiesType;
import org.etsi.uri._01903.v1_3.SignedDataObjectPropertiesType;
import org.etsi.uri._01903.v1_3.SignedPropertiesType;
import org.etsi.uri._01903.v1_3.SignedSignaturePropertiesType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3._2000._09.xmldsig_.DigestMethodType;
import org.w3._2000._09.xmldsig_.X509IssuerSerialType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static javax.xml.crypto.dsig.DigestMethod.SHA1;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

@Slf4j
@Component
public class CreateXAdESArtifacts {

	private static final DigestMethodType sha1DigestMethod = new DigestMethodType();
	private final Clock clock;

	@Autowired
	CreateXAdESArtifacts(Clock clock) {
		this.clock = clock;
	}

	XAdESArtifacts createArtifactsToSign(List<AsicEVedlegg> files, AppCertificate appCertificate) {
		sha1DigestMethod.setAlgorithm(SHA1);

		try {
			byte[] certificateDigestValue = sha1(appCertificate.getX509Certificate().getEncoded());
			X509Certificate certificate = appCertificate.getX509Certificate();

			DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType();
			certificateDigest.setDigestMethod(sha1DigestMethod);
			certificateDigest.setDigestValue(certificateDigestValue);
			X509IssuerSerialType certificateIssuer = new X509IssuerSerialType();
			certificateIssuer.setX509IssuerName(certificate.getIssuerX500Principal().getName());
			certificateIssuer.setX509SerialNumber(certificate.getSerialNumber());
			CertIDType certIDType = new CertIDType();
			certIDType.setCertDigest(certificateDigest);
			certIDType.setIssuerSerial(certificateIssuer);

			SignedSignaturePropertiesType signedSignatureProperties = new SignedSignaturePropertiesType();
			signedSignatureProperties.setSigningTime(getSigningTime());
			signedSignatureProperties.setSigningCertificate(singletonList(certIDType));

			SignedDataObjectPropertiesType signedDataObjectProperties = new SignedDataObjectPropertiesType();
			signedDataObjectProperties.getDataObjectFormat().addAll(dataObjectFormats(files));

			SignedPropertiesType signedProperties = new SignedPropertiesType();
			signedProperties.setSignedSignatureProperties(signedSignatureProperties);
			signedProperties.setSignedDataObjectProperties(signedDataObjectProperties);
			signedProperties.setId("SignedProperties");

			QualifyingPropertiesType qualifyingProperties = new QualifyingPropertiesType();
			qualifyingProperties.setSignedProperties(signedProperties);
			qualifyingProperties.setTarget("#Signature");

			return XAdESArtifacts.from(qualifyingProperties);
		} catch (CertificateEncodingException e) {
			log.error("Could not get encoded certificate", e);
			throw new SikkerDigitalPostException("Could not get encoded certificate", e);
		}
	}

	private static List<DataObjectFormatType> dataObjectFormats(List<AsicEVedlegg> files) {
		AtomicInteger count = new AtomicInteger(0);
		return files.stream().map(file -> {
			String signatureElementIdReference = "#ID_" + count.getAndIncrement();
			DataObjectFormatType dataObjectFormatType = new DataObjectFormatType();
			dataObjectFormatType.setMimeType(file.getMimeType());
			dataObjectFormatType.setObjectReference(signatureElementIdReference);
			return dataObjectFormatType;
		}).toList();
	}

	private XMLGregorianCalendar getSigningTime() {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("Could not get signing time", e);
		}
	}
}
