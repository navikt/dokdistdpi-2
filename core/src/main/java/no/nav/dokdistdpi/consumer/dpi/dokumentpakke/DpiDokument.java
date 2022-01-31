package no.nav.dokdistdpi.consumer.dpi.dokumentpakke;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.AsicEVedlegg;

@Data
@Builder
public class DpiDokument implements AsicEVedlegg {

	public static final String MIMETYPE_PDF = "application/pdf";

	private byte[] contents;
	private String filnavn;
	private String mimeType;
	private String tittel;

	public static DpiDokument fromHoveddokument(final String tittel, final String filnavn, final byte[] contents) {
		return DpiDokument.builder()
				.tittel(tittel)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}

	public static DpiDokument fromVedlegg(final String tittel, final String filnavn, final byte[] contents) {
		return DpiDokument.builder()
				.tittel(tittel)
				.filnavn(filnavn)
				.mimeType(MIMETYPE_PDF)
				.contents(contents)
				.build();
	}

	@Override
	public String getFileName() {
		return filnavn;
	}

	@Override
	public byte[] getBytes() {
		return contents;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public String getTittel() {
		return tittel;
	}
}
