package no.nav.dokdistdpi.consumer.dpi;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.DigitalPostContentPackager;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.StandardBusinessDocumentMapper;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.apache.camel.Handler;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.KANAL;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.KANAL_NAVN;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_DELAY;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.BACKOFF_MULTIPLIER;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DOK_REQUEST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROCESS;
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
	private final String dpiUrl;

	@Autowired
	public DpiMeldingsformidler(@Value("${dpi.url}") String dpiUrl, StandardBusinessDocumentMapper sbdMapper, RestTemplateBuilder restTemplateBuilder,
								DigitalPostContentPackager digitalPostContentPackager, AppCertificate appCertificate) {
		this.dpiUrl = dpiUrl;
		this.sbdMapper = sbdMapper;
		this.digitalPostContentPackager = digitalPostContentPackager;
		this.appCertificate = appCertificate;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(ofSeconds(15))
				.setReadTimeout(ofSeconds(30))
				.build();
	}

	@Handler
	@Monitor(value = DOK_REQUEST, extraTags = {PROCESS, "sendMelding"}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = AbstractDokdistdpiTechnicalException.class, backoff = @Backoff(delay = BACKOFF_DELAY, multiplier = BACKOFF_MULTIPLIER))
	public HttpStatus sendMelding(Forsendelse forsendelse) throws IOException {
		InputStream dokumentpakke = digitalPostContentPackager.createKryptertDokumentpakke(forsendelse, appCertificate);

		String dokumentpakkefingeravtrykk = getDokumentpakkefingeravtrykk(dokumentpakke);

		forsendelse.getDigital().setDokumentpakkefingeravtrykk(DigitalPost.Dokumentpakkefingeravtrykk.builder()
						.digestValue(dokumentpakkefingeravtrykk)
				.build());

		StandardBusinessDocument standardBusinessDocument = sbdMapper.mapDigitalPostEnvelope(forsendelse);
		String uri = UriComponentsBuilder.fromHttpUrl(dpiUrl)
				.queryParam(KANAL, KANAL_NAVN)
				.toUriString();
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("forretningsmelding", standardBusinessDocument);
		multipartBodyBuilder.part("dokumentpakke", dokumentpakke);

		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(headers(forsendelse.getDigital().getMaskinportentoken())), String.class);

		return response.getStatusCode();

	}


	private HttpHeaders headers(final String maskinportentoken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MULTIPART_FORM_DATA);
		headers.setBearerAuth(maskinportentoken);
		return headers;
	}


	public String getDokumentpakkefingeravtrykk(InputStream asicStream) throws IOException {
		MessageDigest digest = new SHA256.Digest();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try(DigestOutputStream digestStream = new DigestOutputStream(baos, digest)) {
			copy(asicStream, digestStream);
		} finally  {
			if (asicStream != null) {
				asicStream.close();

			}
		}
		return new String(Base64.getDecoder().decode(digest.digest()), UTF_8);
	}

	private static void copy(InputStream source, OutputStream sink) throws IOException {
		byte[] buf = new byte[8192];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
		}

	}

}
