package no.nav.dokdistdpi.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.sts.StsRestConsumer;
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

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BEARER_PREFIX;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_PERSONIDENTER;
import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinfoMapper digitalPostKontaktinfoMapper;
	private final StsRestConsumer stsRestConsumer;
	private final RestTemplate restTemplate;
	private final String dkiUrl;

	@Autowired
	public DigitalKontaktinformasjonConsumer(@Value("${dki_api_url}") String dkiUrl,
											 StsRestConsumer stsRestConsumer,
											 RestTemplateBuilder restTemplateBuilder) {
		this.digitalPostKontaktinfoMapper = new DigitalKontaktinfoMapper();
		this.stsRestConsumer = stsRestConsumer;
		this.dkiUrl = dkiUrl;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20))
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = "dok_consumer", extraTags = {"process", "hentSikkerDigitalPostadresse"}, percentiles = {0.5, 0.95}, histogram = true)
	public SikkerDigitalKontaktInfo hentSikkerDigitalPostadresse(final String personidentifikator) {
		HttpHeaders headers = createHeaders();
		final String fnrStriped = personidentifikator.strip();
		headers.add(NAV_PERSONIDENTER, fnrStriped);

		try {
			DigitalKontaktInfoResponse response = restTemplate.exchange(dkiUrl + "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=true",
					GET, new HttpEntity<>(headers), DigitalKontaktInfoResponse.class).getBody();

			if (isValidRespons(response, fnrStriped)) {
				return digitalPostKontaktinfoMapper.mapDigitalKontaktinfo(response.getKontaktinfo().get(fnrStriped), personidentifikator);
			} else {
				throw new DigitalKontaktinformasjonFunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s",
						getErrorMsg(response, fnrStriped)));
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
		return response != null && response.getKontaktinfo() != null && response.getKontaktinfo().get(fnr) != null;
	}

	private String getErrorMsg(DigitalKontaktInfoResponse response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr).getMelding();
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getStsOidcToken());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
