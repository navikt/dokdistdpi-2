package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures;

public interface AsicEVedlegg {
	String getFileName();
	byte[] getBytes();
	String getMimeType();
	String getTittel();
}
