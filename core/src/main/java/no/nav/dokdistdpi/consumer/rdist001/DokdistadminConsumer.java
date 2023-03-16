package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import no.nav.dokdistdpi.common.NavHeadersFilter;
import no.nav.dokdistdpi.config.WebClientAzureAuthentication;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseResponseTo;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AdminstrerForsendelseTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class DokdistadminConsumer {

	private final WebClient webClient;

	public DokdistadminConsumer(AzureTokenConsumer azureTokenConsumer,
								DokdistdpiProperties dokdistdpiProperties,
								WebClient webClient) {
		this.webClient = webClient.mutate()
				.baseUrl(dokdistdpiProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new WebClientAzureAuthentication(azureTokenConsumer,
						dokdistdpiProperties.getEndpoints().getDokdistadmin().getScope()))
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfoRequest) {
		log.info("Mottatt kall til å oppdatere varselInfo. forsendelseId={}", oppdaterVarselInfoRequest.forsendelseId());
		webClient.put()
				.uri("/oppdatervarselinfo")
				.bodyValue(oppdaterVarselInfoRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "opprettForsendelse"}, histogram = true)
	public String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequest) {
		var bestillingsId = opprettForsendelseRequest.getBestillingsId();

		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", bestillingsId);

		var forsendelseId = webClient.post()
				.bodyValue(opprettForsendelseRequest)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponseTo.class)
				.doOnError(this::handleError)
				.map(response -> new Forsendelse(response.getForsendelseId()).getForsendelseId())
				.block();

		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);

		return forsendelseId;
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new AdminstrerForsendelseFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status: %s, feilmelding: %s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new AdminstrerForsendelseTechnicalException(
					String.format("Kall mot rdist001 feilet feilet teknisk med feilmelding: %s", error.getMessage()),
					error) {
			};
		}
	}
}
