package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.asice;


import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;
import no.nav.dokdistdpi.utils.CertificateUtils;
import no.nav.dokdistdpi.utils.TestUtils.ZipFile;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument.fromHoveddokument;
import static no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument.fromVedlegg;
import static no.nav.dokdistdpi.utils.ForsendelseData.forsendelse;
import static no.nav.dokdistdpi.utils.TestUtils.zipEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AsiceCreatorTest {

	private static final String HOVEDDOKUMENT_NAME = "hoveddokument.pdf";
	private static final String TITTEL = "Ikke-sensitiv tittel for forsendelsen";
	private static final String HOVEDDOKUMENT_CONTENTS = "digitalpost";
	private static final String DOKUMENT_1_NAME = "test1.pdf";
	private static final String DOKUMENT_1_CONTENTS = "test1pdf";
	private static final String DOKUMENT_2_NAME = "test2.pdf";
	private static final String DOKUMENT_2_CONTENTS = "test2pdf";

	AsiceCreator asiceCreator = new AsiceCreator();

	@Test
	void shouldCreateAndSignAsice() throws Exception {
		Dokumentpakke dokumentpakke = Dokumentpakke.builder()
				.hoveddokument(fromHoveddokument(TITTEL, HOVEDDOKUMENT_NAME, new ByteArrayInputStream(HOVEDDOKUMENT_CONTENTS.getBytes())))
				.vedlegg(Arrays.asList(fromVedlegg(TITTEL, DOKUMENT_1_NAME, new ByteArrayInputStream(DOKUMENT_1_CONTENTS.getBytes())),
						fromVedlegg(TITTEL, DOKUMENT_2_NAME, new ByteArrayInputStream(DOKUMENT_2_CONTENTS.getBytes()))))
				.build();
		final OutputStream asiceStreamed = asiceCreator.createAsiceStreamed(forsendelse(dokumentpakke),
				new AppCertificate(CertificateUtils.itestVirksomhetssertifikatProperties()));

		final ByteArrayInputStream asice = new ByteArrayInputStream(((ByteArrayOutputStream) asiceStreamed).toByteArray());

		final List<ZipFile> zipEntries = zipEntries(IOUtils.toBufferedInputStream(asice));
		assertEquals(7, zipEntries.size());

		assertThat(zipEntries).extracting(ZipFile::getName).containsAll(
				Arrays.asList("mimetype",
						"manifest.xml",
						"hoveddokument.pdf",
						"test1.pdf",
						"META-INF/signatures.xml",
						"META-INF/manifest.xml"));
		assertFileContents(zipEntries, HOVEDDOKUMENT_NAME, HOVEDDOKUMENT_CONTENTS);
		assertFileContents(zipEntries, DOKUMENT_1_NAME, DOKUMENT_1_CONTENTS);
		assertFileContents(zipEntries, DOKUMENT_2_NAME, DOKUMENT_2_CONTENTS);
	}

	private void assertFileContents(List<ZipFile> zipEntries, String filename, String expectedFileContents) {
		final ZipFile digitalPostMeldingXml = zipEntries.stream()
				.filter(z -> filename.equals(z.getName()))
				.findFirst()
				.orElseThrow(IllegalStateException::new);
		assertEquals(digitalPostMeldingXml.getContentsAsString(), expectedFileContents);
	}

}
