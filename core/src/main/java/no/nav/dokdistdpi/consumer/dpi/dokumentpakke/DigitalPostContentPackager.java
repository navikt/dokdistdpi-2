package no.nav.dokdistdpi.consumer.dpi.dokumentpakke;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.asice.AsiceCreator;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.asice.CreateCMSDocument;
import no.nav.dokdistdpi.exception.technical.DokumentpakkingException;
import no.nav.dokdistdpi.exception.technical.SertifikatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.security.cert.CertificateFactory.getInstance;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

@Slf4j
@Component
public class DigitalPostContentPackager {

	private final AsiceCreator asiceCreator;
	private final CreateCMSDocument createCMSDocument;

	@Autowired
	public DigitalPostContentPackager(AsiceCreator asiceCreator, CreateCMSDocument createCMSDocument) {
		this.asiceCreator = asiceCreator;
		this.createCMSDocument = createCMSDocument;
	}

	public InputStream createDokumentpakke(Forsendelse forsendelse, AppCertificate appCertificate) {
		X509Certificate mottakerCertificate = fraBase64X509String(forsendelse.getMottakerSertifikat());
		try (final OutputStream asiceStreamed = asiceCreator.createAsiceStreamed(forsendelse, appCertificate)) {
			log.info("Opretter CMS dokument");
			byte[] cmsByte = createCMSDocument.createCMSByte(((ByteArrayOutputStream) asiceStreamed).toByteArray(), mottakerCertificate);
			return new ByteArrayInputStream(cmsByte);
		} catch (IOException e) {
			throw new DokumentpakkingException("Klarte ikke lage asic eller kryptere dokumentpakke.", e);
		}
	}

	private X509Certificate fraBase64X509String(String base64) {
		try {
			return lagSertifikat(decodeBase64(base64));
		} catch (CertificateException var2) {
			throw new SertifikatException("Kunne ikke lese sertifikat fra base64-streng", var2);
		}
	}

	private X509Certificate lagSertifikat(byte[] certificate) throws CertificateException {
		return (X509Certificate) getInstance("X509").generateCertificate(new ByteArrayInputStream(certificate));
	}
}
