package no.nav.dokdistdpi.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.TokenConsumer;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.exception.functional.DigitalKontaktinformasjonFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.DigitalKontaktinformasjonTechnicalException;
import org.slf4j.MDC;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinfoMapper digitalPostKontaktinfoMapper;
	private final TokenConsumer tokenConsumer;
	private final RestTemplate restTemplate;
	private final DokdistdpiProperties.AppEndpoint digdirEndpoint;

	public DigitalKontaktinformasjonConsumer(DokdistdpiProperties dokdistdpiProperties,
											 TokenConsumer tokenConsumer,
											 RestTemplateBuilder restTemplateBuilder) {
		this.digitalPostKontaktinfoMapper = new DigitalKontaktinfoMapper();
		this.digdirEndpoint = dokdistdpiProperties.getEndpoints().getDigdir();
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplateBuilder.build();
	}

	@Retryable(includes = AbstractDokdistdpiTechnicalException.class)
	public SikkerDigitalKontaktInfo hentSikkerDigitalPostadresse(final String personidentifikator) {
		HttpHeaders headers = createHeaders();
		final String fnrTrimmed = personidentifikator.strip();

		try {
			PostPersonerRequest postPersonRequest = PostPersonerRequest.builder().personidenter(List.of(fnrTrimmed)).build();
			var request = new HttpEntity<>(postPersonRequest, headers);
			DigitalKontaktInfoResponse response = restTemplate.postForEntity(digdirEndpoint.getUrl() + "/rest/v1/personer?inkluderSikkerDigitalPost=true", request, DigitalKontaktInfoResponse.class).getBody();

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
		String clientCredentialToken = tokenConsumer.getClientCredentialToken(digdirEndpoint.getScope());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(clientCredentialToken);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
