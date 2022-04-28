package no.nav.dokdistdpi.sdist005;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.ForsendelseStatusResponse;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.AvstemForsendelseResponseTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.PersisterForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.PersisterForsendelseResponseTo;
import no.nav.dokdistdpi.consumer.rdist001.map.PersisterForsendelseMapper;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusIkkeFunnetException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static java.util.UUID.randomUUID;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.MELDINGSFEIL;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DISTRIBUSJONS_SDP_KANAL;

@Slf4j
@Component
public class Sdist005Service {

	private final AdministrerForsendelseConsumer administrerForsendelseConsumer;
	private final DpiClient dpiClient;
	private final PersisterForsendelseMapper persisterForsendelseMapper;

	@Autowired
	public Sdist005Service(AdministrerForsendelseConsumer administrerForsendelseConsumer,
						   DpiClient dpiClient) {
		this.administrerForsendelseConsumer = administrerForsendelseConsumer;
		this.dpiClient = dpiClient;
		this.persisterForsendelseMapper = new PersisterForsendelseMapper();
	}

	@Handler
	public List<DistribuerTilKanal> hentStatusFraAksesspunkt(Exchange exchange) {
		List<AvstemForsendelseResponseTo> ikkeKvitterteForsendelser = hentIkkeKvitterteForsendelser();
		if (ikkeKvitterteForsendelser.isEmpty()) {
			return List.of();
		}
		log.info("sdist005 fant antall={} ikke-kvitterte forsendelser", ikkeKvitterteForsendelser.size());

		List<FeiletForsendelseTo> feiledeForsendelser = finnFeiledeForsendelser(ikkeKvitterteForsendelser);
		log.info("sdist005 fant antall={} ikke-kvitterte forsendelser med endelig status FAILED fra hjørne2", feiledeForsendelser.size());

		return feiledeForsendelser.stream()
				.map(feiletForsendelse -> {
					final String nyForsendelseId = behandleFeiletForsendelse(feiletForsendelse);

					DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
					distribuerTilKanal.setForsendelseId(nyForsendelseId);
					return distribuerTilKanal;
				}).collect(Collectors.toList());
	}

	private String behandleFeiletForsendelse(FeiletForsendelseTo feiletForsendelse) {
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelseConsumer.hentForsendelse(feiletForsendelse.getForsendelseId());
		final String bestillingsId = randomUUID().toString();
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = persisterForsendelseMapper.map(hentForsendelseResponse, bestillingsId);
		PersisterForsendelseResponseTo persisterForsendelseResponseTo = administrerForsendelseConsumer.persisterForsendelse(persisterForsendelseRequestTo);
		final String nyForsendelseId = valueOf(persisterForsendelseResponseTo.getForsendelseId());
		log.info("sdist005 har opprettet ny forsendelse med forsendelseId={}, bestillingsId={}", nyForsendelseId, bestillingsId);

		administrerForsendelseConsumer.feilRegistrerForsendelse(FeilRegistrerForsendelseRequest.builder()
				.forsendelseId(feiletForsendelse.getForsendelseId())
				.type(MELDINGSFEIL.name())
				.part("AKSESSPUNKT")
				.tidspunkt(feiletForsendelse.getFeiltidspunkt())
				.detaljer(feiletForsendelse.getFeilbeskrivelse())
				.resendingDistribusjonId(bestillingsId)
				.build());
		log.info("sdist005 har feilregistrert forsendelse med forsendelseId={}, bestillingsId={}", feiletForsendelse.getForsendelseId(),
				feiletForsendelse.getBestillingsId());

		administrerForsendelseConsumer.oppdaterForsendelseStatus(nyForsendelseId, KLAR_FOR_DIST.name());
		log.info("sdist005 har oppdatert ny forsendelse med forsendelseId={}, bestillingsId={} til KLAR_FOR_DIST", feiletForsendelse.getForsendelseId(),
				feiletForsendelse.getBestillingsId());
		return nyForsendelseId;
	}

	private List<AvstemForsendelseResponseTo> hentIkkeKvitterteForsendelser() {
		return administrerForsendelseConsumer.hentForsendelserKvitteringIkkeMottatt(DISTRIBUSJONS_SDP_KANAL, 6)
				.stream()
				.filter(f -> OVERSENDT.name().equals(f.getDistribusjonStatus()))
				.toList();
	}

	private List<FeiletForsendelseTo> finnFeiledeForsendelser(List<AvstemForsendelseResponseTo> ikkeKvitterteForsendelser) {
		return ikkeKvitterteForsendelser.stream()
				.map(f -> {
					final AvstemForsendelseResponseTo.DokumentInfoTo dokumentInfoTo = f.getDokumenter().get(0);
					var feiletForsendelse = hentForsendelseStatuser(dokumentInfoTo.getDokumentId());
					if (feiletForsendelse.isPresent()) {
						ForsendelseStatusResponse forsendelseStatusResponse = feiletForsendelse.get();
						return FeiletForsendelseTo.builder()
								.forsendelseId(dokumentInfoTo.getForsendelseId())
								.bestillingsId(dokumentInfoTo.getDokumentId())
								.feilbeskrivelse(forsendelseStatusResponse.getBeskrivelse())
								.feiltidspunkt(forsendelseStatusResponse.getTimestamp())
								.build();
					} else {
						return null;
					}
				}).filter(Objects::nonNull)
				.toList();
	}

	private Optional<ForsendelseStatusResponse> hentForsendelseStatuser(String bestillingsId) {
		try {
			List<ForsendelseStatusResponse> forsendelseStatusResponses = dpiClient.hentForsendelseStatus(bestillingsId);
			return forsendelseStatusResponses.stream()
					.filter(statusResponse -> ForsendelseStatusResponse.StatusType.FEILET == statusResponse.getStatus())
					.findFirst();
		} catch (ForsendelseStatusIkkeFunnetException | SikkerDigitalPostException e) {
			return Optional.empty();
		}
	}
}
