package no.nav.dokdistdpi.consumer.dpi;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;

import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GenerateJwt {

	private GenerateJwt() {
	}

	public static String generateJWT(JWTClaimsSet claims, AppCertificate appCertificate) {
		List<Base64> certChain = new ArrayList<>();
		try {
			certChain.add(Base64.encode(appCertificate.getX509Certificate().getEncoded()));
		} catch (CertificateEncodingException e) {
			log.error("Could not get encoded certificate", e);
			throw new SikkerDigitalPostException("Could not get encoded certificate", e);
		}
		JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();

		RSASSASigner signer = new RSASSASigner(appCertificate.loadPrivateKey());

		if (appCertificate.shouldLockProvider()) {
			signer.getJCAContext().setProvider(appCertificate.getKeyStore().getProvider());
		}

		SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			log.error("Error occured during signing of JWT", e);
		}
		return signedJWT.serialize();
	}
}
