package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest;

import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.AsicEVedlegg;

public class DpiManifest implements AsicEVedlegg {

	private final byte[] contents;

	public DpiManifest(byte[] contents) {
		this.contents = contents;
	}

	@Override
	public String getFileName() {
		return "manifest.xml";
	}

	@Override
	public byte[] getBytes() {
		return contents;
	}

	@Override
	public String getMimeType() {
		return "application/xml";
	}

	@Override
	public String getTittel() {
		return null;
	}
}
