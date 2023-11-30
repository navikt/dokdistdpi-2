package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import no.nav.dokdistdpi.exception.technical.MaskinportenTechnicalException;
import no.nav.dokdistdpi.exception.technical.SertifikatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static com.nimbusds.jose.JOSEObjectType.JWT;
import static java.time.Duration.ofSeconds;
import static java.util.Date.from;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Slf4j
@Component
public class MaskinportenTokenConsumer {
	public static final String FUNKSJONELL_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Funksjonell feil: ";
	public static final String TEKNISK_FEIL_ERROR_MESSAGE = "Klarte ikke hente AccessToken fra maskinporten. Teknisk feil: ";

	private final MaskinportenProperties maskinportenProperties;
	private final RestTemplate restTemplate;

	@Autowired
	public MaskinportenTokenConsumer(MaskinportenProperties maskinportenProperties,
									 RestTemplateBuilder restTemplateBuilder) {
		this.maskinportenProperties = maskinportenProperties;
		this.restTemplate = restTemplateBuilder
				.messageConverters(new FormHttpMessageConverter(),
						new MappingJackson2HttpMessageConverter())
				.errorHandler(new OidcErrorHandler())
				.setReadTimeout(ofSeconds(30))
				.setConnectTimeout(ofSeconds(5))
				.build();
	}

	@Cacheable(MASKINPORTEN_CACHE)
	public OidcTokenResponse fetchToken() {
		LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
		attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
		attrMap.add("assertion", generateJWT());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);

		try {
			ResponseEntity<OidcTokenResponse> response = restTemplate.exchange(maskinportenProperties.getTokenEndpoint(), POST, httpEntity, OidcTokenResponse.class);
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

	private String generateJWT() {

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.audience(maskinportenProperties.getIssuer())
				.issuer(maskinportenProperties.getClientId())
				.claim("scope", getCurrentScopes())
				.claim("consumer", Consumer.builder()
						.authority(ISO_6523_ACTORID_UPIS.getValue())
						.id(asIso6523(NAV_ORGNUMMER))
						.build())
				.jwtID(UUID.randomUUID().toString())
				.issueTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
				.expirationTime(from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(30)))
				.build();

		return createSignedJWT(maskinportenProperties.getClientJwk(), claims)
				.serialize();
	}

	private String getCurrentScopes() {
		ArrayList<String> scopeList = new ArrayList<>();
		scopeList.add(maskinportenProperties.getScopes());
		return scopeList.stream().reduce((a, b) -> a + " " + b).orElse("");
	}

	private SignedJWT createSignedJWT(String rsaJwk, JWTClaimsSet claimsSet) {
		try {
			var rsaKey = RSAKey.parse(rsaJwk);
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
					.keyID(rsaKey.getKeyID())
					.type(JWT)
					.build();
			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			JWSSigner signer = new RSASSASigner(rsaKey);
			signedJWT.sign(signer);

			return signedJWT;
		} catch (ParseException | JOSEException e) {
			throw new SertifikatException("Klarte ikke å generere signert JWT", e);
		}
	}
}
