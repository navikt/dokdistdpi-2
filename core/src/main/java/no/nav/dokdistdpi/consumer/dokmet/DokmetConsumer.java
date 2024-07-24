package no.nav.dokdistdpi.consumer.dokmet;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DokumenttypeInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DokumenttypeInfoMapper;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfoMapper;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;
import no.nav.dokmet.api.tkat021.VarselInfoTo;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokmetConsumer {

	private static final String VARSELTYPE_INFO_URL = "/rest/varselinfo/{varselTypeId}";
	private static final String DOKUMENTTYPE_INFO_URL = "/rest/dokumenttypeinfo/{dokumenttypeId}";
	private final WebClient webClient;

	@Autowired
	public DokmetConsumer(DokdistdpiProperties dokdistdpiProperties,
						  WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistdpiProperties.getEndpoints().getDokmetUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.defaultHeader(NAV_CONSUMER_ID, APP_NAME)
				.build();
	}

	@Cacheable(value = TKAT020_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumenttypeInfo hentDokumenttypeInfo(final String dokumenttypeId) {

		return webClient.get()
				.uri(DOKUMENTTYPE_INFO_URL, dokumenttypeId)
				.headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
				.retrieve()
				.bodyToMono(DokumenttypeInfoTo.class)
				.mapNotNull(DokumenttypeInfoMapper::mapDokumenttypeInfoTo)
				.doOnError(this::handleErrorForTkat020)
				.block();
	}

	@Cacheable(TKAT021_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public VarselInfo getVarselInfo(String varselTypeId) {
		return webClient.get()
				.uri(VARSELTYPE_INFO_URL, varselTypeId)
				.headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
				.retrieve()
				.bodyToMono(VarselInfoTo.class)
				.mapNotNull(VarselInfoMapper::mapVarselInfo)
				.doOnError(exception -> handleErrorForTkat021(exception, varselTypeId))
				.block();
	}

	private void handleErrorForTkat020(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new DokmetTechnicalException(feilmelding, error);
		}
		String feilmelding = format("Kall mot dokmet feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(feilmelding, error);
		} else {
			throw new DokmetTechnicalException(feilmelding, error);
		}
	}

	private void handleErrorForTkat021(Throwable error, String varselTypeId) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk for varselTypeId=%s med feilmelding=%s", varselTypeId, error.getMessage());

			log.warn(feilmelding);

			throw new DokmetTechnicalException(feilmelding, error);
		}
		String feilmelding = format("Kall mot TKAT021 feilet %s med status=%s for varseltypeId=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				varselTypeId,
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());
		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(feilmelding, error);
		} else {
			throw new DokmetTechnicalException(feilmelding, error);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
