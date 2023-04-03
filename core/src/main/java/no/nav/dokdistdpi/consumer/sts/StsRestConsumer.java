package no.nav.dokdistdpi.consumer.sts;

import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.StsTechnicalException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.config.cache.CacheConfig.STS_CACHE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALLID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class StsRestConsumer {

	private final RestTemplate restTemplate;
	private final String stsUrl;

	@Autowired
	public StsRestConsumer(@Value("${security-token-service.url}") String stsUrl,
						   final ServiceuserProperties serviceuserProperties,
						   RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder
				.basicAuthentication(serviceuserProperties.getUsername(), serviceuserProperties.getPassword())
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.stsUrl = stsUrl;
	}

	@Cacheable(STS_CACHE)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public String getStsOidcToken() {
		try {
			StsResponseTo stsResponse = restTemplate.exchange(stsUrl + "?grant_type=client_credentials&scope=openid", POST, httpEntity(), StsResponseTo.class).getBody();
			return requireNonNull(stsResponse.getAccessToken());
		} catch (HttpStatusCodeException e) {
			throw new StsTechnicalException(format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e
					.getMessage()), e);
		}
	}

	private HttpEntity<?> httpEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(NAV_CALL_ID, MDC.get(NAV_CALLID));
		return new HttpEntity<>(headers);
	}
}
