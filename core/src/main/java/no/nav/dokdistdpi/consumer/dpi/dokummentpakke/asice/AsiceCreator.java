package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.asice;

import lombok.extern.slf4j.Slf4j;
import no.difi.asic.AsicWriter;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.SignatureHelper;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.DpiDokument;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.xmlmanifest.XmlManifestCreator;
import no.nav.dokdistdpi.exception.technical.DokumentpakkingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static no.difi.asic.MimeType.XML;
import static no.difi.asic.MimeType.forString;
import static no.difi.asic.SignatureMethod.XAdES;
import static no.nav.dokdistdpi.consumer.dpi.dokummentpakke.DpiDokument.MIMETYPE_PDF;

@Slf4j
@Component
public class AsiceCreator {

	private static final String MANIFEST_NAVN = "manifest.xml";
	private final XmlManifestCreator xmlManifestCreator;

	@Autowired
	public AsiceCreator() {
		this.xmlManifestCreator = new XmlManifestCreator();
	}

	OutputStream createAsiceStreamed(Forsendelse forsendelse, AppCertificate appCertificate) throws IOException {
		DpiDokument hoveddokument = forsendelse.getDokumentpakke().getHoveddokument();
		List<DpiDokument> vedlegg = forsendelse.getDokumentpakke().getVedlegg();
		ByteArrayOutputStream asiceArchive = new ByteArrayOutputStream();

		log.info("Creating ASiC-E manifest");
		String xmlManifest = xmlManifestCreator.createManifest(forsendelse);

		AsicWriter asicWriter = AsicWriterFactory.newFactory(XAdES).newContainer(asiceArchive)
				.add(new BufferedInputStream(new ByteArrayInputStream(xmlManifest.getBytes())), MANIFEST_NAVN, XML);
		List<InputStream> streamsToClose = new ArrayList<>();

		try (InputStream forsendelseMeldingInputStream = new BufferedInputStream(hoveddokument.getContents())) {
			// Skriv hoveddokument til Asice
			streamsToClose.add(forsendelseMeldingInputStream);
			asicWriter.add(forsendelseMeldingInputStream, hoveddokument.getFilnavn(), forString(MIMETYPE_PDF));

			// Skriv resten av dokumenter til Asice
			vedlegg.forEach(dokument -> {
				if (log.isDebugEnabled()) {
					log.debug("Adding file {} of type {}", dokument.getFilnavn(), dokument.getMimeType());
				}
				try {
					InputStream inputStream = new BufferedInputStream(dokument.getContents());
					streamsToClose.add(inputStream);
					asicWriter.add(inputStream, dokument.getFilnavn(), forString(MIMETYPE_PDF));
				} catch (IOException e) {
					throw new DokumentpakkingException("Kunne ikke pakke asice", e);
				}
			});
			asicWriter.sign(new DefaultSignatureHelper(appCertificate));
			return asiceArchive;
		} finally {
			for (InputStream is : streamsToClose) {
				is.close();
			}
		}
	}

	private static class DefaultSignatureHelper extends SignatureHelper {
		DefaultSignatureHelper(AppCertificate appCertificate) {
			super(appCertificate.shouldLockProvider() ? appCertificate.getKeyStore().getProvider() : null);
			loadCertificate(appCertificate.getKeyStore(), appCertificate.getProperties().getAlias(), appCertificate.getProperties().getPassword());
		}
	}
}
