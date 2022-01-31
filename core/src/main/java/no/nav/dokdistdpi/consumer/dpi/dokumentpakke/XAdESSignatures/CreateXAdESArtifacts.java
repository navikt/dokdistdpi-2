package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

import lombok.extern.slf4j.Slf4j;
import no.digipost.org.w3.xmldsig.X509IssuerSerialType;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.etsi.uri._01903.v1_3.CertIDType;
import org.etsi.uri._01903.v1_3.DataObjectFormat;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingProperties;
import org.etsi.uri._01903.v1_3.SignedDataObjectProperties;
import org.etsi.uri._01903.v1_3.SignedProperties;
import org.etsi.uri._01903.v1_3.SignedSignatureProperties;
import org.etsi.uri._01903.v1_3.SigningCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.xml.crypto.dsig.DigestMethod.SHA1;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

@Slf4j
@Component
public class CreateXAdESArtifacts {

	private static final no.digipost.org.w3.xmldsig.DigestMethod sha1DigestMethod = new no.digipost.org.w3.xmldsig.DigestMethod(emptyList(), SHA1);
	private final Clock clock;

	@Autowired
	CreateXAdESArtifacts(Clock clock) {
		this.clock = clock;
	}

	XAdESArtifacts createArtifactsToSign(List<AsicEVedlegg> files, AppCertificate appCertificate) {

		try {
			byte[] certificateDigestValue = sha1(appCertificate.getX509Certificate().getEncoded());
			X509Certificate certificate = appCertificate.getX509Certificate();

			DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType(sha1DigestMethod, certificateDigestValue);
			X509IssuerSerialType certificateIssuer = new X509IssuerSerialType(certificate.getIssuerX500Principal().getName(), certificate.getSerialNumber());
			SigningCertificate signingCertificate = new SigningCertificate(singletonList(new CertIDType(certificateDigest, certificateIssuer, null)));

			ZonedDateTime now = ZonedDateTime.now(clock);
			SignedSignatureProperties signedSignatureProperties = new SignedSignatureProperties().withSigningTime(now).withSigningCertificate(signingCertificate);
			SignedDataObjectProperties signedDataObjectProperties = new SignedDataObjectProperties().withDataObjectFormats(dataObjectFormats(files));
			SignedProperties signedProperties = new SignedProperties(signedSignatureProperties, signedDataObjectProperties, "SignedProperties");
			QualifyingProperties qualifyingProperties = new QualifyingProperties().withSignedProperties(signedProperties).withTarget("#Signature");

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
			return new DataObjectFormat(null, null, file.getMimeType(), null, signatureElementIdReference);
		}).toList();
	}
}
