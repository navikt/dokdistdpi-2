package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponseTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistdpi.qdist014.domain.ForsendelseStatus;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.qdist014.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.qdist014.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;

@Slf4j
@Component
public class OppdaterForsendelseStatus {

	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final DpiKvitteringService dpiKvitteringService;

	@Autowired
	public OppdaterForsendelseStatus(AdministrerForsendelseConsumer administrerForsendelse, DpiKvitteringService dpiKvitteringService) {
		this.administrerForsendelse = administrerForsendelse;
		this.dpiKvitteringService = dpiKvitteringService;
	}

	@Handler
	public void oppdaterForsendelseStatusToEkspedert(DpiMelding dpiMelding, Exchange exchange) {
		String konversasjonsId = dpiMelding.getKonversasjonsId();
		FinnForsendelseResponseTo finnForsendelseResponse = dpiKvitteringService.finnForsendelse(konversasjonsId);
		HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(finnForsendelseResponse);
		ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, hentForsendelseResponse.getBestillingsId());

		validateKlarForDistStatus(forsendelseStatus);
		if (dpiKvitteringService.isOversendtOrBekreftet(forsendelseStatus)) {
			oppdaterForsendelseStatus(dpiMelding, finnForsendelseResponse.getForsendelseId());
		}
	}

	private void oppdaterForsendelseStatus(DpiMelding dpiMelding, String forsendelseId) {
		if (dpiMelding instanceof LeveringsKvittering leveringsKvittering) {
			if (LEVERING.equals(leveringsKvittering.getKvitteringType())) {
				administrerForsendelse.oppdaterForsendelseStatus(forsendelseId, EKSPEDERT.name());
			}
		}
	}

	private void validateKlarForDistStatus(ForsendelseStatus forsendelseStatus) {
		if (KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(String.format("Ugyldig forsendelse status med status=%s", forsendelseStatus));
		}
	}
}
