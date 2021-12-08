package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import lombok.Builder;
import lombok.Value;

import java.io.InputStream;

@Builder
@Value
public class DigitalPostDokument {

	public static final String MIMETYPE_PDF = "application/pdf";
	public static final String DOT_PDF = ".pdf";

	private InputStream contents;
	private String filnavn;
	private String mimeType;
	private String title;

	public static DigitalPostDokument fromHoveddokument(final String tittle, final String filnavn, final InputStream contents) {
		return DigitalPostDokument.builder()
				.title(tittle)
				.filnavn(filnavn + DOT_PDF)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}

	public static DigitalPostDokument fromVedlegg(final String tittle, final String filnavn, final InputStream contents) {
		return DigitalPostDokument.builder()
				.title(tittle)
				.filnavn(filnavn + DOT_PDF)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}
}
