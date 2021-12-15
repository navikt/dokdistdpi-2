package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.asice;

import org.bouncycastle.jcajce.provider.digest.SHA256;

import javax.activation.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DokumentSignature implements DataSource {
	public static final String CONTENT_TYPE_DOKUMENTPAKKE = "application/cms";
	private final InputStream asicStream;

	public DokumentSignature(InputStream asicStream) {
		this.asicStream = asicStream;
	}


	@Override
	public InputStream getInputStream() {
		return asicStream;
	}

	@Override
	public OutputStream getOutputStream() {
		throw new UnsupportedOperationException("Not supported by handler");
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE_DOKUMENTPAKKE;
	}

	@Override
	public String getName() {
		return "asic.cms";
	}


	public String getDokumentpakkefingeravtrykk() throws IOException {
		MessageDigest digest = new SHA256.Digest();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream asicToRead = this.asicStream;

		try(DigestOutputStream digestStream = new DigestOutputStream(baos, digest)) {
			copy(asicToRead, digestStream);
		} finally  {
			if (asicToRead != null) {
					asicToRead.close();

			}
		}
		return new String(Base64.getDecoder().decode(digest.digest()), UTF_8);
	}

	private static void copy(InputStream source, OutputStream sink) throws IOException {
		byte[] buf = new byte[8192];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
		}

	}

}
