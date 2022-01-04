package no.nav.dokdistdpi.consumer.dpi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DigitalPostContentPackager;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.StandardBusinessDocumentMapper;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.exception.technical.KanIkkeDistribuereForsendelseException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.util.Base64;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.KANAL;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * @author Tsigab A. Gebremedhin, NAV
 */
@Slf4j
@Component
public class DpiMeldingsformidler {

	private final StandardBusinessDocumentMapper sbdMapper;
	private final DigitalPostContentPackager digitalPostContentPackager;
	private final AppCertificate appCertificate;
	private final RestTemplate restTemplate;
	private final DpiClientProperties dpiClientProperties;
	private final ObjectMapper objectMapper;

	@Autowired
	public DpiMeldingsformidler(@Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper, DpiClientProperties dpiClientProperties,
								StandardBusinessDocumentMapper sbdMapper, RestTemplateBuilder restTemplateBuilder,
								DigitalPostContentPackager digitalPostContentPackager, AppCertificate appCertificate) {
		this.objectMapper = dpiObjectMapper;
		this.dpiClientProperties = dpiClientProperties;
		this.sbdMapper = sbdMapper;
		this.digitalPostContentPackager = digitalPostContentPackager;
		this.appCertificate = appCertificate;
		this.restTemplate = restTemplateBuilder.setConnectTimeout(ofSeconds(15)).setReadTimeout(ofSeconds(30)).build();
	}

	@Handler
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "sendMelding"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HttpStatus sendMelding(Forsendelse forsendelse, Exchange exchange) {
		byte[] dokumentpakke = digitalPostContentPackager.createKryptertDokumentpakke(forsendelse, appCertificate);

		forsendelse.getDigital().setDokumentpakkefingeravtrykk(getDokumentpakkefingeravtrykk(dokumentpakke));

		StandardBusinessDocument standardBusinessDocument = sbdMapper.mapDigitalPostEnvelope(forsendelse);
		String uri = UriComponentsBuilder.fromHttpUrl(dpiClientProperties.getUrl())
				.queryParam(KANAL, dpiClientProperties.getMpckanal())
				.toUriString();

		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("forretningsmelding", generateStandardBusinessDocumentJWT(standardBusinessDocument));
		multipartBodyBuilder.part("dokumentpakke", dokumentpakke);
		HttpEntity<?> httpEntity = new HttpEntity<>(multipartBodyBuilder, headers(forsendelse.getDigital().getMaskinportentoken()));

		ResponseEntity<String> response = restTemplate.exchange(uri, POST, httpEntity, String.class);

		exchange.setProperty("DigitalPostStatus", response.getStatusCode());

		if (!CREATED.equals(response.getStatusCode())) {
			throw new KanIkkeDistribuereForsendelseException(format("kunne ikke sende til digdir hjorne2 med status=%s", response.getStatusCode()));
		}

		return response.getStatusCode();
	}

	public DigitalPost.Dokumentpakkefingeravtrykk getDokumentpakkefingeravtrykk(byte[] asicStream) {
		MessageDigest messageDigest = new SHA256.Digest();
		byte[] digest = messageDigest.digest(asicStream);
		return DigitalPost.Dokumentpakkefingeravtrykk.builder()
				.digestMethod("")
				.digestValue(Base64.getEncoder().encodeToString(digest))
				.build();
	}

	private String generateStandardBusinessDocumentJWT(StandardBusinessDocument sbd) {
		try {
			String sbdJson = objectMapper.writeValueAsString(sbd);
			JWTClaimsSet claims = new JWTClaimsSet.Builder().claim("StandardBusinessDocument", sbdJson).build();
			return GenerateJwt.generateJWT(claims, appCertificate);
		} catch (JsonProcessingException e) {
			log.warn("StandardBusinessDocument mapping feilet. Feilmelding: {}", e.getMessage());
			return null;
		}
	}

	private HttpHeaders headers(final String maskinportentoken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MULTIPART_FORM_DATA);
		headers.setBearerAuth(maskinportentoken);
		return headers;
	}
}
