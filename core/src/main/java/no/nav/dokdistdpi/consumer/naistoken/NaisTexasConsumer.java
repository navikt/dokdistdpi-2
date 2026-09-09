package no.nav.dokdistdpi.consumer.naistoken;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import no.nav.dokdistdpi.config.prop.NaisProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

@Component
public class NaisTexasConsumer {

	private static final String NAIS_TEXAS_INSTANCE = "naistexas";
	public static final Pattern TARGET_PATTERN = Pattern.compile("api://[^.]+\\.[^.]+\\.[^.]+/\\.default");

	private final RestClient restClient;

	public NaisTexasConsumer(NaisProperties naisProperties,
							 RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
				.baseUrl(naisProperties.tokenEndpoint())
				.build();
	}

	@Retry(name = NAIS_TEXAS_INSTANCE)
	@CircuitBreaker(name = NAIS_TEXAS_INSTANCE)
	public String getSystemToken(String targetScope) {
		if (isBlank(targetScope) || !TARGET_PATTERN.matcher(targetScope).matches()) {
			throw new IllegalArgumentException("Ugyldig targetScope. Må være på format api://<cluster>.<namespace>.<other-api-app-name>/.default");
		}
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "azuread");
		formData.add("target", targetScope);

		return requireNonNull(restClient.post()
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class)
				.accessToken());
	}

	@Retry(name = NAIS_TEXAS_INSTANCE)
	@CircuitBreaker(name = NAIS_TEXAS_INSTANCE)
	public String getMaskinportenToken(String targetScopes) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("identity_provider", "maskinporten");
		formData.add("target", targetScopes);

		return requireNonNull(restClient.post()
				.accept(APPLICATION_FORM_URLENCODED)
				.body(formData)
				.retrieve()
				.body(NaisTexasToken.class)).accessToken();
	}
}
