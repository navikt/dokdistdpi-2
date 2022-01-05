package no.nav.dokdistdpi.qdist011;

import com.amazonaws.SdkClientException;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.DokumenttypeInfoTo;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfoTo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Identifikator;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusExpedertKanIkkeDistribuereException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDeserialisereS3PayloadException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeFinneDokumentException;
import no.nav.dokdistdpi.qdist011.saf.JournalpostQdist011;
import no.nav.dokdistdpi.s3storage.DokDistDokumentFraS3;
import no.nav.dokdistdpi.s3storage.JsonSerializer;
import no.nav.dokdistdpi.s3storage.Storage;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_EKSPEDERT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.VEDLEGG;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.VEDLEGG_TITTEL_PREFIX;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotNull;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.isBlank;

/**
 * @author Tsigab A. Gebremedhin, NAV
 */
@Slf4j
@Component
public class Qdist011Service {

	private final Storage s3Storage;
	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService;
	private final DigitalPostService digitalPostService;
	private static final String SPRAAK = "NO";

	@Autowired
	public Qdist011Service(Storage s3Storage, AdministrerForsendelseConsumer administrerForsendelse, DigitalPostService digitalPostService,
						   @Qualifier("SafJournalpostQueryServiceQdist011") SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService) {
		this.s3Storage = s3Storage;
		this.administrerForsendelse = administrerForsendelse;
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.digitalPostService = digitalPostService;
	}

