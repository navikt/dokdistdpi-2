package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.certificate.KeyStoreCredentials;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class CertificateUtils {

	public static final String SELF_SIGNED_PEM = "secrets/cert.pem";
	public static final String SELF_SIGNED_RSA_PRIVATE_KEY = "secrets/key.key";
	public static final String SELF_SIGNED_PKCS12 = "../core/src/test/resources/secrets/cert.p12";
	public static final String SELF_SIGNED_PKCS12_BASE64 = "../core/src/test/resources/secrets/cert.p12.b64";
	public static final String PKCS_12 = "PKCS12";
	public static final String SELF_SIGNED_PKCS12_ALIAS = "1";
	public static final String SELF_SIGNED_PKCS12_PASSWORD = "dokdistdpi";

	private CertificateUtils() {
	}

	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096);
		return keyPairGenerator.generateKeyPair();
	}

	public static Certificate generateCertificate(PublicKey subjectPublicKey, PrivateKey issuerPrivateKey) throws ParseException, OperatorCreationException,
			CertificateException, IOException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		X500Name issuer = new X500Name("CN=Issuer and subject (self signed)");
		BigInteger serial = new BigInteger("100");
		Date notBefore = df.parse("2010-01-01");
		Date notAfter = df.parse("2050-01-01");
		X500Name subject = issuer;
		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));

		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerPrivateKey);

		X509CertificateHolder holder = certBuilder.build(signer);

		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		return factory.generateCertificate(new ByteArrayInputStream(holder.getEncoded()));
	}

	public static AppCertificate itestVirksomhetssertifikatAppCertificate() {
		KeyStoreProperties keyStoreProperties = new KeyStoreProperties(SELF_SIGNED_PKCS12, "credentials");
		KeyStoreCredentials credentials= new KeyStoreCredentials(SELF_SIGNED_PKCS12_ALIAS, SELF_SIGNED_PKCS12_PASSWORD, PKCS_12);
		return new AppCertificate(keyStoreProperties, credentials);
	}

	public static AppCertificate itestVirksomhetssertifikatBase64AppCertificate() {
		KeyStoreProperties keyStoreProperties = new KeyStoreProperties(SELF_SIGNED_PKCS12_BASE64, "credentials");
		KeyStoreCredentials credentials= new KeyStoreCredentials(SELF_SIGNED_PKCS12_ALIAS, SELF_SIGNED_PKCS12_PASSWORD, PKCS_12);
		return new AppCertificate(keyStoreProperties, credentials);
	}

	public static PrivateKey itestPrivateKey() throws Exception {
		return getPrivateKey(SELF_SIGNED_RSA_PRIVATE_KEY);
	}

	public static X509Certificate itestPemCertificate() throws Exception {
		return getPemCert(SELF_SIGNED_PEM);
	}

	public static PEMParser openPEMResource(String fileName) {
		InputStream res = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
		Reader fRd = new BufferedReader(new InputStreamReader(Objects.requireNonNull(res)));
		return new PEMParser(fRd);
	}

	public static PrivateKey getPrivateKey(final String path) throws Exception {
		PEMParser pemRd = openPEMResource(path);
		Object o = pemRd.readObject();

		if (o instanceof PrivateKeyInfo) {
			return new JcaPEMKeyConverter().setProvider("BC")
					.getPrivateKey((PrivateKeyInfo) o);
		} else {
			throw new IllegalStateException("Kunne ikke lese privatekey fra angitt path");
		}
	}

	public static X509Certificate getPemCert(final String path) throws Exception {
		PEMParser pemRd = openPEMResource(path);
		Object o = pemRd.readObject();

		if (o instanceof X509CertificateHolder) {
			return new JcaX509CertificateConverter().setProvider("BC")
					.getCertificate((X509CertificateHolder) o);
		} else {
			throw new IllegalStateException("Kunne ikke lese pem sertifikat fra angitt path");
		}
	}
}
