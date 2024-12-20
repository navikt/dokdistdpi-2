package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilregistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.exception.functional.InvalidKvitteringTypeException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
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
import static no.nav.dokdistdpi.consumer.rdist001.domain.Oppslagsnoekkel.KONVERSASJONSID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_STATUS;

@Slf4j
@Component
public class DpiKvitteringService {

	private final DokdistadminConsumer dokdistadminConsumer;

	public DpiKvitteringService(DokdistadminConsumer dokdistadminConsumer) {
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	boolean erStatusEkspedertOrReturOrFeilet(DpiMelding dpiMelding, Exchange exchange) {
		String forsendelseId = finnForsendelse(dpiMelding.getKonversasjonsId());
		HentForsendelseResponse hentForsendelseResponse = hentForsendelse(forsendelseId);
		assertNotNull("HentForsendelseResponseTo", hentForsendelseResponse);

		exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
		exchange.setProperty(PROPERTY_FORSENDELSE_STATUS, hentForsendelseResponse.getForsendelseStatus());

		return isFerdigstiltForsendelseStatus(ForsendelseStatus.valueOf(hentForsendelseResponse.getForsendelseStatus()));
	}

	DistribuerTilKanal persistAndCreateNewForsendelse(DpiMelding dpiMelding,
													  OpprettForsendelseRequestTo request, String forsendelseId) {
		if ((dpiMelding instanceof VarslingFeiletKvittering varslingFeiletKvittering && VARSLINGFEILET.equals(varslingFeiletKvittering.getKvitteringType())) ||
				(dpiMelding instanceof DpiFeilKvittering)) {
			return createAndUpdateFeilForsendelse(dpiMelding, request, forsendelseId);
		}
		throw new InvalidKvitteringTypeException("Kvittering for forsendelse med forsendelseId=%s var av uventet type %s"
				.formatted(forsendelseId, dpiMelding.getClass().getSimpleName()));
	}

	private DistribuerTilKanal createAndUpdateFeilForsendelse(DpiMelding dpiMelding, OpprettForsendelseRequestTo request, String forsendelseId) {

		String nyForsendelseId = dokdistadminConsumer.opprettForsendelse(request);

		createFeilRegistrerForsendelseKvittering(forsendelseId, dpiMelding, request);

		log.info("Forsendelsen med forsendelseId={} er feilregistrert i dokdist databasen.", forsendelseId);

		dokdistadminConsumer.oppdaterForsendelse(OppdaterForsendelseRequest.builder()
				.forsendelseId(Long.valueOf(nyForsendelseId))
				.forsendelseStatus(KLAR_FOR_DIST.name())
				.build());

		return new DistribuerTilKanal().useForsendelseId(forsendelseId);
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

	HentForsendelseResponse hentForsendelse(String forsendelseId) {
		validateFinnForsendelse(forsendelseId);
		log.info("Fant forsendelse med forsendelseId={}", forsendelseId);
		return dokdistadminConsumer.hentForsendelse(forsendelseId);
	}

	String finnForsendelse(String konversasjonsId) {
		assertNotBlank("konversasjonsId", konversasjonsId);
		FinnForsendelseRequest finnForsendelseRequest = FinnForsendelseRequest.builder()
				.oppslagsnoekkel(KONVERSASJONSID)
				.verdi(konversasjonsId)
				.build();
		return dokdistadminConsumer.finnForsendelse(finnForsendelseRequest);
	}

	private void validateFinnForsendelse(String forsendelseId) {
		assertNotBlank("ForsendelseId", forsendelseId);
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
