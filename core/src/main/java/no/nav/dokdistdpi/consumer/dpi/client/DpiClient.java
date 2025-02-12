package no.nav.dokdistdpi.consumer.dpi.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusIkkeFunnetException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDistribuereForsendelseException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeHenteKvitteringException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import no.nav.dokdistdpi.exception.technical.UkjentTekniskFeilException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.config.OAuthEnabledWebClientConfig.MASKINPORTEN_CLIENT_REGISTRATION;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest;
import static org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized;

/**
 * Implementasjon av https://docs.digdir.no/dpi_nyinfrastruktur.html#rest-api-mellom-avsender-og-hj%C3%B8rne-2
 */
@Slf4j
@Component
public class DpiClient {

	private static final String LOG_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status={}, konversasjonId={} og feilmelding={}";
	private static final String EXCEPTION_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status=%s, konversasjonId=%s og feilmelding=%s";
	private static final String SEND_PATH = "/out";
	private static final String MESSAGES_PATH_IN = "in";
	private static final String MESSAGES_PATH_OUT = "out";
	private static final String MESSAGES_PATH_IN_READ = "read";
	private static final String MESSAGES_PATH_OUT_STATUSES = "statuses";
	private static final String QUERY_PARAM_KANAL = "kanal";
	private static final String QUERY_PARAM_PAGE = "page";
	private static final String QUERY_PARAM_PAGESIZE = "page_size";
	// Siden hjørne2 ikke har et veldefinert felt som indikerer duplikate forsendelser så matches det på meldingen under.
	// Ved feil her så sjekk med Digdir og om dette er endret hos hjørne2 leverandør.
	private static final String HJORNE2_DUPLICATE_ERROR_MESSAGE = "ERROR: duplicate key value violates unique constraint";
	public static final String HJORNE2_FINGERAVTRYKK_ERROR_MESSAGE = "Upload was not accepted, SHA-256 digest of dokumentpakke was";
	private static final String RESILIENCE4J_INSTANCE = "dpi";

	private final RestTemplate restTemplate;
	private final WebClient oauth2WebClient;
	private final Retry retry;
	private final CircuitBreaker circuitBreaker;
	private final DpiClientProperties clientProperties;

