package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilregistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.map.OpprettForsendelseMapper;
import no.nav.dokdistdpi.exception.functional.InvalidForsendelseStatusException;
import no.nav.dokdistdpi.exception.functional.InvalidKvitteringTypeException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.MELDINGSFEIL;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.VARSLINGSFEIL;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.qdist014.DpiKvitteringService.isOversendtOrBekreftet;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;

@Slf4j
@Component
public class Qdist014Service {

	private final DokdistadminConsumer dokdistadminConsumer;
	private final DpiKvitteringService dpiKvitteringService;
	private final OpprettForsendelseMapper mapper;

	public Qdist014Service(DokdistadminConsumer dokdistadminConsumer, DpiKvitteringService dpiKvitteringService) {
		this.dokdistadminConsumer = dokdistadminConsumer;
		this.dpiKvitteringService = dpiKvitteringService;
		this.mapper = new OpprettForsendelseMapper();
	}

	@Handler
	public DistribuerTilKanal handleKvitteringFraDpi(DpiMelding dpiMelding, Exchange exchange) {
		String konversasjonsId = dpiMelding.getKonversasjonsId();
		final String bestillingId = UUID.randomUUID().toString();
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingId);
		String forsendelseId = dpiKvitteringService.finnForsendelse(konversasjonsId);
		HentForsendelseResponse hentForsendelseResponse = dpiKvitteringService.hentForsendelse(forsendelseId);
		ForsendelseStatus forsendelseStatus = ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus());

		if (isOversendtOrBekreftet(forsendelseStatus)) {
			OpprettForsendelseRequestTo opprettForsendelseRequestTo = mapper.map(hentForsendelseResponse, bestillingId);
			DistribuerTilKanal distribuerTilKanal = validateReceiptAndCreateFallback(dpiMelding, opprettForsendelseRequestTo, forsendelseId);
			exchange.setProperty(PROPERTY_FORSENDELSE_ID, distribuerTilKanal.getForsendelseId());
			return distribuerTilKanal;
		}
		throw new InvalidForsendelseStatusException(String.format("Ugyldig forsendelse med forsendelseStatus=%s", forsendelseStatus));
	}

	private DistribuerTilKanal validateReceiptAndCreateFallback(DpiMelding dpiMelding,
																OpprettForsendelseRequestTo request, String forsendelseId) {
		boolean isVarslingFeilet = dpiMelding instanceof VarslingFeiletKvittering varslingFeiletKvittering && VARSLINGFEILET.equals(varslingFeiletKvittering.getKvitteringType());
		boolean isDpiFeilKvittering = dpiMelding instanceof DpiFeilKvittering;
		if (isVarslingFeilet || isDpiFeilKvittering) {
			return markDpiForsendelseAsFailedAndCreateFallback(dpiMelding, request, forsendelseId);
		}
		throw new InvalidKvitteringTypeException("Kvittering for forsendelse med forsendelseId=%s var av uventet type %s"
				.formatted(forsendelseId, dpiMelding.getClass().getSimpleName()));
	}

	private DistribuerTilKanal markDpiForsendelseAsFailedAndCreateFallback(DpiMelding dpiMelding, OpprettForsendelseRequestTo request, String forsendelseId) {

		String nyForsendelseId = dokdistadminConsumer.opprettForsendelse(request);

		createFeilRegistrerForsendelseKvittering(forsendelseId, dpiMelding, request);

		log.info("Forsendelsen med forsendelseId={} er feilregistrert i dokdist databasen. Bestiller ny forsendelse til sentral print med forsendelseId={}", forsendelseId, nyForsendelseId);

		dokdistadminConsumer.oppdaterForsendelse(OppdaterForsendelseRequest.builder()
				.forsendelseId(Long.valueOf(nyForsendelseId))
				.forsendelseStatus(KLAR_FOR_DIST.name())
				.build());

		return new DistribuerTilKanal().useForsendelseId(nyForsendelseId);
	}

	private void createFeilRegistrerForsendelseKvittering(String forsendelseId, DpiMelding dpiMelding,
														  OpprettForsendelseRequestTo request) {
		if (dpiMelding instanceof VarslingFeiletKvittering varslingFeiletKvittering) {
			dokdistadminConsumer.feilregistrerForsendelse(FeilregistrerForsendelseRequest.builder()
					.forsendelseId(Long.valueOf(forsendelseId))
					.feilTypeCode(VARSLINGSFEIL.name())
					.tidspunkt(varslingFeiletKvittering.getTidspunkt().toLocalDateTime())
					.detaljer(varslingFeiletKvittering.getVarslingskanal() + ":" + varslingFeiletKvittering.getBeskrivelse())
					.resendingDistribusjonId(request.getBestillingsId())
					.build());
		} else if (dpiMelding instanceof DpiFeilKvittering dpiFeil) {
			dokdistadminConsumer.feilregistrerForsendelse(FeilregistrerForsendelseRequest.builder()
					.forsendelseId(Long.valueOf(forsendelseId))
					.feilTypeCode(MELDINGSFEIL.name())
					.part(dpiFeil.getFeiltype().name())
					.tidspunkt(dpiFeil.getTidspunkt().toLocalDateTime())
					.detaljer(dpiFeil.getDetaljer())
					.resendingDistribusjonId(request.getBestillingsId())
					.build());
		}
	}
}
