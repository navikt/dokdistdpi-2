package no.nav.dokdistdpi.consumer.juridisklogg;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.exception.functional.LagreJuridiskLoggFunctionalException;
import no.nav.dokdistdpi.exception.technical.LagreJuridiskLoggTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class JuridiskLoggConsumer {

	private final RestClient restClient;

	public JuridiskLoggConsumer(DokdistdpiProperties dokdistdpiProperties,
								RestClient.Builder restClientBuilder,
								final ServiceuserProperties serviceuserProperties) {
		this.restClient = restClientBuilder
				.baseUrl(dokdistdpiProperties.getEndpoints().getJuridisklogg())
				.defaultHeaders(httpHeaders ->
						httpHeaders.setBasicAuth(serviceuserProperties.getUsername(), serviceuserProperties.getPassword()))
				.build();
	}

	@Retryable(retryFor = LagreJuridiskLoggTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public LoggMeldingResponse lagreJuridiskLogg(final LoggMeldingRequest loggMeldingRequest) {
		try {
			return restClient.post()
					.uri("/api/rest/logg")
					.contentType(APPLICATION_JSON)
					.body(loggMeldingRequest)
					.retrieve()
					.body(LoggMeldingResponse.class);
		} catch (HttpClientErrorException e) {
			throw new LagreJuridiskLoggFunctionalException(format("lagreJuridiskLogg feilet funksjonelt med status=%s. Feilmelding=%s",
					e.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new LagreJuridiskLoggTechnicalException(format("lagreJuridiskLogg feilet teknisk med status=%s. Feilmelding=%s",
					e.getStatusCode(), e.getResponseBodyAsString()), e);
		} catch (ResourceAccessException e) {
			// For å få retry ved følgende feil:
			// org.springframework.web.client.ResourceAccessException: I/O error on POST request for "https://app.adeo.no/juridisklogg/api/rest/logg": Connection reset
			throw new LagreJuridiskLoggTechnicalException(format("lagreJuridiskLogg feilet teknisk. Feilmelding=%s",
					e.getMessage()), e);
		}
	}
}
