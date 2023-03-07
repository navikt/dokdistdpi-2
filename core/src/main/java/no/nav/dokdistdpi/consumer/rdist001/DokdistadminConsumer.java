package no.nav.dokdistdpi.consumer.rdist001;

import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import no.nav.dokdistdpi.common.NavHeadersFilter;
import no.nav.dokdistdpi.config.WebClientAzureAuthentication;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;

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
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfoRequest) {
		webClient.put()
				.uri("/oppdatervarselinfo")
				.bodyValue(oppdaterVarselInfoRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if (error instanceof WebClientResponseException response && ((WebClientResponseException) error).getStatusCode().is4xxClientError()) {
			throw new AdminstrerForsendelseFunctionalException(
					String.format("Kall mot rdist001 feilet funksjonelt med status: %s, feilmelding: %s",
							response.getRawStatusCode(),
							response.getMessage()),
					error);
		} else {
			throw new AbstractDokdistdpiTechnicalException(
					String.format("Kall mot rdist001 feilet feilet teknisk med feilmelding: %s", error.getMessage()),
					error) {
			};
		}
	}
}
