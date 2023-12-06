package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;

import java.net.MalformedURLException;

@Disabled
class MaskinportenTokenConsumerTest {
	private final KeyStoreProperties keyStoreProperties = new KeyStoreProperties();
	private final MaskinportenProperties maskinportenProperties = new MaskinportenProperties();
	private final DpiClientProperties dpiClientProperties = new DpiClientProperties();

	@BeforeEach
	public void setup() throws MalformedURLException {
		// Sett system properties VM options for testen. Ikke putt det i koden.
		//
		// javax.net.ssl.trustStore
		// javax.net.ssl.trustStorePassword
		// virksomhetssertifikat.type
		// virksomhetssertifikat.alias
		// virksomhetssertifikat.password
		// virksomhetssertifikat.path

		/*System.setProperty("https.proxyHost", "webproxy-utvikler.nav.no");
		  System.setProperty("https.proxyPort", "8088");
		  System.setProperty("https.nonProxyHosts", "*.155.55.|*.192.168.|*.10.|*.local|*.rtv.gov|*.adeo.no|*.nav.no|*.aetat.no|*.devillo.no|*.oera.no");
		*/
		// prod
		/*maskinportenProperties.setAudience("https://maskinporten.no/");
          maskinportenProperties.setUrl(new URL("https://maskinporten.no/token"));*/

		// test
		maskinportenProperties.setClientId("979bcb5d-d311-45b0-83b7-510d82c5a68d");
		maskinportenProperties.setIssuer("https://ver2.maskinporten.no/");
		maskinportenProperties.setTokenEndpoint("https://ver2.maskinporten.no/token");
		keyStoreProperties.setType(System.getProperty("virksomhetssertifikat.type"));
		keyStoreProperties.setAlias(System.getProperty("virksomhetssertifikat.alias"));
		keyStoreProperties.setPassword(System.getProperty("virksomhetssertifikat.password"));
		keyStoreProperties.setPath(new FileSystemResource(System.getProperty("virksomhetssertifikat.path")));
		dpiClientProperties.setUrl("https://srest.qa.dataplatfor.ms/dpi/messages");
		dpiClientProperties.setMpckanal("dokdistdpi-q");
	}

	@Test
	void shouldFetchTokenWhenSystemPropertiesSet() {
		MaskinportenTokenConsumer oidcTokenClient = new MaskinportenTokenConsumer(maskinportenProperties, new RestTemplateBuilder());
		final OidcTokenResponse oidcTokenResponse = oidcTokenClient.fetchToken();
	}
}