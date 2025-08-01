package no.nav.dokdistdpi.consumer.saf.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJsonJournalpost;
import no.nav.dokdistdpi.exception.functional.SafJournalpostFunctionalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryUnauthorizedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.consumer.naistoken.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class SafGraphqlConsumer {

	private final RestClient restClientTexas;
	private final ObjectMapper objectMapper;
	private final DokdistdpiProperties.Endpoints endpoints;

	public SafGraphqlConsumer(RestClient restClientTexas,
							  ObjectMapper objectMapper,
							  DokdistdpiProperties dokdistdpiProperties) {
		this.endpoints = dokdistdpiProperties.getEndpoints();
		this.objectMapper = objectMapper;
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(endpoints.getSaf().getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = SafJournalpostQueryTechnicalException.class)
	public SafJournalpostResponse performQuery(GraphQLRequest graphQLRequest) {
		SafJsonJournalpost response = restClientTexas.post()
				.uri("/graphql")
				.attribute(TARGET_SCOPE, endpoints.getSaf().getScope())
				.body(graphQLRequest)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					ProblemDetail problemDetail = objectMapper.readValue(res.getBody(), ProblemDetail.class);
					if (res.getStatusCode().is5xxServerError()) {
						throw new SafJournalpostQueryTechnicalException(format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", problemDetail.getStatus(), problemDetail.getDetail()));
					}
					throw new SafJournalpostQueryUnauthorizedException(format("Henting av journalpost feilet med status: %s. Skyldes sannsynligvis at appen som gjorde kallet ikke har tilgang til SAF. " +
									"For å få tilgang må appen som kaller dokdistfordeling legges til i SAF sin <env-config.json>. Feilmelding: %s",
							problemDetail.getStatus(), problemDetail.getDetail()));

				})
				.body(SafJsonJournalpost.class);

		if (nonNull(response) && !isEmpty(response.getErrors())) {
			SafJsonJournalpost.Error safError = response.getErrors().getFirst();
			throw new SafJournalpostFunctionalException("Feil i saf query: " + safError.getMessage());
		}
		return response.getData().getJournalpost();
	}
}

