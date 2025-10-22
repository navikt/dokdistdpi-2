package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import no.nav.dokdistdpi.common.NavHeadersFilter;
import no.nav.dokdistdpi.config.WebClientAzureAuthentication;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentUekspederteForsendelserResponse;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AdminstrerForsendelseTechnicalException;
import org.springframework.boot.autoconfigure.http.codec.HttpCodecsProperties;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClientRequest;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class HentUekspederteForsendelserConsumer {

	private final HentUekspederteForsendelserResponse EMPTY_UEKSPEDERTEFORSENDELSER = HentUekspederteForsendelserResponse.builder()
			.uekspederteForsendelser(emptyList())
			.build();

	private final WebClient webClient;

	public HentUekspederteForsendelserConsumer(AzureTokenConsumer azureTokenConsumer,
											   DokdistdpiProperties dokdistdpiProperties,
											   WebClient webClientLongResponseTimeout,
											   HttpCodecsProperties httpCodecsProperties) {
		this.webClient = webClientLongResponseTimeout.mutate()
				.baseUrl(dokdistdpiProperties.getEndpoints().getDokdistadmin().getUrl())
				.filter(new WebClientAzureAuthentication(azureTokenConsumer,
						dokdistdpiProperties.getEndpoints().getDokdistadmin().getScope()))
				.filter(new NavHeadersFilter())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
								.maxInMemorySize((int) httpCodecsProperties.getMaxInMemorySize().toBytes()))
						.build())
				.build();
	}

	@Retryable(retryFor = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HentUekspederteForsendelserResponse hentForsendelserKvitteringIkkeMottatt(String distribusjonskanal, int antallTimer) {
		log.info("hentForsendelserKvitteringIkkeMottatt henter uekspederte forsendelser med distribusjonskanal={}, antallTimer={}",
				distribusjonskanal, antallTimer);

		var response = webClient.get()
				.uri("/hentuekspederteforsendelser/{distribusjonkanal}/{antallTimer}", distribusjonskanal, antallTimer)
				.httpRequest(httpRequest -> {
					HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
					reactorRequest.responseTimeout(ofSeconds(180));
				})
				.retrieve()
				.bodyToMono(HentUekspederteForsendelserResponse.class)
				.defaultIfEmpty(EMPTY_UEKSPEDERTEFORSENDELSER) // Håndtering av HttpStatus NO_CONTENT (204)
				.doOnError(this::handleError)
				.block();

		log.info("hentForsendelserKvitteringIkkeMottatt har hentet {} uekspederte forsendelser med distribusjonskanal={}, antallTimer={}",
				response == null ? 0 : response.getUekspederteForsendelser().size(), distribusjonskanal, antallTimer);

		return response;
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new AdminstrerForsendelseTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot rdist001 feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new AdminstrerForsendelseFunctionalException(feilmelding, error);
		} else {
			throw new AdminstrerForsendelseTechnicalException(feilmelding, error);
		}
	}
}
