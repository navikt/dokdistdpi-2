package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.ServiceRegistryProperties;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.exception.technical.MaskinportenTechnicalException;
import no.nav.dokdistdpi.exception.technical.ServiceRegistryTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BEARER_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Component
public class ServiceRegistryConsumer {
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Teknisk feil: ";
	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente mottakerInfo fra service registry. Funksjonell feil: ";

	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final RestTemplate restTemplate;
	private final String baseUrl;

	public ServiceRegistryConsumer(ServiceRegistryProperties serviceRegistryProperties,
								   MaskinportenTokenConsumer maskinportenTokenConsumer,
								   RestTemplateBuilder restTemplateBuilder) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.baseUrl = serviceRegistryProperties.getUrl().toString();
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(30))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Retryable(value = {MaskinportenTechnicalException.class, ServiceRegistryTechnicalException.class}, backoff = @Backoff(delay = 5000))
	@Monitor(value = "dok_consumer", extraTags = {"process", "serviceregistry"}, percentiles = {0.5, 0.95}, histogram = true)
	public IdentifierResource getIdentifierResource(final String orgnummer, final String serviceProcess) {
		final String accessToken = maskinportenTokenConsumer.fetchToken().getAccessToken();
		URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
				.pathSegment("identifier/" + orgnummer + "/process/" + serviceProcess)
				.build().toUri();

		try {
			final ResponseEntity<IdentifierResource> response = restTemplate.exchange(uri, GET, httpEntity(accessToken), IdentifierResource.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.warn(FUNKSJONELL_FEIL_ERROR_MESSAGE + e.getResponseBodyAsString(), e);
			return IdentifierResource.empty();
		} catch (HttpServerErrorException e) {
			log.error(TEKNISK_FEIL_ERROR_MESSAGE + e.getResponseBodyAsString(), e);
			throw new ServiceRegistryTechnicalException(TEKNISK_FEIL_ERROR_MESSAGE + e.getResponseBodyAsString(), e);
		}
	}

	private HttpEntity<?> httpEntity(final String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.put(AUTHORIZATION, Collections.singletonList(BEARER_PREFIX + accessToken));
		return new HttpEntity<>(headers);
	}
}
