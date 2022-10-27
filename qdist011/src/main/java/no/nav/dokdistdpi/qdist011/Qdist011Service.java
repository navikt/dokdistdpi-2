package no.nav.dokdistdpi.qdist011;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.cloudstorage.DokDistDokumentFraBucket;
import no.nav.dokdistdpi.cloudstorage.EncryptedBucketStorage;
import no.nav.dokdistdpi.cloudstorage.JsonSerializer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.DokumenttypeInfoTo;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfoTo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Avsender;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Identifikator;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DpiDokument;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;
import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusExpedertKanIkkeDistribuereException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDeserialisereBucketPayloadException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDistribuereForsendelseException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeFinneDokumentException;
import no.nav.dokdistdpi.exception.functional.UtenforKjernetidFunctionalException;
import no.nav.dokdistdpi.qdist011.saf.JournalpostQdist011;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_EKSPEDERT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_OPPRETTET;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.VEDLEGG;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.VEDLEGG_TITTEL_PREFIX;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Tsigab A. Gebremedhin, NAV
 */
@Slf4j
@Component
public class Qdist011Service {
	private static final String SPRAAK = "NO";

	private final EncryptedBucketStorage encryptedBucketStorage;
	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService;
	private final DigitalPostService digitalPostService;
	private final LocalTime kjernetidStart;
	private final LocalTime kjernetidSlutt;
	private final Clock clock;

	@Autowired
	public Qdist011Service(EncryptedBucketStorage encryptedBucketStorage,
						   AdministrerForsendelseConsumer administrerForsendelse,
						   DigitalPostService digitalPostService,
						   @Qualifier("SafJournalpostQueryServiceQdist011") SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService,
						   @Value("${kjernetidStart}") String kjernetidStart,
						   @Value("${kjernetidSlutt}") String kjernetidSlutt) {
		this.encryptedBucketStorage = encryptedBucketStorage;
		this.administrerForsendelse = administrerForsendelse;
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.digitalPostService = digitalPostService;
		this.kjernetidStart = LocalTime.parse(kjernetidStart);
		this.kjernetidSlutt = LocalTime.parse(kjernetidSlutt);
		this.clock = Clock.systemDefaultZone();
	}

	@Handler
	public Forsendelse createForsendelse(DistribuerTilKanal distribuerTilKanal, Exchange exchange) {
		validateDistribuerForsendelseTilDpi(distribuerTilKanal);
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelse.hentForsendelse(distribuerTilKanal.getForsendelseId());
		assertForsendelseNotNull(hentForsendelseResponse);
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());

		validateStatus(hentForsendelseResponse.getForsendelseStatus(), distribuerTilKanal.getForsendelseId());
		validateKjernetid(hentForsendelseResponse.getDistribusjonstidspunkt(), hentForsendelseResponse.getBestillingsId());

