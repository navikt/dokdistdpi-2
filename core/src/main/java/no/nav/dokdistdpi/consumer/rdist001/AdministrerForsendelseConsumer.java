package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
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
import org.springframework.http.ResponseEntity;
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
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class AdministrerForsendelseConsumer {

	private final String url;
	private final RestTemplate restTemplate;

	@Autowired
	public AdministrerForsendelseConsumer(RestTemplateBuilder restTemplateBuilder, ServiceuserProperties serviceuser,
										  @Value("${administrerforsendelse.url}") String url) {
		this.url = url;
		this.restTemplate = restTemplateBuilder
				.basicAuthentication(serviceuser.getUsername(), serviceuser.getPassword())
				.setConnectTimeout(ofSeconds(5))
				.setReadTimeout(ofSeconds(20))
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "hentIdent"}, percentiles = {0.5, 0.95}, histogram = true)
	public HentForsendelseResponse hentMottaker(final String forsendelseId) {
		try {
			HttpEntity<?> httpEntity = new HttpEntity<>(createHeader());
			ResponseEntity<HentForsendelseResponse> response = restTemplate.exchange(url + "/" + forsendelseId, GET, httpEntity, HentForsendelseResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 feilet funksjonell med forsendelseId={}, feilmelding={}", forsendelseId, e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format("Kall mot rdist001 - hentForsendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 feilet teknisk med forsendelseId={}, feilmelding={}", forsendelseId, e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format("Kall mot rdist001 feilet teknisk med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
