package no.nav.dokdistdpi.consumer.rdist001;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.consumer.rdist001.domain.DigitalPostAdresseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.FeilRegistrerForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.FinnForsendelseResponseTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.PersisterForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.domain.PersisterForsendelseResponseTo;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.AdminstrerForsendelseTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@Component
public class AdministrerForsendelseConsumer {
	private static final String ERROR_MESSAGE = "Rdist001 feilet til å opprette forsendelse med bestillingsId={}, feilmelding={}";
	private static final String EXCEPTION_MESSAGE = "Kall mot rdist001 - feilet til å opprette forsendelse med bestillingsId=%s, feilmelding=%s";
	private static final String FORSENDELSE_ID = "forsendelseId";
	private final String url;
	private final RestTemplate restTemplate;

	@Autowired
	public AdministrerForsendelseConsumer(RestTemplateBuilder restTemplateBuilder, ServiceuserProperties serviceuser,
										  @Value("${administrerforsendelse.url}") String url) {
		this.url = url;
		this.restTemplate = restTemplateBuilder
				.basicAuthentication(serviceuser.getUsername(), serviceuser.getPassword())
				.setConnectTimeout(ofSeconds(5))
				.setReadTimeout(ofSeconds(20))
				.build();
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "hentIdent"}, percentiles = {0.5, 0.95}, histogram = true)
	public HentForsendelseResponse hentForsendelse(final String forsendelseId) {
		try {
			HttpEntity<?> httpEntity = new HttpEntity<>(createHeaders());
			ResponseEntity<HentForsendelseResponse> response = restTemplate.exchange(url + "/" + forsendelseId, GET, httpEntity, HentForsendelseResponse.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 feilet funksjonell med forsendelseId={}, feilmelding={}", forsendelseId, e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format("Kall mot rdist001 - hentForsendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 feilet teknisk med forsendelseId={}, feilmelding={}", forsendelseId, e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format("Kall mot rdist001 feilet teknisk med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "persisterForsendelse"}, histogram = true)
	public PersisterForsendelseResponseTo persisterForsendelse(final PersisterForsendelseRequestTo forsendelseRequestTo) {
		try {
			HttpEntity<?> entity = new HttpEntity<>(forsendelseRequestTo, createHeaders());
			ResponseEntity<PersisterForsendelseResponseTo> response = restTemplate.exchange(url, POST, entity, PersisterForsendelseResponseTo.class);
			return response.getBody();

		} catch (HttpClientErrorException e) {
			log.error(ERROR_MESSAGE, forsendelseRequestTo.getBestillingsId(), e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format(EXCEPTION_MESSAGE, forsendelseRequestTo.getBestillingsId(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error(ERROR_MESSAGE, forsendelseRequestTo.getBestillingsId(), e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format(EXCEPTION_MESSAGE, forsendelseRequestTo.getBestillingsId(), e.getMessage()), e);
		}
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "oppdaterForsendelseStatus"}, histogram = true)
	public void oppdaterForsendelseStatus(String forsendelseId, String forsendelseStatus) {
		String uri = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam(FORSENDELSE_ID, forsendelseId)
				.queryParam("forsendelseStatus", forsendelseStatus)
				.toUriString();
		log.info("Mottatt kall til å oppdatere forsendelse med forsendelseId={} forsendelseStatus={}", forsendelseId, forsendelseStatus);
		oppdaterForsendelse(uri, null);
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "oppdaterForsendelseStatusDigitalLeverandoerAndPostkasseadresse"}, histogram = true)
	public void oppdaterForsendelseStatusDigitalLeverandoerAndPostkasseadresse(DigitalPostAdresseRequestTo digitalPostAdresseRequestTo) {
		String uri = UriComponentsBuilder.fromHttpUrl(url)
				.path("/oppdaterdigitalinfo")
				.toUriString();
		log.info("forsendelse med forsendelseId={} mottatt kall til å oppdatere forsendelseStatus={}, digitalLeverandoeradresse og digitalPostkasseadresse",
				digitalPostAdresseRequestTo.getForsendelseId(), digitalPostAdresseRequestTo.getForsendelseStatus());
		oppdaterForsendelse(uri, digitalPostAdresseRequestTo);
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "oppdaterKonversasjonsId"}, histogram = true)
	public void oppdaterKonversasjonsId(String forsendelseId, String konversasjonsId) {
		String uri = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam(FORSENDELSE_ID, forsendelseId)
				.queryParam("konversasjonsId", konversasjonsId)
				.toUriString();
		log.info("Mottatt kall til å oppdatere forsendelse med forsendelseId={} konversasjonsId={}", forsendelseId, konversasjonsId);
		oppdaterForsendelse(uri, null);
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "finnForsendelse"}, histogram = true)
	public FinnForsendelseResponseTo finnForsendelse(final FinnForsendelseRequestTo finnForsendelseRequestTo) {
		String uri = UriComponentsBuilder.fromHttpUrl(url)
				.path("/finnforsendelse")
				.queryParam(finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi())
				.toUriString();
		try {
			HttpEntity<?> entity = new HttpEntity<>(createHeaders());
			log.info("Mottatt kall til å finne forsendelse med {}={}", finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi());
			ResponseEntity<FinnForsendelseResponseTo> response = restTemplate.exchange(uri, GET, entity, FinnForsendelseResponseTo.class);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 - finnForsendelse feilet med {}={}, feilmelding={}", finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi(), e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format("Kall mot rdist001 - finnFrosendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()),
					e);

		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 - finnForsendelse feilet med {}={}, feilmelding={}", finnForsendelseRequestTo.getOppslagsNoekkel(), finnForsendelseRequestTo.getVerdi(), e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format("Kall mot rdist001 - finnFrosendelse feilet teknisk med statusCode=%s,feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	@Monitor(value = DOK_REQUEST, extraTags = {"process", "feilRegistrerForsendelse"}, histogram = true)
	public void feilRegistrerForsendelse(FeilRegistrerForsendelseRequest feilRegistrerForsendelse) {

		try {
			HttpEntity<?> entity = new HttpEntity<>(feilRegistrerForsendelse, createHeaders());
			restTemplate.exchange(url + "/feilregistrerforsendelse", PUT, entity, Object.class).getBody();
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 - feilet til å feilregistrer forsendelse med forsendelseId={}, feilmelding={}", feilRegistrerForsendelse.getForsendelseId(), e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 - feilet til å feilregistrer forsendelse med forsendelseId={}, feilmelding={}", feilRegistrerForsendelse.getForsendelseId(), e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format("Kall mot rdist001 - feilet til å opprette forsendelse med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private void oppdaterForsendelse(String uri, DigitalPostAdresseRequestTo digitalPostAdresseRequestTo) {
		try {
			HttpEntity<?> entity = digitalPostAdresseRequestTo == null ? new HttpEntity<>(createHeaders()) :
					new HttpEntity<>(digitalPostAdresseRequestTo, createHeaders());
			restTemplate.exchange(uri, PUT, entity, String.class);
		} catch (HttpClientErrorException e) {
			log.error("Kall mot rdist001 - oppdaterForsendelse feilet med feilmelding={}", e.getMessage());
			throw new AdminstrerForsendelseFunctionalException(format("Kall mot rdist001 - oppdaterForsendelse feilet med statusCode=%s, feilmelding=%s", e.getStatusCode(), e.getMessage()),
					e);
		} catch (HttpServerErrorException e) {
			log.error("Kall mot rdist001 - oppdaterForsendelse feilet med feilmelding={}", e.getMessage());
			throw new AdminstrerForsendelseTechnicalException(format("Kall mot rdist001 - oppdaterForsendelse feilet teknisk med statusCode=%s,feilmelding=%s", e.getStatusCode(), e.getMessage()), e);
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.add(CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}
