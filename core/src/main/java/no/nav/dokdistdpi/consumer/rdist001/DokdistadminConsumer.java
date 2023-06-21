package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import no.nav.dokdistdpi.common.NavHeadersFilter;
import no.nav.dokdistdpi.config.WebClientAzureAuthentication;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilregistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseResponseTo;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AdminstrerForsendelseTechnicalException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
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

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {

		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.doOnError(this::handleError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public String finnForsendelse(final FinnForsendelseRequest finnForsendelseRequest) {
		var oppslagsnoekkel = finnForsendelseRequest.getOppslagsnoekkel();
		var verdi = finnForsendelseRequest.getVerdi();

		log.info("finnForsendelse henter forsendelse med {}={}", oppslagsnoekkel, verdi);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/{oppslagsnoekkel}/{verdi}")
						.build(oppslagsnoekkel, verdi))
				.retrieve()
				.bodyToMono(FinnForsendelseResponse.class)
				.map(FinnForsendelseResponse::getForsendelseId)
				.doOnError(this::handleError)
				.block();

		log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og {}={}", response, oppslagsnoekkel, verdi);

		return response;
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.getForsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterForsendelse har oppdatert forsendelse med forsendelseId={}", oppdaterForsendelse.getForsendelseId());
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri("/feilregistrerforsendelse")
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Retryable(include = AdminstrerForsendelseTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfoRequest) {
		log.info("oppdaterVarselInfo oppdaterer varselInfo med forsendelseId={}", oppdaterVarselInfoRequest.forsendelseId());
		webClient.put()
				.uri("/oppdatervarselinfo")
				.bodyValue(oppdaterVarselInfoRequest)
				.retrieve()
				.toBodilessEntity()
				.doOnError(this::handleError)
				.block();

		log.info("oppdaterVarselInfo har oppdatert varselInfo med forsendelseId={}", oppdaterVarselInfoRequest.forsendelseId());
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot rdist001 feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new AdminstrerForsendelseTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot rdist001 feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				response.getRawStatusCode(),
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
