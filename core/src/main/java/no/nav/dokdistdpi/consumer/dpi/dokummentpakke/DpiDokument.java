package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class DpiDokument {

	public static final String MIMETYPE_PDF = "application/pdf";

	private InputStream contents;
	private String filnavn;
	private String mimeType;
	private String tittel;

	public static DpiDokument fromHoveddokument(final String tittel, final String filnavn, final InputStream contents) {
		return DpiDokument.builder()
				.tittel(tittel)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}

	public static DpiDokument fromVedlegg(final String tittel, final String filnavn, final InputStream contents) {
		return DpiDokument.builder()
				.tittel(tittel)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}
}
