package no.nav.dokdistdpi.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.TokenConsumer;
import no.nav.dokdistdpi.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.DigitalKontaktinformasjonTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALLID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinfoMapper digitalPostKontaktinfoMapper;
	private final TokenConsumer tokenConsumer;
	private final RestTemplate restTemplate;
	private final String dkiUrl;
	private final String dkiScope;
	private static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	public DigitalKontaktinformasjonConsumer(@Value("${digdir_krr_proxy_url}") String dkiUrl,
											 @Value("${digdir_krr_proxy_scope}") String dkiScope,
											 TokenConsumer tokenConsumer,
											 RestTemplateBuilder restTemplateBuilder) {
		this.digitalPostKontaktinfoMapper = new DigitalKontaktinfoMapper();
		this.tokenConsumer = tokenConsumer;
		this.dkiUrl = dkiUrl;
		this.dkiScope = dkiScope;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20))
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = "dok_consumer", extraTags = {"process", "hentSikkerDigitalPostadresse"}, percentiles = {0.5, 0.95}, histogram = true)
	public SikkerDigitalKontaktInfo hentSikkerDigitalPostadresse(final String personidentifikator) {
		HttpHeaders headers = createHeaders();
		final String fnrTrimmed = personidentifikator.strip();

		try {
			PostPersonerRequest postPersonRequest = PostPersonerRequest.builder().personidenter(Arrays.asList(fnrTrimmed)).build();
			HttpEntity<String> request = new HttpEntity(postPersonRequest, headers);
			DigitalKontaktInfoResponse response = restTemplate.postForEntity(dkiUrl + "/rest/v1/personer?inkluderSikkerDigitalPost=true", request, DigitalKontaktInfoResponse.class).getBody();

			if (isValidRespons(response, fnrTrimmed)) {
				return digitalPostKontaktinfoMapper.mapDigitalKontaktinfo(response.getPersoner().get(fnrTrimmed), personidentifikator);
			} else {
				throw new DigitalKontaktinformasjonFunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s",
						getErrorMsg(response, fnrTrimmed)));
			}

		} catch (HttpClientErrorException e) {
			throw new DigitalKontaktinformasjonFunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DigitalKontaktinformasjonTechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private boolean isValidRespons(DigitalKontaktInfoResponse response, String fnr) {
		return response != null && response.getPersoner() != null && response.getPersoner().get(fnr) != null;
	}

	private String getErrorMsg(DigitalKontaktInfoResponse response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr);
		}
	}

	private HttpHeaders createHeaders() {
		String clientCredentialToken = tokenConsumer.getClientCredentialToken(dkiScope);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + clientCredentialToken);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(NAV_CALLID));
		return headers;
	}
}
