package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.exception.functional.KanIkkeDistribuereFunctinalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.KanIkkeDistribuereForsendelseException;
import no.nav.dokdistdpi.exception.technical.KunneIkkeHentKvitteringException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.KANAL;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@Slf4j
@Component
public class DpiClient {

	private static final String LOG_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status={}, bestillingsId={} og feilmelding={}";
	private static final String EXCEPTION_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status=%s, bestillingsId=%s og feilmelding=%s";
	private static final String KVITTERING_FEIL_MELDING = "Feilet til å markere kvitteringen med bestillingId=%s som mottatt, feilmelding=%s";
	private static final String SEND_PATH = "/out";
	private static final String HENT_PATH = "/in";
	private static final String PAGE_SIZE = "page_size";
	private static final String PAGE = "page";
	private static final String READ = "/read";

	private final RestTemplate restTemplate;
	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final DpiClientProperties clientProperties;

	public DpiClient(RestTemplateBuilder restTemplateBuilder, MaskinportenTokenConsumer maskinportenTokenConsumer, DpiClientProperties clientProperties) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.clientProperties = clientProperties;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(ofSeconds(15))
				.setReadTimeout(ofSeconds(30))
				.build();
	}

	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "dpiSendClient"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HttpStatus sendDpiForsendelse(MultipartBodyBuilder multipartBodyBuilder, Forsendelse forsendelse) {

		String uri = UriComponentsBuilder.fromHttpUrl(clientProperties.getUrl())
				.path(SEND_PATH)
				.queryParam(KANAL, clientProperties.getMpckanal())
				.toUriString();

		HttpEntity<?> httpEntity = new HttpEntity<>(multipartBodyBuilder.build(), headers(forsendelse.getDigital().getMaskinportentoken(), MULTIPART_FORM_DATA));

		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, POST, httpEntity, String.class);

			if (!CREATED.equals(response.getStatusCode())) {
				log.error(LOG_FEIL_MELDING, response.getStatusCode(), forsendelse.getBestillingsId(), response.getBody());
				throw new KanIkkeDistribuereForsendelseException(format(EXCEPTION_FEIL_MELDING, response.getStatusCode(), forsendelse.getBestillingsId(), response.getBody()));
			}
			log.info("Brev sendt til DPI hjørne-2 med bestillingsId={}, status={}", forsendelse.getBestillingsId(), response.getStatusCode());
			return response.getStatusCode();
		} catch (HttpClientErrorException e) {
			log.error(LOG_FEIL_MELDING, e.getStatusCode(), forsendelse.getBestillingsId(), e.getMessage());
			throw new KanIkkeDistribuereFunctinalException(format(EXCEPTION_FEIL_MELDING, e.getStatusCode(), forsendelse.getBestillingsId(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error(LOG_FEIL_MELDING, e.getStatusCode(), forsendelse.getBestillingsId(), e.getMessage());
			throw new KanIkkeDistribuereForsendelseException(format(EXCEPTION_FEIL_MELDING, e.getStatusCode(), forsendelse.getBestillingsId(), e.getMessage()), e);
		}
	}

	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "hentKvittering"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public ResponseEntity<HentKvitteringResponse[]> hentKvittering() {

		String uri = UriComponentsBuilder
				.fromHttpUrl(clientProperties.getUrl())
				.path(HENT_PATH)
				.queryParam(KANAL, clientProperties.getMpckanal())
				.queryParam(PAGE_SIZE, clientProperties.getPagesize())
				.queryParam(PAGE, clientProperties.getPage())
				.toUriString();

		HttpHeaders headers = headers(maskinportenTokenConsumer.fetchToken().getAccessToken(), APPLICATION_JSON);

		try {

			ResponseEntity<HentKvitteringResponse[]> response = restTemplate.exchange(uri, GET, new HttpEntity<>(headers), HentKvitteringResponse[].class);
			return response;

		} catch (HttpClientErrorException e) {
			throw new KunneIkkeHentKvitteringException(format("Feilet til å hente kvitteringer med feilmelding=%s", e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new KunneIkkeHentKvitteringException(format("Feilet til å hente kvitteringer med feilmelding=%s", e.getMessage()), e);
		}
	}

	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "bekreft"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HttpStatus bekreft(String bestllingId) {

		String uri = UriComponentsBuilder.fromHttpUrl(clientProperties.getUrl())
				.path(HENT_PATH + "/")
				.path(bestllingId).path(READ).toUriString();
		OidcTokenResponse oidcTokenResponse = maskinportenTokenConsumer.fetchToken();
		HttpHeaders headers = headers(oidcTokenResponse.getAccessToken(), APPLICATION_JSON);

		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, POST, new HttpEntity<>(headers), String.class);
			if (!OK.equals(response.getStatusCode())) {
				throw new KunneIkkeHentKvitteringException(format("Feilet til å markere kvitteringen med bestillingId=%s og status={} som mottatt", bestllingId, response.getStatusCode()));
			}
			log.info("Kvitteringen med bestillingId={} og status={} bekreftet mottatt", bestllingId, response.getStatusCode());
			return response.getStatusCode();
		} catch (HttpClientErrorException e) {
			log.warn(format("Feilet til å markere kvitteringen med bestillingId=%s og feilmelding=%s som mottatt", bestllingId, e.getMessage()), e);
			throw new KunneIkkeHentKvitteringException(format(KVITTERING_FEIL_MELDING, bestllingId, e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.warn(format("Feilet til å markere kvitteringen med bestillingId={} som mottatt", bestllingId), e);
			throw new KunneIkkeHentKvitteringException(format(KVITTERING_FEIL_MELDING, bestllingId, e.getMessage()), e);
		}
	}

	private HttpHeaders headers(final String maskinportentoken, MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setBearerAuth(maskinportentoken);
		return headers;
	}
}
