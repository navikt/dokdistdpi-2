package no.nav.dokdistdpi.consumer.dokmet.tkat21;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import no.nav.dokdistdpi.consumer.dokmet.DokmetTechnicalException;
import no.nav.dokmet.api.tkat021.VarselInfoTo;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.config.cache.CacheConfig.VARSELINFO_CACHE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class Tkat021Consumer {

	private static final String VARSELTYPE_INFO_URL = "/rest/varselinfo/{varselTypeId}";
	private final WebClient webClient;

	@Autowired
	public Tkat021Consumer(DokdistdpiProperties dokdistdpiProperties,
						   WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistdpiProperties.getEndpoints().getDokmetUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Cacheable(VARSELINFO_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public VarselInfo getVarselInfo(String varselTypeId) {
		return webClient.get()
				.uri(VARSELTYPE_INFO_URL, varselTypeId)
				.headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
				.retrieve()
				.bodyToMono(VarselInfoTo.class)
				.mapNotNull(this::mapResponse)
				.doOnError(exception -> handleError(exception, varselTypeId))
				.block();
	}

	private VarselInfo mapResponse(final VarselInfoTo response) {
		return isNull(response) ? null : VarselInfo.builder()
				.varselTypeId(response.getVarseltypeId())
				.stoppRepeterendeVarsel(response.getRevarslingIntervall() != null)
				.antallDagerListe(toDagerListe(response))
				.varslingsTekst(getVarslingsTekst(response))
				.preferertKanal(response.getPreferertKanal())
				.build();
	}

	private Map<String, String> getVarslingsTekst(VarselInfoTo varselInfoRestTo) {
		Map<String, String> varslingsTekst = new HashMap<>();
		varselInfoRestTo.getVarselmals().forEach(
				varselMalRestTo -> varslingsTekst.put(varselMalRestTo.getKanal(), varselMalRestTo.getFoerstegangsvarselTekst()));
		return varslingsTekst;
	}

	private List<Integer> toDagerListe(VarselInfoTo varselInfoRestTo) {
		List<Integer> antallDagerListe = new ArrayList<>();
		antallDagerListe.add(0);
		IntStream.range(0, varselInfoRestTo.getAntallRevarslinger())
				.forEach(i ->
						antallDagerListe.add(varselInfoRestTo.getRevarslingIntervall() * (i + 1))

				);
		return antallDagerListe;
	}

	private void handleError(Throwable error, String varselTypeId) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk for varselTypeId=%S med feilmelding=%s", varselTypeId, error.getMessage());

			log.warn(feilmelding);

			throw new DokmetTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot TKAT021 feilet %s med status=%s for varseltypeId=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
				varselTypeId,
				response.getStatusCode(),
				response.getMessage(),
				response.getResponseBodyAsString());

		log.warn(feilmelding);

		if (response.getStatusCode().is4xxClientError()) {
			throw new DokmetFunctionalException(feilmelding, error);
		} else {
			throw new DokmetTechnicalException(feilmelding, error);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
