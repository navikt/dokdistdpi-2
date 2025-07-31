package no.nav.dokdistdpi.consumer.naistoken;

import no.nav.dokdistdpi.config.prop.NaisProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class NaisTexasConsumer {

	private static final Pattern TARGET_PATTERN = Pattern.compile("api://[^.]+\\.[^.]+\\.[^.]+/\\.default");

	private final RestClient restClient;

	public NaisTexasConsumer(NaisProperties naisProperties,
							 RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder
				.baseUrl(naisProperties.tokenEndpoint())
				.build();
	}

	public String getSystemToken(String targetScope) {
		if (isBlank(targetScope) || !TARGET_PATTERN.matcher(targetScope).matches()) {
			throw new IllegalArgumentException("Ugyldig targetScope. Må være på format api://<cluster>.<namespace>.<other-api-app-name>/.default");
		}
		MultiValueMap<String, String> fromData = new LinkedMultiValueMap<>();
		fromData.add("identity_provider", "azuread");
		fromData.add("target", targetScope);

		return Objects.requireNonNull(restClient.post()
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(fromData)
				.retrieve()
				.body(NaisTexasToken.class)
				.accessToken());
	}
}