		String konversasjonId = getConversationId(hentForsendelseResponse, distribuerTilKanal.getForsendelseId());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());
		exchange.setProperty(PROPERTY_CONVERSATION_ID, konversasjonId);
		String maskinportenToken = digitalPostService.getMaskinportenToken();

		DokumenttypeInfoTo dokumenttypeInfo = digitalPostService.getDokumenttypeInfo(hentForsendelseResponse);

		VarselInfoTo varselInfoTo = digitalPostService.getVarselInfo(dokumenttypeInfo);

		SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = digitalPostService.hentDigitalKontaktInfo(hentForsendelseResponse, varselInfoTo);

		Varsler varsler = digitalPostService.mapVarsler(varselInfoTo, sikkerDigitalKontaktInfo, hentForsendelseResponse.getDistribusjonstype());
		return Forsendelse.builder()
				.forsendelseId(distribuerTilKanal.getForsendelseId())
				.personidentifikator(sikkerDigitalKontaktInfo.getPersonidentifikator())
				.mottakerSertifikat(sikkerDigitalKontaktInfo.getLeverandoerSertifikat())
				.digitalPostLeverandoerAdresse(sikkerDigitalKontaktInfo.getLeverandoerAdresse())
				.bestillingsId(hentForsendelseResponse.getBestillingsId())
				.konversasjonId(konversasjonId)
				.digital(DigitalPost.builder()
						.avsender(Avsender.builder()
								.virksomhetsidentifikator(Identifikator.builder()
										.authority(ISO_6523_ACTORID_UPIS.getValue())
										.value(asIso6523(NAV_ORGNUMMER))
										.build())
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
				.dokumentpakke(getDocumentpakkeFromBucket(hentForsendelseResponse))
				.build();
	}


	private JournalpostQdist011 getJournalpostQdist011(HentForsendelseResponse HentForsendelseResponse) {
		if (HentForsendelseResponse.isIkkeArkivertIJoark()) {
			return null;
		}
		return safJournalpostQueryService.hentJournalpost(HentForsendelseResponse.getArkivInformasjon().getArkivId());
	}

	Dokumentpakke getDocumentpakkeFromBucket(HentForsendelseResponse hentForsendelseResponse) {
		final var bestillingsId = hentForsendelseResponse.getBestillingsId();
		if (hentForsendelseResponse.getDokumenter().isEmpty()) {
			throw new KunneIkkeFinneDokumentException(
					format("Finnes ikke dokumenter med bestillingsId=%s", bestillingsId)
			);
		}

		JournalpostQdist011 journalpostQdist011 = getJournalpostQdist011(hentForsendelseResponse);
		DpiDokument hovedDokument = hentForsendelseResponse.getDokumenter()
				.stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.map(dokument ->
						DpiDokument.fromHoveddokument(hentForsendelseResponse.getForsendelseTittel(),
								getHoveddokumentFilnavn(hentForsendelseResponse), this.getDocumentFromBucket(dokument, bestillingsId).getPdf()
						))
				.findFirst().orElseThrow(() -> new KunneIkkeFinneDokumentException("Kunne ikke finne hovedDokument"));

		AtomicInteger vedleggIdx = new AtomicInteger(1);
		List<DpiDokument> vedleggList = hentForsendelseResponse.getDokumenter()
				.stream()
				.filter(dokument -> VEDLEGG.equals(dokument.getTilknyttetSom()))
				.map(dokument -> {
					DokDistDokumentFraBucket dokDistDokumentFraBucket = this.getDocumentFromBucket(dokument, bestillingsId);

					return DpiDokument.fromVedlegg(getVedleggTittel(
									journalpostQdist011,
									dokument,
									vedleggIdx.getAndIncrement()
							),
							dokDistDokumentFraBucket.getDokumentObjektReferanse(), dokDistDokumentFraBucket.getPdf()
					);
				})
				.toList();

		return Dokumentpakke.builder()
				.hoveddokument(hovedDokument)
				.vedlegg(vedleggList)
				.build();
	}

	private DokDistDokumentFraBucket getDocumentFromBucket(HentForsendelseResponse.DokumentTo dokument, String bestillingsId) {
		String jsonPayload = encryptedBucketStorage.downloadObject(dokument.getDokumentObjektReferanse(), bestillingsId);
		DokDistDokumentFraBucket dokDistDokumentFraBucket = deserializeBucketJsonPayloadToDokdistDokument(jsonPayload, dokument.getDokumentObjektReferanse());
		dokDistDokumentFraBucket.setDokumentInfoId(dokument.getArkivDokumentInfoId());

		return dokDistDokumentFraBucket;
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

	private void validateStatus(String forsendelseStatus, String forsendelseId) {
		if (FORSENDELSE_STATUS_EKSPEDERT.equals(forsendelseStatus)) {
			log.info("Forsendelse med forsendelseId={}, status={} er ekspdert og behandlingen avsluttes",
					forsendelseId, forsendelseStatus);
			throw new ForsendelseStatusExpedertKanIkkeDistribuereException(format("Forsendelse med forsendelseId=%s, status=%s er ekspdert og behandlingen avsluttes",
					forsendelseId, forsendelseStatus));
		} else if (FORSENDELSE_STATUS_OPPRETTET.equals(forsendelseStatus)) {
			throw new KunneIkkeDistribuereForsendelseException(format("Kunne ikke distribuere forsendelse med forsendelseId=%s, status=%s", forsendelseId, forsendelseStatus));
		}
	}

	private void validateDistribuerForsendelseTilDpi(DistribuerTilKanal distribuerTilKanal) {
		assertNotNull("DistribuerTilKanal", distribuerTilKanal);
		assertNotBlank("forsendelseId", distribuerTilKanal.getForsendelseId());
	}

	private void assertForsendelseNotNull(HentForsendelseResponse hentForsendelseResponse) {
		assertNotNull("HentForsendelseResponseTo", hentForsendelseResponse);
		assertNotNull("HentForsendelseResponseTo.MottakerTo", hentForsendelseResponse.getMottaker());
	}

	private void validateKjernetid(DistribusjonstidspunktKode distribusjonstidspunkt, String bestillingsId) {
		if (!innenKjernetid(distribusjonstidspunkt)) {
			log.info("Legger melding med distribusjonstidspunkt {} på vente-kø for bestillingsId={}", distribusjonstidspunkt, bestillingsId);
			throw new UtenforKjernetidFunctionalException("Utenfor kjernetid, legges på ventekø");
		}
	}

	private boolean innenKjernetid(DistribusjonstidspunktKode distribusjonstidspunkt) {
		if (distribusjonstidspunkt == null || distribusjonstidspunkt.equals(DistribusjonstidspunktKode.UMIDDELBART)) {
			return true;
		}
		LocalTime tid = LocalTime.now(clock);
		return (tid.isAfter(kjernetidStart) && tid.isBefore(kjernetidSlutt));
	}

	public static DokDistDokumentFraBucket deserializeBucketJsonPayloadToDokdistDokument(String jsonPayload, String objektReferanse) {
		DokDistDokumentFraBucket dokDistDokumentFraBucket;
		try {
			dokDistDokumentFraBucket = JsonSerializer.deserialize(jsonPayload, DokDistDokumentFraBucket.class);
			dokDistDokumentFraBucket.setDokumentObjektReferanse(objektReferanse);
		} catch (IllegalStateException e) {
			throw new KunneIkkeDeserialisereBucketPayloadException(format("Kunne ikke deserialisere jsonPayload fra bucket for dokument med dokumentobjektreferanse=%s. Dokumentet er ikke persistert til bucket med korrekt format!", objektReferanse));
		}
		return dokDistDokumentFraBucket;
	}

	private String getConversationId(HentForsendelseResponse hentForsendelse, String forsendelseId) {
		return isBlank(hentForsendelse.getKonversasjonId()) ? generateKonversasjonsId(forsendelseId) : hentForsendelse.getKonversasjonId();
	}

	private String generateKonversasjonsId(String forsendelseId) {
		String konversasjonsId = UUID.randomUUID().toString();
		administrerForsendelse.oppdaterForsendelseAndDigitalPostkasseInfo(OppdaterForsendelseRequestTo.builder()
				.forsendelseId(forsendelseId)
				.konversasjonId(konversasjonsId)
				.build());
		log.info("Oppdatert forsendelse med forsendelseId={} til konversasjonsId={}", forsendelseId, konversasjonsId);
		return konversasjonsId;
	}
}
