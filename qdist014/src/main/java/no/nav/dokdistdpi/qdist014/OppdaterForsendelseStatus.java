package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DistribusjonInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistdpi.service.DigitalPostService;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static java.lang.Long.valueOf;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.map.VarselMapper.mapVarsler;
import static no.nav.dokdistdpi.qdist014.DpiKvitteringService.isOversendtOrBekreftet;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;

@Slf4j
@Component
public class OppdaterForsendelseStatus {

	private final DokdistadminConsumer dokdistadminConsumer;
	private final DpiKvitteringService dpiKvitteringService;
	private final DigitalPostService digitalPostService;

	public OppdaterForsendelseStatus(DpiKvitteringService dpiKvitteringService,
									 DokdistadminConsumer dokdistadminConsumer,
									 DigitalPostService digitalPostService) {
		this.dpiKvitteringService = dpiKvitteringService;
		this.dokdistadminConsumer = dokdistadminConsumer;
		this.digitalPostService = digitalPostService;
	}

	@Handler
	public void oppdaterForsendelseStatusToEkspedert(DpiMelding dpiMelding, Exchange exchange) {
		String konversasjonsId = dpiMelding.getKonversasjonsId();
		String forsendelseId = dpiKvitteringService.finnForsendelse(konversasjonsId);
		HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(forsendelseId);
		ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());



		if (isOversendtOrBekreftet(forsendelseStatus) || KLAR_FOR_DIST.equals(forsendelseStatus)) {
			oppdaterForsendelseStatus(dpiMelding, forsendelseId);
			validerVarselInfo(hentForsendelseResponse);
		}
	}

	private void validerVarselInfo(HentForsendelseResponse hentForsendelseResponse) {
		DistribusjonInfo distribusjonInfo = digitalPostService.hentDokumenttypeInfo(hentForsendelseResponse);

		VarselInfo varselInfo = digitalPostService.getVarselInfo(distribusjonInfo);
		if (varselInfo == null) {
			SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = digitalPostService.hentDigitalKontaktInfo(hentForsendelseResponse, varselInfo);

		}
	}

	private void oppdaterForsendelseStatus(DpiMelding dpiMelding, String forsendelseId) {
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
}
