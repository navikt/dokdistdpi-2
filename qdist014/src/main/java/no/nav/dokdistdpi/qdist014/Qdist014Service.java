package no.nav.dokdistdpi.qdist014;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.map.OpprettForsendelseMapper;
import no.nav.dokdistdpi.exception.functional.InvalidForsendelseStatusException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;

@Component
public class Qdist014Service {

	private final DpiKvitteringService dpiKvitteringService;
	private final OpprettForsendelseMapper mapper;

	public Qdist014Service(DpiKvitteringService dpiKvitteringService) {
		this.dpiKvitteringService = dpiKvitteringService;
		this.mapper = new OpprettForsendelseMapper();
	}

	@Handler
	public DistribuerTilKanal handleKvitteringFraDpi(DpiMelding dpiMelding, Exchange exchange) {
		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();

		String konversasjonsId = dpiMelding.getKonversasjonsId();
		final String bestillingId = UUID.randomUUID().toString();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingId);
		FinnForsendelseResponse finnForsendelseResponse = dpiKvitteringService.finnForsendelse(konversasjonsId);
		HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(finnForsendelseResponse);
		OpprettForsendelseRequestTo opprettForsendelseRequestTo = mapper.map(hentForsendelseResponse, bestillingId);
		ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());

		validateForsendelseStatusErKlarForDist(forsendelseStatus);

		if (isOversendtOrBekreftet(forsendelseStatus)) {
			distribuerTilKanal = dpiKvitteringService.persistAndCreateNewForsendelse(dpiMelding, opprettForsendelseRequestTo, finnForsendelseResponse.getForsendelseId());
			exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());
		}
		return distribuerTilKanal;
	}

	private boolean isOversendtOrBekreftet(ForsendelseStatus status) {
		return OVERSENDT.equals(status) || BEKREFTET.equals(status);
	}

	private void validateForsendelseStatusErKlarForDist(ForsendelseStatus forsendelseStatus) {
		if (KLAR_FOR_DIST.equals(forsendelseStatus)) {
			throw new InvalidForsendelseStatusException(String.format("Ugyldig forsendelse med forsendelseStatus=%s", forsendelseStatus));
		}
	}
}
