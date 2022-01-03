package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import no.nav.dokdistdpi.exception.technical.MaskinportenTechnicalException;
import no.nav.dokdistdpi.metrics.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static java.util.Date.from;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@Component
public class MaskinportenTokenConsumer {
	private static final String SCOPE_DPI = "digitalpostinnbygger:send";
	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final AppCertificate appCertificate;
	private final MaskinportenProperties maskinportenProperties;
	private final RestTemplate restTemplate;

	@Autowired
	public MaskinportenTokenConsumer(AppCertificate appCertificate,
									 MaskinportenProperties maskinportenProperties,
									 RestTemplateBuilder restTemplateBuilder) {
		this.appCertificate = appCertificate;
		this.maskinportenProperties = maskinportenProperties;
		this.restTemplate = restTemplateBuilder
				.messageConverters(new FormHttpMessageConverter(),
						new MappingJackson2HttpMessageConverter())
				.errorHandler(new OidcErrorHandler())
				.setReadTimeout(ofSeconds(30))
				.setConnectTimeout(ofSeconds(5))
				.build();
	}

	@Monitor(value = "dok_consumer", extraTags = {"process", "maskinporten_fetchtoken"}, percentiles = {0.5, 0.95}, histogram = true)
	public OidcTokenResponse fetchToken() {
		URI accessTokenUri;
		try {
			accessTokenUri = maskinportenProperties.getUrl().toURI();
		} catch (URISyntaxException e) {
			log.error("Error converting property to URI", e);
			throw new RuntimeException(e);
		}

		final String maskinportenUrl = maskinportenProperties.getUrl().toString();

		LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
		attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		attrMap.add("assertion", generateJWT(maskinportenUrl));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);


		try {
			log.info("Henter accessToken fra maskinporten på url={}", maskinportenUrl);
			ResponseEntity<OidcTokenResponse> response = restTemplate.exchange(accessTokenUri, HttpMethod.POST,
					httpEntity, OidcTokenResponse.class);
			log.info("AccessToken hentet OK fra maskinporten på url={}", maskinportenUrl);
			return response.getBody();
		} catch (HttpClientErrorException e) {
			final String errorMessage = FUNKSJONELL_FEIL_ERROR_MESSAGE + e.getResponseBodyAsString();
			log.warn(errorMessage, e);
			throw new MaskinportenFunctionalException(errorMessage, e);
		} catch (HttpServerErrorException e) {
			final String errorMessage = TEKNISK_FEIL_ERROR_MESSAGE + e.getResponseBodyAsString();
			log.error(errorMessage, e);
			throw new MaskinportenTechnicalException(errorMessage, e);
		}
	}

	private String generateJWT(String issuer) {
		List<Base64> certChain = new ArrayList<>();
		try {
			certChain.add(Base64.encode(appCertificate.getX509Certificate().getEncoded()));
		} catch (CertificateEncodingException e) {
			log.error("Could not get encoded certificate", e);
			throw new RuntimeException(e);
		}

		JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.audience(maskinportenProperties.getAudience())
				.issuer(issuer)
				.claim("scope", getCurrentScopes())
				.claim("consumer", Consumer.builder()
						.authority(ISO_6523_ACTORID_UPIS.getValue())
						.id(asIso6523(NAV_ORGNUMMER))
						.build())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
				.expirationTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(30)))
				.build();

		RSASSASigner signer = new RSASSASigner(appCertificate.loadPrivateKey());

		if (appCertificate.shouldLockProvider()) {
			signer.getJCAContext().setProvider(appCertificate.getKeyStore().getProvider());
		}

		SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			log.error("Error occured during signing of JWT", e);
		}

		return signedJWT.serialize();
	}

	private String getCurrentScopes() {
		ArrayList<String> scopeList = new ArrayList<>();
		scopeList.add(SCOPE_DPI);
		return scopeList.stream().reduce((a, b) -> a + " " + b).orElse("");
	}
}
