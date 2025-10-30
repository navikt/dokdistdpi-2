package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DistribusjonInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
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

import java.util.List;

import static java.lang.Long.valueOf;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.map.VarselMapper.mapVarslerHvisRiktigDistribusjonstype;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;

@Slf4j
@Component
public class OppdaterForsendelse {

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
		String konversasjonsId = dpiMelding.getKonversasjonsId();
		String forsendelseId = dpiKvitteringService.finnForsendelse(konversasjonsId);
		HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(forsendelseId);
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());

		ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());

		switch (forsendelseStatus) {
			case OVERSENDT, BEKREFTET -> oppdaterForsendelseStatusTilEkspedert(dpiMelding, forsendelseId);
			case KLAR_FOR_DIST ->
					oppdaterForsendelseMedDigitalKontaktinfoOgVarsler(hentForsendelseResponse, forsendelseId, dpiMelding);
		}
	}

	private void oppdaterForsendelseStatusTilEkspedert(DpiMelding dpiMelding, String forsendelseId) {
		if (dpiMelding instanceof LeveringsKvittering leveringsKvittering) {
			if (LEVERING.equals(leveringsKvittering.getKvitteringType())) {
				dokdistadminConsumer.oppdaterForsendelse(
						OppdaterForsendelseRequest.builder()
								.forsendelseId(valueOf(forsendelseId))
								.forsendelseStatus(EKSPEDERT.name())
								.build());
			}
		}
	}

	private void oppdaterForsendelseMedDigitalKontaktinfoOgVarsler(HentForsendelseResponse hentForsendelseResponse, String forsendelseId, DpiMelding dpiMelding) {
		if (dpiMelding instanceof LeveringsKvittering leveringsKvittering) {
			if (LEVERING.equals(leveringsKvittering.getKvitteringType())) {
				log.info("Qdist014 oppdaterer forsendelseId={} med digital kontaktinfo og varsler", forsendelseId);

				DistribusjonInfo distribusjonInfo = digitalPostService.hentDokumenttypeInfo(hentForsendelseResponse);
				VarselInfo varselInfo = digitalPostService.getVarselInfo(distribusjonInfo);
				SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = digitalPostService.hentDigitalKontaktInfo(hentForsendelseResponse, varselInfo);

				Varsler varsler = mapVarslerHvisRiktigDistribusjonstype(hentForsendelseResponse, varselInfo, sikkerDigitalKontaktInfo);

				oppdaterForsendelse(Forsendelse.builder()
						.forsendelseId(Long.valueOf(forsendelseId))
						.mottakerSertifikat(sikkerDigitalKontaktInfo.getLeverandoerSertifikat())
						.digitalPostLeverandoerAdresse(sikkerDigitalKontaktInfo.getLeverandoerAdresse())
						.digital(DigitalPost.builder()
								.varsler(varsler)
								.build())
						.build());

				log.info("Qdist014 har oppdatert forsendelseId={} med digital kontaktinfo og varsler", forsendelseId);
			}
		}
	}

	private void oppdaterForsendelse(Forsendelse forsendelse) {
		oppdaterForsendelseDigitalKontaktinfo(forsendelse);

		if (forsendelse.getDigital().getVarsler() != null) {
			oppdaterForsendelseVarselInfo(forsendelse);
		}
	}

	private void oppdaterForsendelseDigitalKontaktinfo(Forsendelse forsendelse) {
		OppdaterForsendelseRequest oppdaterForsendelseRequest = OppdaterForsendelseRequest.builder()
				.forsendelseId(forsendelse.getForsendelseId())
				.forsendelseStatus(EKSPEDERT.name())
				.digitalLeverandoeradresse(forsendelse.getMottakerSertifikat())
				.digitalPostkasseadresse(forsendelse.getDigitalPostLeverandoerAdresse())
				.build();

		dokdistadminConsumer.oppdaterForsendelse(oppdaterForsendelseRequest);
	}

	private void oppdaterForsendelseVarselInfo(Forsendelse forsendelse) {
		List<Notifikasjon> notifikasjoner = oppdaterVarselInfoMapper.mapNotifikasjon(
				forsendelse.getDigital().getVarsler(),
				forsendelse.getDistribusjonsTypeKode());

		OppdaterVarselInfoRequest oppdaterVarselInfoRequest = new OppdaterVarselInfoRequest(forsendelse.getForsendelseId(), notifikasjoner);

		dokdistadminConsumer.oppdaterVarselInfo(oppdaterVarselInfoRequest);
	}
}
