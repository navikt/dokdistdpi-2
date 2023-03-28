package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponseTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.MELDINGSFEIL;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarselType.VARSLINGSFEIL;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.FEILET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.OVERSENDT;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.RETURPOSTBEHANDLET;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_STATUS;

@Slf4j
@Component
public class DpiKvitteringService {

	private static final String KONVERSASJONS_ID = "konversasjonsId";

	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final DokdistadminConsumer dokdistadminConsumer;

	@Autowired
	public DpiKvitteringService(AdministrerForsendelseConsumer administrerForsendelse,
								DokdistadminConsumer dokdistadminConsumer) {
		this.administrerForsendelse = administrerForsendelse;
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	boolean erStatusEkspedertOrReturOrFeilet(DpiMelding dpiMelding, Exchange exchange) {
		FinnForsendelseResponseTo finnForsendelseResponse = finnForsendelse(dpiMelding.getKonversasjonsId());
		HentForsendelseResponse hentForsendelseResponse = hentForsendelse(finnForsendelseResponse);
		assertNotNull("HentForsendelseResponseTo", hentForsendelseResponse);
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, finnForsendelseResponse.getForsendelseId());
		exchange.setProperty(PROPERTY_FORSENDELSE_STATUS, hentForsendelseResponse.getForsendelseStatus());

		return isFerdigstiltForsendelseStatus(ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus()));
	}

	DistribuerTilKanal persistAndCreateNewForsendelse(DpiMelding dpiMelding,
													  OpprettForsendelseRequestTo request, String forsendelseId) {
		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();
		if (dpiMelding instanceof VarslingFeiletKvittering varslingFeiletKvittering) {
			if (VARSLINGFEILET.equals(varslingFeiletKvittering.getKvitteringType())) {
				createAndUpdateFeilForsendelse(dpiMelding, request, forsendelseId, distribuerTilKanal);
			}
		} else if (dpiMelding instanceof DpiFeilKvittering) {
			createAndUpdateFeilForsendelse(dpiMelding, request, forsendelseId, distribuerTilKanal);
		}
		return distribuerTilKanal;
	}

	private void createAndUpdateFeilForsendelse(DpiMelding dpiMelding, OpprettForsendelseRequestTo request, String forsendelseId, DistribuerTilKanal distribuerTilKanal) {

		String nyForsendelseId = dokdistadminConsumer.opprettForsendelse(request);

		createFeilRegistrerForsendelseKvittering(forsendelseId, dpiMelding, request);

		log.info("Forsendelsen med forsendelseId={} er feilregistrert i dokdist databasen.", forsendelseId);

		administrerForsendelse.oppdaterForsendelseAndDigitalPostkasseInfo(OppdaterForsendelseRequestTo.builder()
				.forsendelseId(nyForsendelseId)
				.forsendelseStatus(KLAR_FOR_DIST.name())
				.build());

		distribuerTilKanal.setForsendelseId(nyForsendelseId);
	}

	private void createFeilRegistrerForsendelseKvittering(String forsendelseId, DpiMelding dpiMelding,
														  OpprettForsendelseRequestTo request) {
		if (dpiMelding instanceof VarslingFeiletKvittering varslingFeiletKvittering) {
			administrerForsendelse.feilRegistrerForsendelse(FeilRegistrerForsendelseRequest.builder()
					.forsendelseId(forsendelseId)
					.type(VARSLINGSFEIL.name())
					.tidspunkt(varslingFeiletKvittering.getTidspunkt().toLocalDateTime())
					.detaljer(varslingFeiletKvittering.getVarslingskanal() + ":" + varslingFeiletKvittering.getBeskrivelse())
					.resendingDistribusjonId(request.getBestillingsId())
					.build());
		} else if (dpiMelding instanceof DpiFeilKvittering dpiFeil) {
			administrerForsendelse.feilRegistrerForsendelse(FeilRegistrerForsendelseRequest.builder()
					.forsendelseId(forsendelseId)
					.type(MELDINGSFEIL.name())
					.part(dpiFeil.getFeiltype().name())
					.tidspunkt(dpiFeil.getTidspunkt().toLocalDateTime())
					.detaljer(dpiFeil.getDetaljer())
					.resendingDistribusjonId(request.getBestillingsId())
					.build());
		}
	}

	HentForsendelseResponse hentForsendelse(FinnForsendelseResponseTo finnForsendelseResponse) {
		validateFinnForsendelse(finnForsendelseResponse);
		log.info("Fant forsendelse med forsendelseId={}", finnForsendelseResponse.getForsendelseId());
		return dokdistadminConsumer.hentForsendelse(finnForsendelseResponse.getForsendelseId());
	}

	FinnForsendelseResponseTo finnForsendelse(String konversasjonsId) {
		assertNotBlank("konversasjonsId", konversasjonsId);
		FinnForsendelseRequestTo finnForsendelseRequest = FinnForsendelseRequestTo.builder()
				.oppslagsNoekkel(KONVERSASJONS_ID)
				.verdi(konversasjonsId)
				.build();
		return administrerForsendelse.finnForsendelse(finnForsendelseRequest);
	}

	private void validateFinnForsendelse(FinnForsendelseResponseTo responseTo) {
		assertNotNull("FinnForsendelseResponseTo", responseTo);
		assertNotBlank("FinnForsendelseResponseTo.ForsendelseId", String.valueOf(responseTo.getForsendelseId()));

	}

	private boolean isFerdigstiltForsendelseStatus(ForsendelseStatus forsendelseStatus) {
		assertNotBlank("ForsendelseStatus", forsendelseStatus.name());
		List<ForsendelseStatus> forsendelseStatuses = Arrays.asList(EKSPEDERT, RETURPOSTBEHANDLET, FEILET);
		return forsendelseStatuses.contains(forsendelseStatus);
	}

	boolean isOversendtOrBekreftet(ForsendelseStatus status) {
		return OVERSENDT.equals(status) || BEKREFTET.equals(status);
	}

	private void assertNotBlank(String feltnavn, String value) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(String.format("%s kan ikke være null", feltnavn));
		}
	}

	private void assertNotNull(String feltnavn, Object object) {
		if (object == null) {
			throw new IllegalArgumentException(String.format("%s kan ikke være null", feltnavn));
		}
	}
}
