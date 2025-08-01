package no.nav.dokdistdpi.consumer.saf.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJournalpostResponse;
import no.nav.dokdistdpi.consumer.saf.journalpost.SafJsonJournalpost;
import no.nav.dokdistdpi.exception.functional.SafJournalpostFunctionalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryTechnicalException;
import no.nav.dokdistdpi.exception.technical.SafJournalpostQueryUnauthorizedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.consumer.naistoken.NaisTexasRequestInterceptor.TARGET_SCOPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
public class SafGraphqlConsumer {

	private static final String NOT_FOUND = "not_found";
	private static final String FORBIDDEN = "forbidden";
	private static final String SERVER_ERROR = "server_error";
	private static final String BAD_REQUEST = "bad_request";
	private static final String CLASSIFICATION_VALIDATIONERROR = "ValidationError";

	private final RestClient restClientTexas;
	private final ObjectMapper objectMapper;
	private final DokdistdpiProperties.AppEndpoint safEndpoint;

	public SafGraphqlConsumer(RestClient restClientTexas,
							  ObjectMapper objectMapper,
							  DokdistdpiProperties dokdistdpiProperties) {
		this.safEndpoint = dokdistdpiProperties.getEndpoints().getSaf();
		this.objectMapper = objectMapper;
		this.restClientTexas = restClientTexas.mutate()
				.baseUrl(safEndpoint.getUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(retryFor = SafJournalpostQueryTechnicalException.class)
	public SafJournalpostResponse performQuery(GraphQLRequest graphQLRequest) {
		SafJsonJournalpost response = restClientTexas.post()
				.uri("/graphql")
				.attribute(TARGET_SCOPE, safEndpoint.getScope())
				.body(graphQLRequest)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> handleError(res))
				.body(SafJsonJournalpost.class);

		if (nonNull(response) && !isEmpty(response.getErrors())) {
			SafJsonJournalpost.Error safError = response.getErrors().getFirst();
			if (safError.getExtensions().getClassification().contains(CLASSIFICATION_VALIDATIONERROR)) {
				throw new SafJournalpostQueryTechnicalException("Feil i saf query: " + safError.getMessage());
			}
			String safErrorCode = safError.getExtensions().getCode();

			switch (safErrorCode) {
				case NOT_FOUND ->
						throw new SafJournalpostQueryTechnicalException("Fant ikke journalposten i fagarkivet");
				case FORBIDDEN ->
						throw new SafJournalpostQueryUnauthorizedException("Saksbehandler har ikke tilgang til journalposten. Feilmelding fra SAF: " + safError.getMessage());
				case SERVER_ERROR -> {
					log.warn("Teknisk feil mot SAF. Feilmelding: " + safError.getMessage());
					throw new SafJournalpostQueryTechnicalException(safError.getMessage());
				}
				case BAD_REQUEST ->
						throw new SafJournalpostFunctionalException("Bad request mot SAF: " + safError.getMessage());
				default ->
						throw new SafJournalpostFunctionalException("Ukjent error code fra SAF. Håndtering av ny feilkode må legges inn her. Feilmelding: " + safError.getMessage());
			}
		}

		return response.getData().getJournalpost();
	}

	private void handleError(ClientHttpResponse res) throws IOException {
		ProblemDetail problemDetail = objectMapper.readValue(res.getBody(), ProblemDetail.class);
		if (res.getStatusCode().is5xxServerError()) {
			throw new SafJournalpostQueryTechnicalException(format("Tjenesten SAF (graphQL) feilet med status: %s, feilmelding: %s", problemDetail.getStatus(), problemDetail.getDetail()));
		}
		throw new SafJournalpostQueryUnauthorizedException(format("Kunne ikke hente journalpost med status: %s. Dette skyldes sannsynligvis at appen som utførte kallet mangler tilgang til SAF. " +
						"For å få tilgang må appen som kaller dokdistdpi legges til i SAF sin <env-config.json>. Feilmelding: %s",
				problemDetail.getStatus(), problemDetail.getDetail()));
	}
}

