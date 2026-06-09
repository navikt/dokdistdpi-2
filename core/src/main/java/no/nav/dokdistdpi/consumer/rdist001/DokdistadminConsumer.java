package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import no.nav.dokdistdpi.common.NavHeadersFilter;
import no.nav.dokdistdpi.config.WebClientAzureAuthentication;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribuerTilNyKanalRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilregistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.OpprettForsendelseResponseTo;
import no.nav.dokdistdpi.exception.functional.AdministrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.functional.KanIkkeDistribuereTilNyKanalException;
import no.nav.dokdistdpi.exception.technical.AdministrerForsendelseTechnicalException;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
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

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequest) {
		var bestillingsId = opprettForsendelseRequest.getBestillingsId();

		log.info("opprettForsendelse oppretter forsendelse med bestillingsId={}", bestillingsId);

		var forsendelseId = webClient.post()
				.bodyValue(opprettForsendelseRequest)
				.retrieve()
				.bodyToMono(OpprettForsendelseResponseTo.class)
				.onErrorMap(this::mapError)
				.map(response -> new Forsendelse(response.getForsendelseId()).getForsendelseId())
				.block();

		log.info("opprettForsendelse har opprettet forsendelse med forsendelseId={} og bestillingsId={}", forsendelseId, bestillingsId);

		return forsendelseId;
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {

		log.info("hentForsendelse henter forsendelse med forsendelseId={}", forsendelseId);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/{forsendelseId}")
						.build(forsendelseId))
				.retrieve()
				.bodyToMono(HentForsendelseResponse.class)
				.onErrorMap(this::mapError)
				.block();

		log.info("hentForsendelse har hentet forsendelse med forsendelseId={}", forsendelseId);

		return response;
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public String finnForsendelse(final FinnForsendelseRequest finnForsendelseRequest) {
		var oppslagsnoekkel = finnForsendelseRequest.getOppslagsnoekkel().noekkel;
		var verdi = finnForsendelseRequest.getVerdi();

		log.info("finnForsendelse henter forsendelse med {}={}", oppslagsnoekkel, verdi);

		var response = webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/finnforsendelse/{oppslagsnoekkel}/{verdi}")
						.build(oppslagsnoekkel, verdi))
				.retrieve()
				.bodyToMono(FinnForsendelseResponse.class)
				.map(FinnForsendelseResponse::getForsendelseId)
				.onErrorMap(this::mapError)
				.block();

		log.info("finnForsendelse har hentet forsendelse med forsendelseId={} og {}={}", response, oppslagsnoekkel, verdi);

		return response;
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelse) {
		log.info("oppdaterForsendelse oppdaterer forsendelse med forsendelseId={}", oppdaterForsendelse.getForsendelseId());

		webClient.put()
				.uri("/oppdaterforsendelse")
				.bodyValue(oppdaterForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("oppdaterForsendelse har oppdatert forsendelse med forsendelseId={}", oppdaterForsendelse.getForsendelseId());
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public void feilregistrerForsendelse(FeilregistrerForsendelseRequest feilregistrerForsendelse) {
		log.info("feilregistrerForsendelse feilregistrerer forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());

		webClient.put()
				.uri("/feilregistrerforsendelse")
				.bodyValue(feilregistrerForsendelse)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("feilregistrerForsendelse har feilregistrert forsendelse med forsendelseId={}", feilregistrerForsendelse.getForsendelseId());
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class)
	public void oppdaterVarselInfo(OppdaterVarselInfoRequest oppdaterVarselInfoRequest) {
		log.info("oppdaterVarselInfo oppdaterer varselInfo med forsendelseId={}", oppdaterVarselInfoRequest.forsendelseId());
		webClient.put()
				.uri("/oppdatervarselinfo")
				.bodyValue(oppdaterVarselInfoRequest)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();

		log.info("oppdaterVarselInfo har oppdatert varselInfo med forsendelseId={}", oppdaterVarselInfoRequest.forsendelseId());
	}

	@Retryable(includes = AdministrerForsendelseTechnicalException.class, multiplier = BACKOFF_MULTIPLIER)
	public void distribuerTilNyKanal(final DistribuerTilNyKanalRequest distribuerTilNyKanalRequest) {

		log.info("distribuerTilNyKanal distribuerer forsendelse med forsendelseId={} til print", distribuerTilNyKanalRequest.forsendelseId());
		webClient.post()
				.uri("/distribuertilnykanal")
				.bodyValue(distribuerTilNyKanalRequest)
				.retrieve()
				.toBodilessEntity()
				.onErrorMap(this::mapError)
				.block();
	}

	private Throwable mapError(Throwable error) {
		if (error instanceof WebClientResponseException response && response.getStatusCode().is4xxClientError()) {
			if(response.getStatusCode() == HttpStatus.CONFLICT) {
				String responseBody = response.getResponseBodyAsString();
				throw new KanIkkeDistribuereTilNyKanalException("distribuerTilNyKanal feilet. " + responseBody, response);
			}
			return new AdministrerForsendelseFunctionalException(
					format("Kall mot AdministrerForsendelse feilet funksjonell med status=%s, feilmelding=%s",
							response.getStatusCode(),
							response.getMessage()),
					error);
		} else {
			return new AdministrerForsendelseTechnicalException(
					format("Kall mot AdministrerForsendelse feilet teknisk med feilmelding=%s", error.getMessage()),
					error);
		}
	}
}
