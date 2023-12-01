package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

public class XAdESSignatures implements AsicEVedlegg {
	private final byte[] xmlBytes;

	public XAdESSignatures(byte[] xmlBytes) {
		this.xmlBytes = xmlBytes;
	}

	@Override
	public String getFileName() {
		return "META-INF/signatures.xml";
	}

	@Override
	public byte[] getBytes() {
		return xmlBytes;
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
