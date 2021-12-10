package no.nav.dokdistdpi.utils;

import lombok.Data;
import lombok.SneakyThrows;
import no.nav.dokdistdpi.exception.technical.SertifikatException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

	private TestUtils() {
	}

	@SneakyThrows
	public static String classpathToString(String classpathResource) {
		try {

			InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
			String message = IOUtils.toString(inputStream, UTF_8);
			IOUtils.closeQuietly(inputStream);
			return message;
		} catch (IOException e) {
			throw new IOException(format("Kunne ikke åpne classpath-ressurs %s", classpathResource), e);
		}
	}

	public static List<ZipFile> zipEntries(InputStream inputStream) {
		final List<ZipFile> zipEntries = new ArrayList<>();
		try {
			try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					final byte[] contents = IOUtils.toByteArray(zipInputStream);
					zipEntries.add(new ZipFile(zipEntry.getName(), contents));
					zipInputStream.closeEntry();
				}
			}
			return zipEntries;
		} catch (IOException e) {
			return zipEntries;
		}
	}

	@Data
	public static class ZipFile {
		private final String name;
		private final byte[] contents;

		public String getContentsAsString() {
			return new String(contents, UTF_8);
		}
	}

	public static List<String> zipFilenames(InputStream inputStream) {
		return zipEntries(inputStream).stream().map(ZipFile::getName).collect(Collectors.toList());
	}

	public static X509Certificate fraBase64X509String(String base64) {
		try {
			return lagSertifikat(Base64.decodeBase64(base64));
		} catch (CertificateException e) {
			throw new SertifikatException("Kunne ikke lese sertifikat fra base64-streng", e);
		}
	}

	private static X509Certificate lagSertifikat(byte[] certificate) throws CertificateException {
		return  (X509Certificate) CertificateFactory
				.getInstance("X509")
				.generateCertificate(new ByteArrayInputStream(certificate));
	}
}
