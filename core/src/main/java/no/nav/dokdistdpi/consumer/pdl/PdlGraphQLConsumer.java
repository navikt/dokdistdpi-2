package no.nav.dokdistdpi.consumer.pdl;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.sts.StsRestConsumer;
import no.nav.dokdistdpi.exception.functional.PdlFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.PdlHentIdentTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.RequestEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BEARER_PREFIX;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_TOKEN;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class PdlGraphQLConsumer {

	private RestTemplate restTemplate;
	private final StsRestConsumer stsConsumer;
	private final String pdlUrl;
	private final HentIdentMapper hentIdentMapper;

	@Autowired
	public PdlGraphQLConsumer(RestTemplateBuilder restTemplateBuilder, StsRestConsumer stsConsumer,
							  @Value("${pdl.url}") String pdlUrl) {
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5L))
				.setReadTimeout(Duration.ofSeconds(15L))
				.build();
		this.stsConsumer = stsConsumer;
		this.pdlUrl = pdlUrl;
		this.hentIdentMapper = new HentIdentMapper();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "hentIdent"}, percentiles = {0.5, 0.95}, histogram = true)
	public String hentIdent(final String ident) {
		try {
			RequestEntity<PDLRequest> requestEntity = createRequestEntity()
					.body(mapRequest(ident));

			final HentIdentResponse response = requireNonNull(restTemplate.exchange(requestEntity, HentIdentResponse.class).getBody());
			if (nonNull(response.getErrors()) || response.getErrors().isEmpty()) {
				throw new PdlFunctionalException("Kunne ikke hente identer fra pdl med feilmelding={}" + response.getErrors());
			}
			return hentIdentMapper.map(response);
		} catch (HttpClientErrorException e) {
			throw new PdlFunctionalException("Kunne ikke hente person fra pdl.", e);
		} catch (HttpServerErrorException e) {
			throw new PdlHentIdentTechnicalException("Teknisk feil ved kall mot PDL.", e);
		}
	}

	private RequestEntity.BodyBuilder createRequestEntity() {
		final UriComponents uri = UriComponentsBuilder.fromHttpUrl(pdlUrl).build();
		final String serviceUserToken = BEARER_PREFIX + stsConsumer;
		return RequestEntity.post(uri.toUri())
				.accept(APPLICATION_JSON)
				.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.header(AUTHORIZATION, serviceUserToken)
				.header(NAV_CONSUMER_TOKEN, serviceUserToken)
				.header(NAV_CALL_ID, MDC.get(CALL_ID));
	}

	private PDLRequest mapRequest(final String ident) {
		final HashMap<String, Object> variables = new HashMap<>();
		variables.put("ident", ident);
		return PDLRequest.builder().query("query hentIdenter($ident: ID!) {\n" +
				"  hentIdenter(ident: $ident, grupper: FOLKEREGISTERIDENT, historikk: false){\n" +
				"      identer{\n" +
				"        ident\n" +
				"        gruppe\n" +
				"        historisk\n" +
				"      }\n" +
				"  }\n" +
				"}").variables(variables).build();
	}
}