	@Handler
	public Forsendelse createForsendelse(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		validateDistribuerForsendelseTilDpi(distribuerTilKanal);
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(distribuerTilKanal.getForsendelseId());
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());
		if (FORSENDELSE_STATUS_EKSPEDERT.equals(hentForsendelseResponse.getForsendelseStatus())) {
			log.info("Forsendelse med forsendelseId={}, status={} er ekspdert og behandlingen avsluttes",
					distribuerTilKanal.getForsendelseId(), hentForsendelseResponse.getForsendelseStatus());
			throw new ForsendelseStatusExpedertKanIkkeDistribuereException(format("Forsendelse med forsendelseId=%s, status=%s er ekspdert og behandlingen avsluttes",
					distribuerTilKanal.getForsendelseId(), hentForsendelseResponse.getForsendelseStatus()));
		}
		String konversasjonId = getConversationId(hentForsendelseResponse, distribuerTilKanal.getForsendelseId());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());
		exchange.setProperty(PROPERTY_CONVERSATION_ID, konversasjonId);
		String maskinportenToken = digitalPostService.getMaskinportenToken();

		DokumenttypeInfoTo dokumenttypeInfo = digitalPostService.getDokumenttypeInfo(hentForsendelseResponse);

		VarselInfoTo varselInfoTo = digitalPostService.getVarselInfo(dokumenttypeInfo);

		assertForsendelseNotNull(hentForsendelseResponse);
		SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = digitalPostService.hentDigitalKontaktInfo(hentForsendelseResponse, varselInfoTo);

		Varsler varsler = digitalPostService.mapVarsler(varselInfoTo, sikkerDigitalKontaktInfo);
		return Forsendelse.builder()
				.personidentifikator(sikkerDigitalKontaktInfo.getPersonidentifikator())
				.mottakerSertifikat(sikkerDigitalKontaktInfo.getLeverandoerSertifikat())
				.digitalPostLeverandoerAdresse(sikkerDigitalKontaktInfo.getLeverandoerAdresse())
				.bestillingsId(hentForsendelseResponse.getBestillingsId())
				.konversasjonId(konversasjonId)
				.digital(DigitalPost.builder()
						.avsender(DigitalPost.Avsender.builder()
								.virksomhetsidentifikator(Identifikator.builder()
										.authority(ISO_6523_ACTORID_UPIS)
										.value(asIso6523(NAV_ORGNUMMER))
										.build())
								.avsenderindentifikator(NAV_ORGNUMMER)
								.build())
						.mottaker(DigitalPost.Personmottaker.builder()
								.postkasseadresse(sikkerDigitalKontaktInfo.getBrukerAdresse())
								.build())
						.maskinportentoken(maskinportenToken)
						.sikkerhetsnivaa(dokumenttypeInfo.getSikkerhetsnivaa())
						.virkningsdato(LocalDate.now())
						.aapningskvittering(false)
						.ikkesensitivtittel(hentForsendelseResponse.getForsendelseTittel())
						.spraak(SPRAAK)
						.varsler(varsler)
						.build())
				.dokumentpakke(getDocumentpakkeFromS3(hentForsendelseResponse))
				.build();
	}


	private JournalpostQdist011 getJournalpostQdist011(HentForsendelseResponse HentForsendelseResponse) {
		if (HentForsendelseResponse.isIkkeArkivertIJoark()) {
			return null;
		}
		return safJournalpostQueryService.hentJournalpost(HentForsendelseResponse.getArkivInformasjon().getArkivId());
	}

	Dokumentpakke getDocumentpakkeFromS3(HentForsendelseResponse hentForsendelseResponse) {
		if (hentForsendelseResponse.getDokumenter().isEmpty()) {
			throw new KunneIkkeFinneDokumentException(
					format("Finnes ikke dokumenter med bestillingsId=%s", hentForsendelseResponse.getBestillingsId())
			);
		}

		JournalpostQdist011 journalpostQdist011 = getJournalpostQdist011(hentForsendelseResponse);
		DpiDokument hovedDokument = hentForsendelseResponse.getDokumenter()
				.stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.map(dokument ->
						DpiDokument.fromHoveddokument(hentForsendelseResponse.getForsendelseTittel(),
								getHoveddokumentFilnavn(hentForsendelseResponse),
								new ByteArrayInputStream(this.getDocumentForS3(dokument).getPdf())
						))
				.findFirst().orElseThrow(() -> new KunneIkkeFinneDokumentException("Kunne ikke finne hovedDokument"));

		AtomicInteger vedleggIdx = new AtomicInteger(1);
		List<DpiDokument> vedleggList = hentForsendelseResponse.getDokumenter()
				.stream()
				.filter(dokument -> VEDLEGG.equals(dokument.getTilknyttetSom()))
				.map(dokument -> {
					DokDistDokumentFraS3 dokDistDokumentFraS3 = this.getDocumentForS3(dokument);

					return DpiDokument.fromVedlegg(getVedleggTittel(
									journalpostQdist011,
									dokument,
									vedleggIdx.getAndIncrement()
							),
							dokDistDokumentFraS3.getDokumentObjektReferanse(),
							new ByteArrayInputStream(dokDistDokumentFraS3.getPdf())
					);
				})
				.toList();

		return Dokumentpakke.builder()
				.hoveddokument(hovedDokument)
				.vedlegg(vedleggList)
				.build();
	}

	private DokDistDokumentFraS3 getDocumentForS3(HentForsendelseResponse.DokumentTo dokument) {
		String jsonPayload = s3Storage.get(dokument.getDokumentObjektReferanse());
		DokDistDokumentFraS3 dokDistDokumentFraS3 = deserializeS3JsonPayloadToDokdistDokument(jsonPayload, dokument.getDokumentObjektReferanse());
		dokDistDokumentFraS3.setDokumentInfoId(dokument.getArkivDokumentInfoId());

		return dokDistDokumentFraS3;
	}

	private String getHoveddokumentFilnavn(HentForsendelseResponse hentForsendelseResponseTo) {
		return hentForsendelseResponseTo.getDokumenter().stream()
				.filter(dokumentTo -> HOVEDDOKUMENT.equals(dokumentTo.getTilknyttetSom()))
				.findAny()
				.map(HentForsendelseResponse.DokumentTo::getDokumentObjektReferanse)
				.orElseThrow(() -> new KunneIkkeFinneDokumentException(
						format("Kunne ikke finne hoveddokument for bestilling med bestillingsId=%s",
								hentForsendelseResponseTo.getBestillingsId())))
				.concat(".pdf");
	}

	private String getVedleggTittel(JournalpostQdist011 journalpostQdist011,
									HentForsendelseResponse.DokumentTo dokumentTo, int vedleggIdx) {
		if (journalpostQdist011 == null) {
			return VEDLEGG_TITTEL_PREFIX + vedleggIdx;
		}

		String arkivDokumentInfoId = dokumentTo.getArkivDokumentInfoId();
		return journalpostQdist011.getDokumenter().stream()
				.filter(dokumentInfo -> dokumentInfo.getDokumentInfoId().equals(arkivDokumentInfoId))
				.findAny()
				.orElseThrow(() -> new KunneIkkeFinneDokumentException(
						format("DokumentInfoId=%s ikke funnet i journalpost", arkivDokumentInfoId)))
				.getTittel();
	}

	private void validateDistribuerForsendelseTilDpi(DistribuerTilKanal distribuerTilKanal) {
		assertNotNull("DistribuerTilKanal", distribuerTilKanal);
		assertNotBlank("forsendelseId", distribuerTilKanal.getForsendelseId());
	}

	private void assertForsendelseNotNull(HentForsendelseResponse hentForsendelseResponse) {
		assertNotNull("HentForsendelseResponseTo", hentForsendelseResponse);
		assertNotNull("HentForsendelseResponseTo.MottakerTo", hentForsendelseResponse.getMottaker());
	}

	public static DokDistDokumentFraS3 deserializeS3JsonPayloadToDokdistDokument(String jsonPayload, String objektReferanse) {
		DokDistDokumentFraS3 dokDistDokumentFraS3;
		try {
			dokDistDokumentFraS3 = JsonSerializer.deserialize(jsonPayload, DokDistDokumentFraS3.class);
			dokDistDokumentFraS3.setDokumentObjektReferanse(objektReferanse);
		} catch (SdkClientException e) {
			throw new KunneIkkeDeserialisereS3PayloadException(format("Kunne ikke deserialisere jsonPayload fra s3 bucket for dokument med dokumentobjektreferanse=%s. Dokumentet er ikke persistert til s3 med korrekt format!", objektReferanse));
		}
		return dokDistDokumentFraS3;
	}

	private String getConversationId(HentForsendelseResponse hentForsendelse, String forsendelseId) {
		return isBlank(hentForsendelse.getKonversasjonId()) ? generateKonversasjonsId(forsendelseId) : hentForsendelse.getKonversasjonId();
	}

	private String generateKonversasjonsId(String forsendelseId) {
		String konversasjonsId = UUID.randomUUID().toString();
		administrerForsendelse.oppdaterKonversasjonsId(forsendelseId, konversasjonsId);
		log.info("Oppdatert forsendelse med forsendelseId={} til konversasjonsId={}", forsendelseId, konversasjonsId);
		return konversasjonsId;
	}
}
