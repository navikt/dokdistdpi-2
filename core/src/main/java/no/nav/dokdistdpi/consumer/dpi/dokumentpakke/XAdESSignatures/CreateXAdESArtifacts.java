package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.etsi.uri._01903.v1_3.CertIDType;
import org.etsi.uri._01903.v1_3.DataObjectFormat;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingProperties;
import org.etsi.uri._01903.v1_3.SignedDataObjectProperties;
import org.etsi.uri._01903.v1_3.SignedProperties;
import org.etsi.uri._01903.v1_3.SignedSignatureProperties;
import org.springframework.stereotype.Component;
import org.w3._2000._09.xmldsig_.DigestMethod;
import org.w3._2000._09.xmldsig_.X509IssuerSerialType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static javax.xml.crypto.dsig.DigestMethod.SHA1;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

@Slf4j
@Component
public class CreateXAdESArtifacts {

	static XAdESArtifacts createArtifactsToSign(List<AsicEVedlegg> files, AppCertificate appCertificate) {

		try {
			final DigestMethod sha1DigestMethod = new DigestMethod();
			sha1DigestMethod.setAlgorithm(SHA1);

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

			SignedSignatureProperties signedSignatureProperties = new SignedSignatureProperties();
			signedSignatureProperties.setSigningTime(getSigningTime());
			signedSignatureProperties.setSigningCertificate(singletonList(certIDType));

			SignedDataObjectProperties signedDataObjectProperties = new SignedDataObjectProperties();
			signedDataObjectProperties.getDataObjectFormats().addAll(dataObjectFormats(files));

			SignedProperties signedProperties = new SignedProperties();
			signedProperties.setSignedSignatureProperties(signedSignatureProperties);
			signedProperties.setSignedDataObjectProperties(signedDataObjectProperties);
			signedProperties.setId("SignedProperties");

			QualifyingProperties qualifyingProperties = new QualifyingProperties();
			qualifyingProperties.setSignedProperties(signedProperties);
			qualifyingProperties.setTarget("#Signature");

			return XAdESArtifacts.from(qualifyingProperties);
		} catch (CertificateEncodingException e) {
			log.error("Could not get encoded certificate", e);
			throw new SikkerDigitalPostException("Could not get encoded certificate", e);
		}
	}

	private static List<DataObjectFormat> dataObjectFormats(List<AsicEVedlegg> files) {
		AtomicInteger count = new AtomicInteger(0);
		return files.stream().map(file -> {
			String signatureElementIdReference = "#ID_" + count.getAndIncrement();
			DataObjectFormat dataObjectFormat = new DataObjectFormat();
			dataObjectFormat.setMimeType(file.getMimeType());
			dataObjectFormat.setObjectReference(signatureElementIdReference);
			return dataObjectFormat;
		}).toList();
	}

	private static XMLGregorianCalendar getSigningTime() {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("Could not get signing time", e);
		}
	}
}
