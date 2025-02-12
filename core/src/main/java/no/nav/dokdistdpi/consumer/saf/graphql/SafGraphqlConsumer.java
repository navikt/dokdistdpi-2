package no.nav.dokdistdpi.consumer.saf.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJsonJournalpost;
import no.nav.dokdistdpi.consumer.sts.StsRestConsumer;
import no.nav.dokdistdpi.exception.functional.SafJournalpostIkkeFunnetException;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryUnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class SafGraphqlConsumer {

	private final RestTemplate restTemplate;
	private final String graphQLurl;
	private final StsRestConsumer stsConsumer;

	public SafGraphqlConsumer(RestTemplateBuilder restTemplateBuilder,
							  @Value("${saf.graphql.url}") String graphQLurl,
							  StsRestConsumer stsConsumer) {
		this.restTemplate = restTemplateBuilder
				.readTimeout(Duration.ofSeconds(20))
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.graphQLurl = graphQLurl;
		this.stsConsumer = stsConsumer;
	}

	@Retryable(retryFor = SafJournalpostQueryTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public SafJournalpostResponse performQuery(GraphQLRequest graphQLRequest) {
		try {
			ResponseEntity<SafJsonJournalpost> responseEntity = restTemplate.exchange(graphQLurl, HttpMethod.POST, new HttpEntity<>(requestToJson(graphQLRequest), createAuthorizationHeader()), SafJsonJournalpost.class);

			if (isNull(responseEntity.getBody()) && isNull(responseEntity.getBody().getData()) &&
				isNull(responseEntity.getBody().getData().getJournalpost())) {
				// Forsøk på nytt. GraphQL endepunktet gir kun httpstatus 200. Verdikjeden forventer at man finner journalpost her.
				// Hvis ikke er dette en teknisk feil, ikke funksjonell feil.
				throw new SafJournalpostIkkeFunnetException("Ingen journalpost ble funnet i saf.");
			}
			return responseEntity.getBody().getJournalpost();
		} catch (HttpClientErrorException e) {
			throw new SafJournalpostQueryUnauthorizedException(format("Henting av journalpost feilet med status: %s, feilmelding: %s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SafJournalpostQueryTechnicalException(format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createAuthorizationHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(stsConsumer.getStsOidcToken());
		return headers;
	}

	private String requestToJson(GraphQLRequest graphQLRequest) {
		try {
			return new ObjectMapper().writeValueAsString(graphQLRequest);
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException(format("Kunne ikke konvertere graphQlRequest til json, feilmelding=%s", e.getMessage()), e);
		}
	}
}
