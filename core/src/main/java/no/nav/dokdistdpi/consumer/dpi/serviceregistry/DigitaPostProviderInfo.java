package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import lombok.Getter;
import lombok.ToString;
import no.nav.dokdistdpi.exception.functional.DigitaPostProviderInfoIkkeFunnetException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.isBlank;

@Getter
@ToString(exclude = {"pemCertificate", "x509Certificate"})
public class DigitaPostProviderInfo {

	private final String orgnummer;
	private final String pemCertificate;
	private X509Certificate x509Certificate;
	private final String serviceCode;
	private final String serviceEditionCode;

	public DigitaPostProviderInfo(String orgnummer, String pemCertificate, String serviceCode, String serviceEditionCode) {
		this.orgnummer = orgnummer;
		this.pemCertificate = pemCertificate;
		this.x509Certificate = convertToX509(pemCertificate);
		this.serviceCode = serviceCode;
		this.serviceEditionCode = serviceEditionCode;
	}

	private X509Certificate convertToX509(String pemCertificate) {
		if (isBlank(pemCertificate)) {
			throw new DigitaPostProviderInfoIkkeFunnetException("Fant ikke PEM sertifikat");
		}
		PEMParser pemParser = openPemParser(pemCertificate);
		try {
			final Object certificate = pemParser.readObject();
			if (!(certificate instanceof X509CertificateHolder)) {
				throw new DigitaPostProviderInfoIkkeFunnetException("PEM data inneholder ikke et X.509 sertifikat.");
			}
			return new JcaX509CertificateConverter().setProvider("BC").getCertificate((X509CertificateHolder) certificate);
		} catch (CertificateException e) {
			throw new DigitaPostProviderInfoIkkeFunnetException("Klarte ikke konvertere PEM data til X.509 sertifikat.", e);
		} catch (IOException e) {
			throw new DigitaPostProviderInfoIkkeFunnetException("Klarte ikke lese PEM data.", e);
		}
	}

	private PEMParser openPemParser(final String pemCertificate) {
		Reader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(pemCertificate.getBytes())));
		return new PEMParser(bufferedReader);
	}
}
