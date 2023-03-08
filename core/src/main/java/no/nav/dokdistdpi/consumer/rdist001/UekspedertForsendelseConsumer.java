package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.AvstemForsendelseResponseTo;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.AdminstrerForsendelseTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALLID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Egen administrerforsendelse konsument for å hente uekspederte forsendelser.
 * Da dette kallet kan ta litt tid og vi ikke ønsker at timeout er generelt høyt på ikke-batch kall.
 */
@Slf4j
@Component
public class UekspedertForsendelseConsumer {
	private static final String FORSENDELSE_ID = "forsendelseId";
	private final String url;
	private final RestTemplate restTemplate;

	@Autowired
	public UekspedertForsendelseConsumer(RestTemplateBuilder restTemplateBuilder, ServiceuserProperties serviceuser,
										 @Value("${administrerforsendelse.url}") String url) {
		this.url = url;
		this.restTemplate = restTemplateBuilder
				.basicAuthentication(serviceuser.getUsername(), serviceuser.getPassword())
				.setConnectTimeout(ofSeconds(5))
				.setReadTimeout(ofMinutes(10))
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "hentForsendelserKvitteringIkkeMottatt"}, histogram = true)
	public List<AvstemForsendelseResponseTo> hentForsendelserKvitteringIkkeMottatt(String distribusjonKanal, int antallTimer) {
		try {
			HttpHeaders httpHeaders = createHeaders();
			log.info("Mottatt kall om å hente uekspederte forsendelser med distribusjonKanal={}, antallTimer={}",
					distribusjonKanal, antallTimer);
			ResponseEntity<AvstemForsendelseResponseTo[]> responseEntity = restTemplate
					.exchange(format("%s/henteuekspederforsendelse/%s/%s", url, distribusjonKanal, antallTimer),
							HttpMethod.GET, new HttpEntity<>(httpHeaders), AvstemForsendelseResponseTo[].class);

			return responseEntity.getBody() == null ? Collections.emptyList() : List.of(responseEntity.getBody());
		} catch (HttpClientErrorException e) {
			log.warn("{} Kall mot rdist001 feilet med status={}, feilmelding={}", MDC.get(NAV_CONSUMER_ID), e.getStatusCode(), e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(
					format("Kall mot rdist001 feilet funksjonelt. status=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.warn("Kall mot rdist001 feilet teknisk. status={}, feilmelding={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new AdminstrerForsendelseTechnicalException(
					format("Kall mot rdist001 feilet teknisk. status=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(NAV_CALLID, MDC.get(NAV_CALLID));
		return headers;
	}
}
