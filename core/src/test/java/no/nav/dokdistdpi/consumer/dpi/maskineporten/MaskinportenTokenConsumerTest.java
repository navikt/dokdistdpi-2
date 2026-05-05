package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;

@Disabled
class MaskinportenTokenConsumerTest {
	private final MaskinportenProperties maskinportenProperties = new MaskinportenProperties();

	@BeforeEach
	public void setup() {
		// test
		maskinportenProperties.setClientId(System.getProperty("maskinporten.client-id"));
		maskinportenProperties.setIssuer("https://test.maskinporten.no/");
		maskinportenProperties.setTokenEndpoint("https://test.maskinporten.no/token");
		maskinportenProperties.setScopes("digitalpostinnbygger:send");
		maskinportenProperties.setClientJwk(System.getProperty("maskinporten.client-jwk"));
	}

	@Test
	void shouldFetchTokenWhenSystemPropertiesSet() {
		MaskinportenTokenConsumer oidcTokenClient = new MaskinportenTokenConsumer(maskinportenProperties, new RestTemplateBuilder());
		final OidcTokenResponse oidcTokenResponse = oidcTokenClient.fetchToken();
		System.out.println(oidcTokenResponse.getAccessToken());
	}
}