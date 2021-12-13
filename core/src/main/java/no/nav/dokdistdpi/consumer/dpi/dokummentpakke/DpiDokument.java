package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.io.InputStream;

@Data
@Builder
public class DpiDokument {

	public static final String MIMETYPE_PDF = "application/pdf";

	private InputStream contents;
	private String filnavn;
	private String mimeType;
	private String tittle;

	public static DpiDokument fromHoveddokument(final String tittle, final String filnavn, final InputStream contents) {
		return DpiDokument.builder()
				.tittle(tittle)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}

	public static DpiDokument fromVedlegg(final String tittle, final String filnavn, final InputStream contents) {
		return DpiDokument.builder()
				.tittle(tittle)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}
}
