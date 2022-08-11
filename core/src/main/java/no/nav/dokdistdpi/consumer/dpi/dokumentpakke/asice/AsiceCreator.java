package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.asice;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.AsicEVedlegg;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.CreateSignature;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.XAdESSignatures;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.XAdESSignatures.XmlValideringException;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest.DpiManifest;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.xmlmanifest.XmlManifestCreator;
import no.nav.dokdistdpi.exception.technical.XMLXAdESSignaturesException;
import no.nav.dokdistdpi.utils.CreateZip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.CreateZip.zipEntries;


@Slf4j
@Component
public class AsiceCreator {

	private final XmlManifestCreator xmlManifestCreator;
	private final CreateSignature createSignature;

	@Autowired
	public AsiceCreator(CreateSignature createSignature) {
		this.xmlManifestCreator = new XmlManifestCreator();
		this.createSignature = createSignature;
	}


	public OutputStream createAsiceStreamed(Forsendelse forsendelse, AppCertificate appCertificate) throws IOException {
		DpiDokument hoveddokument = forsendelse.getDokumentpakke().getHoveddokument();
		List<DpiDokument> vedlegg = forsendelse.getDokumentpakke().getVedlegg();

		ByteArrayOutputStream asiceArchive = new ByteArrayOutputStream();
		List<AsicEVedlegg> asicEAttachables = new ArrayList<>();

		log.info("Oppretter ASiC-E manifest. bestillingsId={}. antall={} dokumenter (hoveddokument + vedlegg)",
				forsendelse.getBestillingsId(), 1 + vedlegg.size());
		DpiManifest xmlManifest = xmlManifestCreator.createManifest(forsendelse);

		asicEAttachables.add(xmlManifest);
		asicEAttachables.add(hoveddokument);
		asicEAttachables.addAll(vedlegg);

		try {
			// Lag signatur over alle filene i pakka
			log.info("Signerer ASiC-E dokumenter med bestillingsId={} ved bruk av private key.", forsendelse.getBestillingsId());
			XAdESSignatures signatures = createSignature.createSignature(appCertificate, asicEAttachables);
			asicEAttachables.add(signatures);
		} catch (XmlValideringException e) {
			log.error(format("Klarte ikke å signere ASiC-E element, bestillingsId=%s.", forsendelse.getBestillingsId()));
			throw new XMLXAdESSignaturesException("Klarte ikke å signere ASiC-E element.", e);
		}

		// Zip filene
		log.trace("Zipping ASiC-E files. Contains a total of " + asicEAttachables.size() + " files (including the generated manifest and signatures)");
		CreateZip.Archive archive = zipEntries(asicEAttachables);
		asiceArchive.writeBytes(archive.getBytes());

		return asiceArchive;
	}
}
