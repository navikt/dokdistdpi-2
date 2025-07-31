package no.nav.dokdistdpi.consumer.saf.graphql;

import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJsonJournalpost;
import no.nav.dokdistdpi.consumer.sts.StsRestConsumer;
import no.nav.dokdistdpi.exception.functional.SafJournalpostIkkeFunnetException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryUnauthorizedException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class SafGraphqlConsumer {

	private final RestClient restClient;
	private final StsRestConsumer stsRestConsumer;
	private final DokdistdpiProperties.Endpoints endpoints;

	public SafGraphqlConsumer(RestClient.Builder restClientBuilder,
							  DokdistdpiProperties dokdistdpiProperties,
							  StsRestConsumer stsRestConsumer) {
		this.endpoints = dokdistdpiProperties.getEndpoints();
		this.restClient = restClientBuilder
				.baseUrl(endpoints.getSaf().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
		this.stsRestConsumer = stsRestConsumer;
	}

	@Retryable(retryFor = SafJournalpostQueryTechnicalException.class)
	public SafJournalpostResponse performQuery(GraphQLRequest graphQLRequest) {
		try {
			SafJsonJournalpost responseEntity = restClient.post()
					.uri("/graphql")
					.headers(httpHeaders -> httpHeaders.setBearerAuth(stsRestConsumer.getStsOidcToken()))
					.body(graphQLRequest)
					.retrieve()
					.body(SafJsonJournalpost.class);


			if (isNull(responseEntity) && isNull(responseEntity.getData()) &&
					isNull(responseEntity.getData().getJournalpost())) {
				// Forsøk på nytt. GraphQL endepunktet gir kun httpstatus 200. Verdikjeden forventer at man finner journalpost her.
				// Hvis ikke er dette en teknisk feil, ikke funksjonell feil.
				throw new SafJournalpostIkkeFunnetException("Ingen journalpost ble funnet i saf.");
			}
			return responseEntity.getJournalpost();
		} catch (HttpClientErrorException e) {
			throw new SafJournalpostQueryUnauthorizedException(format("Henting av journalpost feilet med status: %s, feilmelding: %s", e
					.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new SafJournalpostQueryTechnicalException(format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", e.getStatusCode(), e.getMessage()), e);
		}
	}
}
