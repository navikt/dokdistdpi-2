package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusIkkeFunnetException;
import no.nav.dokdistdpi.exception.functional.KunneIkkeDistribuereForsendelseException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.KunneIkkeHentKvitteringException;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.KANAL;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.PAGE_SIZE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@Slf4j
@Component
public class DpiClient {

	private static final String LOG_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status={}, konversasjonId={} og feilmelding={}";
	private static final String EXCEPTION_FEIL_MELDING = "Kunne ikke sende til DPI hjørne-2 med status=%s, konversasjonId=%s og feilmelding=%s";
	private static final String KVITTERING_FEIL_MELDING = "Feilet å markere kvitteringen med konversasjonId=%s som mottatt, feilmelding=%s";
	private static final String SEND_PATH = "/out";
	private static final String HENT_PATH = "/in";
	private static final String READ = "/read";
	private static final String STATUSES = "/statuses";
	// Siden hjørne2 ikke har et veldefinert felt som indikerer duplikate forsendelser så matches det på meldingen under.
	// Ved feil her så sjekk med Digdir og om dette er endret hos hjørne2 leverandør.
	private static final String HJORNE2_DUPLICATE_ERROR_MESSAGE = "ERROR: duplicate key value violates unique constraint";
	public static final String HJORNE2_FINGERAVTRYKK_ERROR_MESSAGE = "Upload was not accepted, SHA-256 digest of dokumentpakke was";

	private final RestTemplate restTemplate;
	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final DpiClientProperties clientProperties;

	@Autowired
	public DpiClient(RestTemplateBuilder restTemplateBuilder,
					 MaskinportenTokenConsumer maskinportenTokenConsumer, DpiClientProperties clientProperties) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.clientProperties = clientProperties;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(ofSeconds(15))
				.setReadTimeout(ofSeconds(30))
				.build();
	}

	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public List<ForsendelseStatusResponse> sendDpiForsendelse(MultipartBodyBuilder multipartBodyBuilder, Forsendelse forsendelse) {

		String uri = UriComponentsBuilder.fromHttpUrl(clientProperties.getUrl())
				.path(SEND_PATH)
				.queryParam(KANAL, clientProperties.getMpckanal())
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
			} else if(e.getStatusCode() == BAD_REQUEST && e.getMessage().contains(HJORNE2_FINGERAVTRYKK_ERROR_MESSAGE)) {
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

	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public List<ForsendelseStatusResponse> hentForsendelseStatus(String konversasjonId) {

		String uri = UriComponentsBuilder
				.fromHttpUrl(clientProperties.getUrl())
				.path(SEND_PATH + "/")
				.path(konversasjonId).path(STATUSES).toUriString();
		try {
			ResponseEntity<ForsendelseStatusResponse[]> forsendelseStatues = restTemplate.exchange(uri, GET, new HttpEntity<>(jsonTypeHeaders()), ForsendelseStatusResponse[].class);

			List<ForsendelseStatusResponse> forsendelseStatusResponses = mapForsendelseStatus(forsendelseStatues.getBody());
			log.info("Hentet status på forsendelse med konversasjonId={} og status={} hos hjørne2", konversasjonId, forsendelseStatusResponses);
			return forsendelseStatusResponses;
		} catch (HttpClientErrorException e) {
			throw new ForsendelseStatusIkkeFunnetException(format("Finner ikke forsendelse status med konversasjonId=%s hos hjørne2. Feilmelding=%s", konversasjonId, e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SikkerDigitalPostException("Hente forsendelse status feilet mot hjørne2.", e);
		}
	}

	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public ResponseEntity<HentKvitteringResponse[]> hentKvittering() {

		String uri = UriComponentsBuilder
				.fromHttpUrl(clientProperties.getUrl())
				.path(HENT_PATH)
				.queryParam(KANAL, clientProperties.getMpckanal())
				.queryParam(PAGE_SIZE, clientProperties.getPagesize())
				.toUriString();

		try {
			return restTemplate.exchange(uri, GET, new HttpEntity<>(jsonTypeHeaders()), HentKvitteringResponse[].class);
		} catch (HttpClientErrorException e) {
			throw new KunneIkkeHentKvitteringException(format("Feilet til å hente kvitteringer med feilmelding=%s", e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SikkerDigitalPostException(format("Feilet til å hente kvitteringer med feilmelding=%s", e.getMessage()), e);
		}
	}

	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HttpStatusCode bekreft(String konversasjonId) {

		String uri = UriComponentsBuilder.fromHttpUrl(clientProperties.getUrl())
				.path(HENT_PATH + "/")
				.path(konversasjonId).path(READ).toUriString();
		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, POST, new HttpEntity<>(jsonTypeHeaders()), String.class);
			if (!OK.equals(response.getStatusCode())) {
				throw new KunneIkkeHentKvitteringException(format("Feilet til å markere kvitteringen med konversasjonId=%s og status=%s som mottatt", konversasjonId, response.getStatusCode()));
			}
			log.info("Kvitteringen med konversasjonId={} og status={} bekreftet mottatt", konversasjonId, response.getStatusCode());
			return response.getStatusCode();
		} catch (HttpClientErrorException e) {
			log.warn(format("Feilet til å markere kvitteringen med konversasjonId=%s og feilmelding=%s som mottatt", konversasjonId, e.getMessage()), e);
			throw new KunneIkkeHentKvitteringException(format(KVITTERING_FEIL_MELDING, konversasjonId, e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.warn(format("Feilet til å markere kvitteringen med konversasjonId=%s som mottatt", konversasjonId), e);
			throw new SikkerDigitalPostException(format(KVITTERING_FEIL_MELDING, konversasjonId, e.getMessage()), e);
		}
	}

	private List<ForsendelseStatusResponse> mapForsendelseStatus(ForsendelseStatusResponse[] statusResponses) {
		return Arrays.stream(statusResponses)
				.filter(Objects::nonNull)
				.toList();
	}

	private HttpHeaders jsonTypeHeaders() {
		OidcTokenResponse oidcTokenResponse = maskinportenTokenConsumer.fetchToken();
		return headers(oidcTokenResponse.getAccessToken(), APPLICATION_JSON);
	}

	private HttpHeaders headers(final String maskinportentoken, MediaType mediaType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setBearerAuth(maskinportentoken);
		return headers;
	}
}