	public DpiClient(WebClient oauth2WebClient,
					 RestTemplateBuilder restTemplateBuilder,
					 DpiClientProperties clientProperties,
					 CircuitBreakerRegistry circuitBreakerRegistry,
					 RetryRegistry retryRegistry) {
		this.clientProperties = clientProperties;
		this.restTemplate = restTemplateBuilder
				.connectTimeout(ofSeconds(15))
				.readTimeout(ofSeconds(30))
				.build();
		this.oauth2WebClient = oauth2WebClient.mutate()
				.baseUrl(clientProperties.getUrl())
				.build();
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE);
		this.retry = retryRegistry.retry(RESILIENCE4J_INSTANCE);
	}

	// https://docs.digdir.no/resources/begrep/sikkerDigitalPost/nyinf/api/openapi_spec.html#/paths/~1messages~1out/post
	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public List<ForsendelseStatusResponse> sendDpiForsendelse(MultipartBodyBuilder multipartBodyBuilder, Forsendelse forsendelse) {

		String uri = UriComponentsBuilder.fromHttpUrl(clientProperties.getUrl())
				.path(SEND_PATH)
				.queryParam(QUERY_PARAM_KANAL, clientProperties.getMpckanal())
				.toUriString();

		String konversasjonId = forsendelse.getKonversasjonId();

		HttpEntity<?> httpEntity = new HttpEntity<>(multipartBodyBuilder.build(), headers(forsendelse.getDigital().getMaskinportentoken(), MULTIPART_FORM_DATA));

		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, POST, httpEntity, String.class);

			if (!CREATED.equals(response.getStatusCode())) {
				log.error(LOG_FEIL_MELDING, response.getStatusCode(), konversasjonId, response.getBody());
				throw new KunneIkkeDistribuereForsendelseException(format(EXCEPTION_FEIL_MELDING, response.getStatusCode(), konversasjonId, response.getBody()));
			}
			log.info("Brev sendt til DPI hjørne2 med konversasjonId={}, status={}", konversasjonId, response.getStatusCode());

			return hentForsendelseStatus(konversasjonId);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == BAD_REQUEST && e.getMessage() != null && e.getMessage().contains(HJORNE2_DUPLICATE_ERROR_MESSAGE)) {
				log.info("Brev sendt til DPI hjørne2 tidligere, fortsetter behandling. Dette kallet ble avvist på duplikatkontroll hos hjørne2. konversasjonId={}, status={}, melding={}",
						konversasjonId, e.getStatusCode(), e.getMessage());
				return hentForsendelseStatus(konversasjonId);
			} else if (e.getStatusCode() == BAD_REQUEST && e.getMessage().contains(HJORNE2_FINGERAVTRYKK_ERROR_MESSAGE)) {
				// Trigger retry og legger på BQ i stedet
				throw new SikkerDigitalPostException(format(EXCEPTION_FEIL_MELDING, e.getStatusCode(), konversasjonId, e.getMessage()), e);
			}

			log.error(LOG_FEIL_MELDING, e.getStatusCode(), konversasjonId, e.getMessage());
			throw new KunneIkkeDistribuereForsendelseException(format(EXCEPTION_FEIL_MELDING, e.getStatusCode(), konversasjonId, e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error(LOG_FEIL_MELDING, e.getStatusCode(), konversasjonId, e.getMessage());
			throw new SikkerDigitalPostException(format(EXCEPTION_FEIL_MELDING, e.getStatusCode(), konversasjonId, e.getMessage()), e);
		}
	}

	// https://docs.digdir.no/resources/begrep/sikkerDigitalPost/nyinf/api/openapi_spec.html#/paths/~1messages~1out~1{id}~1statuses/get
	public List<ForsendelseStatusResponse> hentForsendelseStatus(String konversasjonId) {
		return oauth2WebClient.get()
				.uri(uriBuilder -> uriBuilder
						.pathSegment(MESSAGES_PATH_OUT, "{konversasjonId}", MESSAGES_PATH_OUT_STATUSES)
						.build(konversasjonId))
				.accept(APPLICATION_JSON, APPLICATION_PROBLEM_JSON)
				.attributes(clientRegistrationId(MASKINPORTEN_CLIENT_REGISTRATION))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<ForsendelseStatusResponse>>() {
				})
				.doOnError(handleForsendelseStatuserErrors(konversasjonId))
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.block();
	}

	private Consumer<Throwable> handleForsendelseStatuserErrors(String konversasjonId) {
		return error -> {
			if (error instanceof WebClientResponseException webException) {
				if (webException.getStatusCode().is4xxClientError()) {
					throw new ForsendelseStatusIkkeFunnetException(format("Finner ikke forsendelse status med konversasjonId=%s hos hjørne2. feilmelding=%s",
							konversasjonId, webException.getMessage()), webException);
				} else {
					throw new SikkerDigitalPostException(format("Feilet å hente forsendelse status med konversasjonId=%s hos hjørne2. status=%s, feilmelding=%s",
							konversasjonId, webException.getStatusCode(), webException.getMessage()), webException);
				}
			} else {
				throw new SikkerDigitalPostException(format("Feilet å hente forsendelse status. Ukjent teknisk feil. feilmelding=%s",
						error.getMessage()), error);
			}
		};
	}

	// https://docs.digdir.no/resources/begrep/sikkerDigitalPost/nyinf/api/openapi_spec.html#/paths/~1messages~1in/get
	public Flux<HentKvitteringResponse> hentKvitteringerAsync(final int page) {
		return oauth2WebClient.get()
				.uri(uriBuilder -> uriBuilder
						.pathSegment(MESSAGES_PATH_IN)
						.queryParam(QUERY_PARAM_KANAL, clientProperties.getMpckanal())
						.queryParam(QUERY_PARAM_PAGESIZE, clientProperties.getPagesize())
						.queryParam(QUERY_PARAM_PAGE, page)
						.build())
				.accept(APPLICATION_JSON, APPLICATION_PROBLEM_JSON)
				.attributes(clientRegistrationId(MASKINPORTEN_CLIENT_REGISTRATION))
				.retrieve()
				.bodyToFlux(HentKvitteringResponse.class)
				.doOnError(handleHentKvitteringerErrors())
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.onErrorResume(throwable -> {
					log.error(throwable.getMessage(), throwable);
					return Flux.empty();
				});
	}

	private Consumer<Throwable> handleHentKvitteringerErrors() {
		return error -> {
			if (error instanceof WebClientResponseException webException) {
				ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);
				if (webException instanceof BadRequest || webException instanceof Unauthorized) {
					throw new KunneIkkeHenteKvitteringException("Klarte ikke hente kvitteringer. problem=" + problemDetail);
				} else {
					// Retry hvis NotFound
					throw new SikkerDigitalPostException("Klarte ikke hente kvitteringer. problem=" + problemDetail);
				}
			} else {
				throw new UkjentTekniskFeilException("Henting av kvitteringer feilet med ukjent teknisk feil. Se stacktrace", error);
			}
		};
	}

	// https://docs.digdir.no/resources/begrep/sikkerDigitalPost/nyinf/api/openapi_spec.html#/paths/~1messages~1in~1{id}~1read/post
	public Mono<Void> markerKvitteringMottattAsync(String konversasjonId) {
		return oauth2WebClient.post()
				.uri(uriBuilder -> uriBuilder.pathSegment(MESSAGES_PATH_IN, "{konversasjonId}", MESSAGES_PATH_IN_READ).build(konversasjonId))
				.attributes(clientRegistrationId(MASKINPORTEN_CLIENT_REGISTRATION))
				.retrieve()
				.bodyToMono(Void.class)
				.doOnError(handleMarkerKvitteringMottattErrors(konversasjonId))
				.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
				.transformDeferred(RetryOperator.of(retry))
				.onErrorResume(throwable -> {
					log.error(throwable.getMessage(), throwable);
					return Mono.empty();
				});
	}

	private Consumer<Throwable> handleMarkerKvitteringMottattErrors(String konversasjonId) {
		return error -> {
			if (error instanceof WebClientResponseException webException) {
				ProblemDetail problemDetail = webException.getResponseBodyAs(ProblemDetail.class);
				if (webException instanceof BadRequest || webException instanceof Unauthorized) {
					throw new KunneIkkeHenteKvitteringException("Klarte ikke markere kvittering med konversasjonId=" + konversasjonId + " som mottatt. problem=" + problemDetail);
				} else {
					// Retry hvis NotFound
					throw new SikkerDigitalPostException("Klarte ikke markere kvittering med konversasjonId=" + konversasjonId + " som mottatt. problem=" + problemDetail);
				}
			} else {
				throw new UkjentTekniskFeilException("Ukjent teknisk feil. Klarte ikke å markere kvitteringen med konversasjonId=%s som mottatt. Se stacktrace", error);
			}
		};
	}

	private HttpHeaders headers(final String maskinportentoken, MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setBearerAuth(maskinportentoken);
		return headers;
	}
}
