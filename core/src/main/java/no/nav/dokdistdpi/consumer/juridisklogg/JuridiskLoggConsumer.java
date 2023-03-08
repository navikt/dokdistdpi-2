package no.nav.dokdistdpi.consumer.juridisklogg;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.exception.functional.LagreJuridiskLoggFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.LagreJuridiskLoggTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALLID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class JuridiskLoggConsumer {
	private final String juridiskLoggUrl;
	private final RestTemplate restTemplate;

	public JuridiskLoggConsumer(@Value("${juridisklogg.url}") String juridiskLoggUrl,
								RestTemplateBuilder restTemplateBuilder,
								final ServiceuserProperties serviceuserProperties) {
		this.juridiskLoggUrl = juridiskLoggUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(ofSeconds(20))
				.setConnectTimeout(ofSeconds(5))
				.basicAuthentication(serviceuserProperties.getUsername(), serviceuserProperties.getPassword())
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "lagreJuridiskLogg"}, histogram = true)
	public LoggMeldingResponse lagreJuridiskLogg(final LoggMeldingRequest loggMeldingRequest) {
		try {
			HttpEntity<LoggMeldingRequest> meldingRequestHttpEntity = new HttpEntity<>(loggMeldingRequest, createHeaders());
			return restTemplate.exchange(this.juridiskLoggUrl, POST, meldingRequestHttpEntity, LoggMeldingResponse.class).getBody();
		} catch (HttpClientErrorException e) {
			throw new LagreJuridiskLoggFunctionalException(format("lagreJuridiskLogg feilet funksjonelt med statusKode=%s. Feilmelding=%s",
					e.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new LagreJuridiskLoggTechnicalException(format("lagreJuridiskLogg feilet teknisk med statusKode=%s. Feilmelding=%s", e
					.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(NAV_CALLID, MDC.get(NAV_CALLID));
		return headers;
	}
}
