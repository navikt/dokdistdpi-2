package no.nav.dokdistdpi.certificate;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
public class AppCertificate {
	private static final String ERR_MISSING_PRIVATE_KEY = "No PrivateKey with alias \"%s\" found in the KeyStore";
	private static final String ERR_MISSING_CERTIFICATE = "No AppCertificate with alias \"%s\" found in the KeyStore";
	private static final String ERROR_GENERAL = "Unable to initiate AppCertificate. Please check that credentials, keystore- and certificate-configuration are correct";

	private final KeyStoreCredentials credentials;
	private final KeyStore keyStore;
	private final PrivateKey privateKey;
	private final X509Certificate certificate;
	private final JWSHeader jwsHeader;
	private final RSASSASigner rsaSigner;
	private final Certificate[] certificateList;

	public AppCertificate(KeyStoreProperties properties, KeyStoreCredentials credentials) {
		this.credentials = credentials;
		try {
			this.keyStore = loadKeyStore(properties, credentials);
			this.privateKey = loadPrivateKey();
			this.certificate = loadX509Certificate();
			List<Base64> certChain = List.of(Base64.encode(certificate.getEncoded()));
			this.jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();
			this.rsaSigner = new RSASSASigner(privateKey);
			this.certificateList = keyStore.getCertificateChain(credentials.alias());
		} catch (Exception e) {
			log.error(ERROR_GENERAL, e);
			throw new IllegalStateException(ERROR_GENERAL, e);
		}
	}

	private static KeyStore loadKeyStore(KeyStoreProperties properties, KeyStoreCredentials credentials) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
		String type = credentials.type();
		String password = credentials.password();
		Resource path = new FileSystemResource(properties.key());

		KeyStore keyStore = KeyStore.getInstance(type);
		if ("none".equalsIgnoreCase(path.getFilename())) {
			keyStore.load(null, password.toCharArray());
		} else {
			if (path.getFilename().endsWith(".b64")) {
				keyStore.load(java.util.Base64.getDecoder().wrap(path.getInputStream()), password.toCharArray());
			} else {
				keyStore.load(path.getInputStream(), password.toCharArray());
			}
		}
		return keyStore;
	}

	private PrivateKey loadPrivateKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		PrivateKey privateKey = (PrivateKey) keyStore.getKey(credentials.alias(), credentials.password().toCharArray());
		if (privateKey == null) {
			throw new IllegalStateException(String.format(ERR_MISSING_PRIVATE_KEY, credentials.alias()));
		}
		return privateKey;
	}

	private X509Certificate loadX509Certificate() throws KeyStoreException {
		X509Certificate certificate = (X509Certificate) keyStore.getCertificate(credentials.alias());
		if (certificate == null) {
			throw new IllegalStateException(String.format(ERR_MISSING_CERTIFICATE, credentials.alias()));
		}
		return certificate;
	}

	public X509Certificate getX509Certificate() {
		return certificate;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public Certificate[] getCertificateList() {
		return certificateList;
	}

	public String generateJWT(JWTClaimsSet claims) {
		SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
		try {
			signedJWT.sign(rsaSigner);
		} catch (JOSEException e) {
			log.error("Error occured during signing of JWT. Continuing anyway.", e);
		}
		return signedJWT.serialize();
	}
}
