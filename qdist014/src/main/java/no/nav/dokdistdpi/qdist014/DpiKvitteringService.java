package no.nav.dokdistdpi.qdist014;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.rdist001.DokdistadminConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.BEKREFTET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.FEILET;
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


	HentForsendelseResponse hentForsendelse(String forsendelseId) {
		assertNotBlank("ForsendelseId", forsendelseId);
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

	private static boolean isFerdigstiltForsendelseStatus(ForsendelseStatus forsendelseStatus) {
		assertNotBlank("ForsendelseStatus", forsendelseStatus.name());
		List<ForsendelseStatus> forsendelseStatuses = Arrays.asList(EKSPEDERT, RETURPOSTBEHANDLET, FEILET);
		return forsendelseStatuses.contains(forsendelseStatus);
	}

	public static boolean isOversendtOrBekreftet(ForsendelseStatus status) {
		return OVERSENDT.equals(status) || BEKREFTET.equals(status);
	}

	private static void assertNotBlank(String feltnavn, String value) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(String.format("%s kan ikke være null", feltnavn));
		}
	}

	private static void assertNotNull(String feltnavn, Object object) {
		if (object == null) {
			throw new IllegalArgumentException(String.format("%s kan ikke være null", feltnavn));
		}
	}
}
