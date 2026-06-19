package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DistribusjonInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.Notifikasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper;
import no.nav.dokdistdpi.service.DigitalPostService;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static java.lang.Long.valueOf;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.map.VarselMapper.mapVarslerHvisRiktigDistribusjonstype;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;

@Slf4j
@Component
public class OppdaterForsendelse {

	private static final String OSLO_ZONE_ID = "Europe/Oslo";

	private final DokdistadminConsumer dokdistadminConsumer;
	private final DpiKvitteringService dpiKvitteringService;
	private final DigitalPostService digitalPostService;
	private final OppdaterVarselInfoMapper oppdaterVarselInfoMapper;

	public OppdaterForsendelse(DpiKvitteringService dpiKvitteringService,
							   DokdistadminConsumer dokdistadminConsumer,
							   DigitalPostService digitalPostService) {
		this.dpiKvitteringService = dpiKvitteringService;
		this.dokdistadminConsumer = dokdistadminConsumer;
		this.digitalPostService = digitalPostService;
		this.oppdaterVarselInfoMapper = new OppdaterVarselInfoMapper();
	}

	@Handler
	public void oppdaterForsendelse(DpiMelding dpiMelding, Exchange exchange) {
		if (isLeveringsKvittering(dpiMelding)) {
			String konversasjonsId = dpiMelding.getKonversasjonsId();
			String forsendelseId = dpiKvitteringService.finnForsendelse(konversasjonsId);
			HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(forsendelseId);
			exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());

			ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());

			final LocalDateTime ekspedertDato = dpiMelding.getTidspunkt()
					.atZoneSameInstant(ZoneId.of(OSLO_ZONE_ID))
					.toLocalDateTime();

			switch (forsendelseStatus) {
				case OVERSENDT, BEKREFTET -> oppdaterForsendelseStatusTilEkspedert(forsendelseId, ekspedertDato);
				case KLAR_FOR_DIST ->
						oppdaterForsendelseMedDigitalKontaktinfoOgVarsler(hentForsendelseResponse, forsendelseId, ekspedertDato);
			}
		}
	}

	private boolean isLeveringsKvittering(DpiMelding dpiMelding) {
		return dpiMelding instanceof LeveringsKvittering leveringsKvittering &&
				LEVERING.equals(leveringsKvittering.getKvitteringType());
	}

	private void oppdaterForsendelseStatusTilEkspedert(String forsendelseId, LocalDateTime ekspedertDato) {
		dokdistadminConsumer.oppdaterForsendelse(
				OppdaterForsendelseRequest.builder()
						.forsendelseId(valueOf(forsendelseId))
						.forsendelseStatus(EKSPEDERT.name())
						.ekspedertDato(ekspedertDato)
						.build());
	}

	private void oppdaterForsendelseMedDigitalKontaktinfoOgVarsler(HentForsendelseResponse hentForsendelseResponse,
																   String forsendelseId,
																   LocalDateTime ekspedertDato) {
		log.info("Qdist014 oppdaterer forsendelseId={} med digital kontaktinfo og varsler", forsendelseId);

		DistribusjonInfo distribusjonInfo = digitalPostService.hentDokumenttypeInfo(hentForsendelseResponse);
		VarselInfo varselInfo = digitalPostService.getVarselInfo(distribusjonInfo);
		SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = digitalPostService.hentDigitalKontaktInfo(hentForsendelseResponse);

		Varsler varsler = mapVarslerHvisRiktigDistribusjonstype(hentForsendelseResponse, varselInfo, sikkerDigitalKontaktInfo);

		oppdaterForsendelseDigitalKontaktinfo(Long.parseLong(forsendelseId), sikkerDigitalKontaktInfo.getLeverandoerAdresse(),
				sikkerDigitalKontaktInfo.getBrukerAdresse(), ekspedertDato);

		if (varsler != null) {
			oppdaterForsendelseVarselInfo(Long.parseLong(forsendelseId), varsler, hentForsendelseResponse.getDistribusjonstype());
		}
		log.info("Qdist014 har oppdatert forsendelseId={} med digital kontaktinfo og varsler", forsendelseId);
	}

	private void oppdaterForsendelseDigitalKontaktinfo(long forsendelseId, String leverandoerAdresse,
													   String brukerAdresse, LocalDateTime ekspedertDato) {
		OppdaterForsendelseRequest oppdaterForsendelseRequest = OppdaterForsendelseRequest.builder()
				.forsendelseId(forsendelseId)
				.forsendelseStatus(EKSPEDERT.name())
				.ekspedertDato(ekspedertDato)
				.digitalLeverandoeradresse(leverandoerAdresse)
				.digitalPostkasseadresse(brukerAdresse)
				.build();

		dokdistadminConsumer.oppdaterForsendelse(oppdaterForsendelseRequest);
	}

	private void oppdaterForsendelseVarselInfo(long forsendelseId, Varsler varsler, DistribusjonsTypeKode distribusjonsTypeKode) {
		List<Notifikasjon> notifikasjoner = oppdaterVarselInfoMapper.mapNotifikasjon(varsler, distribusjonsTypeKode);

		OppdaterVarselInfoRequest oppdaterVarselInfoRequest = new OppdaterVarselInfoRequest(forsendelseId, notifikasjoner);

		dokdistadminConsumer.oppdaterVarselInfo(oppdaterVarselInfoRequest);
	}
}
