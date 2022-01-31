package no.nav.dokdistdpi.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.AsicEVedlegg;
import no.nav.dokdistdpi.exception.functional.RuntimeIOException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class CreateZip {

	private CreateZip() {
	}

	public static Archive zipEntries(List<AsicEVedlegg> files) {
		try (ByteArrayOutputStream archive = new ByteArrayOutputStream(); ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(archive)) {
			zipOutputStream.setEncoding(StandardCharsets.UTF_8.name());
			zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
			for (AsicEVedlegg file : files) {
				log.trace("Adding " + file.getFileName() + " to archive. Size in bytes before compression: " + file.getBytes().length);
				ZipArchiveEntry zipEntry = new ZipArchiveEntry(file.getFileName());
				zipEntry.setSize(file.getBytes().length);

				zipOutputStream.putArchiveEntry(zipEntry);
				IOUtils.write(file.getBytes(), zipOutputStream);
				zipOutputStream.closeArchiveEntry();
			}
			zipOutputStream.finish();
			zipOutputStream.close();

			return new Archive(archive.toByteArray());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}


	@Data
	public static class Archive {
		private final byte[] bytes;

		public byte[] getBytes() {
			return bytes;
		}
	}
}
