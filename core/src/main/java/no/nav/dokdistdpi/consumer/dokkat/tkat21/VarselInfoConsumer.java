package no.nav.dokdistdpi.consumer.dokkat.tkat21;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.exception.functional.Tkat021FunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.Tkat021TechnicalException;
import no.nav.dokkat.schemas.tkat021.VarselInfoRestTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.config.cache.CacheConfig.VARSELINFO_CACHE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;

@Slf4j
@Component
public class VarselInfoConsumer implements VarselInfo {

	private final RestTemplate restTemplate;
	private final String varselInfoUrl;

	@Autowired
	public VarselInfoConsumer(RestTemplateBuilder restTemplateBuilder,
							  @Value("${varselinfo_url}") String varselInfoUrl) {
		this.varselInfoUrl = varselInfoUrl;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(ofSeconds(5))
				.setReadTimeout(ofSeconds(20))
				.build();
	}

	@Cacheable(VARSELINFO_CACHE)
	@Retryable(retryFor = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public VarselInfoTo getVarselInfo(String varselTypeId) {
		try {
			VarselInfoRestTo response = restTemplate.getForObject(this.varselInfoUrl + "/" + varselTypeId, VarselInfoRestTo.class);
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new Tkat021FunctionalException(format("TKAT021 feilet med statusKode=%s. Fant ingen VarselInfo med VarselTypeId=%s. Feilmelding=%s",
					e.getStatusCode(), varselTypeId, e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new Tkat021TechnicalException(format("TKAT021 feilet teknisk med statusKode=%s i oppslag på varselTypeId=%s. Feilmelding=%s", e
					.getStatusCode(), varselTypeId, e.getResponseBodyAsString()), e);
		}
	}

	private VarselInfoTo mapResponse(final VarselInfoRestTo response) {
		return isNull(response) ? null : VarselInfoTo.builder()
				.varselTypeId(response.getVarseltypeId())
				.stoppRepeterendeVarsel(response.getRevarslingIntervall() != null)
				.antallDagerListe(toDagerListe(response))
				.varslingsTekst(getVarslingsTekst(response))
				.preferertKanal(response.getPreferertKanal())
				.build();
	}

	private Map<String, String> getVarslingsTekst(VarselInfoRestTo varselInfoRestTo) {
		Map<String, String> varslingsTekst = new HashMap<>();
		varselInfoRestTo.getVarselmals().forEach(
				varselMalRestTo -> varslingsTekst.put(varselMalRestTo.getKanal(), varselMalRestTo.getFoerstegangsvarselTekst()));
		return varslingsTekst;
	}

	private List<Integer> toDagerListe(VarselInfoRestTo varselInfoRestTo) {
		List<Integer> antallDagerListe = new ArrayList<>();
		antallDagerListe.add(0);
		IntStream.range(0, varselInfoRestTo.getAntallRevarslinger())
				.forEach(i ->
						antallDagerListe.add(varselInfoRestTo.getRevarslingIntervall() * (i + 1))

				);
		return antallDagerListe;
	}
}
