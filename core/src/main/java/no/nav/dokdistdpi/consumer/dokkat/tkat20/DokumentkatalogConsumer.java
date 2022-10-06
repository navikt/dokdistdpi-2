package no.nav.dokdistdpi.consumer.dokkat.tkat20;

import no.nav.dokdistdpi.azure.TokenConsumer;
import no.nav.dokdistdpi.azure.TokenResponse;
import no.nav.dokdistdpi.config.cache.CacheConfig;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.exception.functional.Tkat020FunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.Tkat020TechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import no.nav.dokkat.api.tkat020.DistribusjonVarselTo;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.*;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static org.springframework.http.HttpMethod.GET;

@Component
public class DokumentkatalogConsumer implements Dokumentkatalog {
	private static final String BEARER_PREFIX = "Bearer ";
	private final String dokumenttypeInfoV4Url;
	private final String dokmetScope;
	private final TokenConsumer tokenConsumer;
	private final RestTemplate restTemplate;

	@Autowired
	public DokumentkatalogConsumer(@Value("${DokumenttypeInfo_v4_url}") String dokumenttypeInfoV4Url,
								   @Value("${dokmet_scope}") String dokmetScope,
								   TokenConsumer tokenConsumer,
								   RestTemplateBuilder restTemplateBuilder) {
		this.dokumenttypeInfoV4Url = dokumenttypeInfoV4Url;
		this.dokmetScope = dokmetScope;
		this.tokenConsumer = tokenConsumer;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@Override
	@Cacheable(CacheConfig.TKAT020_CACHE)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = "dok_consumer", extraTags = {"process", "getDokumenttypeInfo"}, histogram = true)
	public DokumenttypeInfoTo getDokumenttypeInfo(String dokumenttypeId) {
		HttpHeaders headers = createHeaders();
		try {
			HttpEntity<String> request = new HttpEntity(headers);
			DokumentTypeInfoToV4 response = restTemplate.exchange(this.dokumenttypeInfoV4Url + "/" + dokumenttypeId, GET, request, DokumentTypeInfoToV4.class).getBody();
			return mapResponse(response);
		} catch (HttpClientErrorException e) {
			throw new Tkat020FunctionalException(format("TKAT020 feilet med statusKode=%s. Fant ingen dokumenttypeInfo med dokumenttypeId=%s. Feilmelding=%s",
					e.getStatusCode(), dokumenttypeId, e.getResponseBodyAsString()), e);
		} catch (HttpServerErrorException e) {
			throw new Tkat020TechnicalException(format("TKAT020 feilet teknisk med statusKode=%s, feilmelding=%s", e.getStatusCode(), e.getResponseBodyAsString()), e);
		}
	}


	private DokumenttypeInfoTo mapResponse(final DokumentTypeInfoToV4 response) {
		if (isNull(response.getDokumentProduksjonsInfo()) &&
				isNull(response.getDokumentProduksjonsInfo().getDistribusjonInfo())) {
			throw new Tkat020FunctionalException(format("DokumentProduksjonsInfo eller DokumentProduksjonsInfo.DistribusjonInfo er null for dokumenttypeId=%s. Ikke et utgående dokument? dokumentType=%s",
					response.getDokumenttypeId(), response.getDokumentType()));
		}

		DistribusjonVarselTo distribusjonVarsel = response.getDokumentProduksjonsInfo()
				.getDistribusjonInfo().getDistribusjonVarsels().stream()
				.filter(distribusjonVarselTo -> DISTRIBUSJONS_SDP_KANAL.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))
				.findAny()
				.orElseThrow(() -> new Tkat020FunctionalException(format("Fant ingen distribusjonVarsel med varselForDistribusjonKanal=%s for dokumenttypeId=%s",
						DISTRIBUSJONS_SDP_KANAL, response.getDokumenttypeId())));

		return DokumenttypeInfoTo.builder()
				.varselTypeId(distribusjonVarsel.getVarseltypeId())
				.sikkerhetsnivaa(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getSikkerhetsnivaa())
				.build();
	}

	private HttpHeaders createHeaders() {
		TokenResponse clientCredentialToken = tokenConsumer.getClientCredentialToken(dokmetScope);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + clientCredentialToken.getAccess_token());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
