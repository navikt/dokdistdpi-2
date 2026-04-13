package no.nav.dokdistdpi.sdist005;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.ForsendelseStatusResponse;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.HentUekspederteForsendelserConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilregistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentUekspederteForsendelserResponse.DokumentInfoTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentUekspederteForsendelserResponse.UekspedertForsendelse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.map.OpprettForsendelseMapper;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusIkkeFunnetException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static no.nav.dokdistdpi.consumer.dpi.client.StatusType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.MELDINGSFEIL;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DISTRIBUSJONS_SDP_KANAL;

@Slf4j
@Component
public class Sdist005Service {

	private static final ZoneId EUROPE_OSLO = ZoneId.of("Europe/Oslo");
	private final DokdistadminConsumer dokdistadminConsumer;
	private final HentUekspederteForsendelserConsumer hentUekspederteForsendelserConsumer;
	private final DpiClient dpiClient;
	private final OpprettForsendelseMapper opprettForsendelseMapper;

	public Sdist005Service(DokdistadminConsumer dokdistadminConsumer,
						   HentUekspederteForsendelserConsumer hentUekspederteForsendelserConsumer,
						   DpiClient dpiClient) {
		this.dokdistadminConsumer = dokdistadminConsumer;
		this.hentUekspederteForsendelserConsumer = hentUekspederteForsendelserConsumer;
		this.dpiClient = dpiClient;
		this.opprettForsendelseMapper = new OpprettForsendelseMapper();
	}

	@SuppressWarnings("unused")
	@Handler
	public List<DistribuerTilKanal> hentStatusFraAksesspunkt(Exchange exchange) {
		log.info("Sdist005 leter etter ukvitterte forsendelser");
		List<UekspedertForsendelse> ikkeKvitterteForsendelser = hentIkkeKvitterteForsendelser();
		if (ikkeKvitterteForsendelser.isEmpty()) {
			return emptyList();
		}
		log.info("Sdist005 fant antall={} ikke-kvitterte forsendelser", ikkeKvitterteForsendelser.size());

		List<FeiletForsendelseTo> feiledeForsendelser = finnFeiledeForsendelser(ikkeKvitterteForsendelser);
		log.info("Sdist005 fant antall={} ikke-kvitterte forsendelser med endelig status FAILED fra hjørne2", feiledeForsendelser.size());

		if (feiledeForsendelser.isEmpty()) {
			return List.of();
		}

		return feiledeForsendelser.stream()
				.map(feiletForsendelse -> {
					final String nyForsendelseId = behandleFeiletForsendelse(feiletForsendelse);

					return new DistribuerTilKanal().useForsendelseId(nyForsendelseId);
				}).collect(Collectors.toList());
	}

	private String behandleFeiletForsendelse(FeiletForsendelseTo feiletForsendelse) {
		log.info("Sdist005 behandler feilet forsendelse med forsendelseId={}, bestillingsId={} som har endelig status FAILED fra hjørne2",
				feiletForsendelse.getForsendelseId(), feiletForsendelse.getBestillingsId());
		HentForsendelseResponse hentForsendelseResponse = dokdistadminConsumer.hentForsendelse(feiletForsendelse.getForsendelseId());
		final String bestillingsId = randomUUID().toString();

		OpprettForsendelseRequestTo opprettForsendelseRequestTo = opprettForsendelseMapper.map(hentForsendelseResponse, bestillingsId);
		String nyForsendelseId = dokdistadminConsumer.opprettForsendelse(opprettForsendelseRequestTo);

		log.info("Sdist005 har opprettet ny forsendelse med forsendelseId={}, bestillingsId={}", nyForsendelseId, bestillingsId);

		dokdistadminConsumer.feilregistrerForsendelse(FeilregistrerForsendelseRequest.builder()
				.forsendelseId(Long.valueOf(feiletForsendelse.getForsendelseId()))
				.feilTypeCode(MELDINGSFEIL.name())
				.part("AKSESSPUNKT")
				.tidspunkt(feiletForsendelse.getFeiltidspunkt())
				.detaljer(feiletForsendelse.getFeilbeskrivelse())
				.resendingDistribusjonId(bestillingsId)
				.build());
		log.info("Sdist005 har feilregistrert forsendelse med forsendelseId={}, bestillingsId={}",
				feiletForsendelse.getForsendelseId(), feiletForsendelse.getBestillingsId());

		dokdistadminConsumer.oppdaterForsendelse(
				OppdaterForsendelseRequest.builder()
						.forsendelseId(Long.valueOf(nyForsendelseId))
						.forsendelseStatus(KLAR_FOR_DIST.name())
						.build());
		log.info("Sdist005 har oppdatert ny forsendelse med forsendelseId={}, bestillingsId={} til KLAR_FOR_DIST",
				nyForsendelseId, bestillingsId);
		return nyForsendelseId;
	}

	private List<UekspedertForsendelse> hentIkkeKvitterteForsendelser() {
		var response = hentUekspederteForsendelserConsumer.hentForsendelserKvitteringIkkeMottatt(DISTRIBUSJONS_SDP_KANAL, 6);

		return response.getUekspederteForsendelser()
				.stream()
				.filter(f -> OVERSENDT.name().equals(f.getDistribusjonStatus()))
				.toList();
	}

	private List<FeiletForsendelseTo> finnFeiledeForsendelser(List<UekspedertForsendelse> ikkeKvitterteForsendelser) {
		return ikkeKvitterteForsendelser.stream()
				.map(f -> {
					final DokumentInfoTo dokumentInfoTo = f.getDokumenter().getFirst();
					var feiletForsendelse = hentForsendelseStatuser(dokumentInfoTo.getKonversasjonId());
					if (feiletForsendelse.isPresent()) {
						ForsendelseStatusResponse forsendelseStatusResponse = feiletForsendelse.get();
						return FeiletForsendelseTo.builder()
								.forsendelseId(dokumentInfoTo.getForsendelseId())
								.bestillingsId(dokumentInfoTo.getDokumentId())
								.feilbeskrivelse(forsendelseStatusResponse.getBeskrivelse())
								.feiltidspunkt(forsendelseStatusResponse.getTimestamp().atZoneSameInstant(EUROPE_OSLO).toLocalDateTime())
								.build();
					} else {
						log.warn("Sdist005 fant ikke-kvittert forsendelse med forsendelseId={}, bestillingsId={} UTEN endelig FEILET status i hjørne2",
								dokumentInfoTo.getForsendelseId(), dokumentInfoTo.getDokumentId());
						return null;
					}
				}).filter(Objects::nonNull)
				.toList();
	}

	private Optional<ForsendelseStatusResponse> hentForsendelseStatuser(String konversasjonId) {
		try {
			List<ForsendelseStatusResponse> forsendelseStatusResponses = dpiClient.hentForsendelseStatus(konversasjonId);
			return forsendelseStatusResponses.stream()
					.filter(statusResponse -> FEILET == statusResponse.getStatus())
					.findFirst();
		} catch (ForsendelseStatusIkkeFunnetException | SikkerDigitalPostException e) {
			return Optional.empty();
		}
	}
}
