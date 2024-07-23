package no.nav.dokdistdpi.consumer.dokmet.tkat20;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import no.nav.dokdistdpi.consumer.dokmet.DokmetTechnicalException;
import no.nav.dokmet.api.tkat020.DistribusjonVarselTo;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.APP_NAME;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DISTRIBUSJONS_SDP_KANAL;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Component
public class Tkat020Consumer {

	private static final String DOKUMENTTYPE_INFO_URL = "/rest/dokumenttypeinfo/{dokumenttypeId}";

	private final WebClient webClient;

	public Tkat020Consumer(DokdistdpiProperties dokdistdpiProperties,
						   WebClient webClient) {
		this.webClient = webClient
				.mutate()
				.baseUrl(dokdistdpiProperties.getEndpoints().getDokmetUrl())
				.defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.build();
	}

	@Cacheable(value = TKAT020_CACHE)
	@Retryable(retryFor = DokmetTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DokumenttypeInfo hentDokumenttypeInfo(final String dokumenttypeId) {

		return webClient.get()
				.uri(DOKUMENTTYPE_INFO_URL, dokumenttypeId)
				.headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
				.retrieve()
				.bodyToMono(DokumenttypeInfoTo.class)
				.mapNotNull(this::mapDokumenttypeInfoTo)
				.doOnError(this::handleError)
				.block();
	}

	private void handleError(Throwable error) {
		if (!(error instanceof WebClientResponseException response)) {
			String feilmelding = format("Kall mot dokmet feilet teknisk med feilmelding=%s", error.getMessage());

			log.warn(feilmelding);

			throw new DokmetTechnicalException(feilmelding, error);
		}

		String feilmelding = format("Kall mot dokmet feilet %s med status=%s, feilmelding=%s, response=%s",
				response.getStatusCode().is4xxClientError() ? "funksjonelt" : "teknisk",
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

	private DokumenttypeInfo mapDokumenttypeInfoTo(final DokumenttypeInfoTo response) {
		if (isNull(response.getDokumentProduksjonsInfo()) &&
				isNull(response.getDokumentProduksjonsInfo().getDistribusjonInfo())) {
			throw new DokmetFunctionalException(format("DokumentProduksjonsInfo eller DokumentProduksjonsInfo.DistribusjonInfo er null for dokumenttypeId=%s. Ikke et utgående dokument? dokumentType=%s",
					response.getDokumenttypeId(), response.getDokumentType()));
		}

		DistribusjonVarselTo distribusjonVarsel = response.getDokumentProduksjonsInfo()
				.getDistribusjonInfo().getDistribusjonVarsels().stream()
				.filter(distribusjonVarselTo -> DISTRIBUSJONS_SDP_KANAL.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))
				.findAny()
				.orElseThrow(() -> new DokmetFunctionalException(format("Fant ingen distribusjonVarsel med varselForDistribusjonKanal=%s for dokumenttypeId=%s",
						DISTRIBUSJONS_SDP_KANAL, response.getDokumenttypeId())));

		return DokumenttypeInfo.builder()
				.varselTypeId(distribusjonVarsel.getVarseltypeId())
				.sikkerhetsnivaa(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getSikkerhetsnivaa())
				.build();
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
