package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import no.nav.dokdistdpi.certificate.AppCertificate;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;

import java.net.MalformedURLException;
import java.net.URL;

@Disabled
class MaskinportenTokenConsumerTest {
	private KeyStoreProperties keyStoreProperties = new KeyStoreProperties();
	private MaskinportenProperties maskinportenProperties = new MaskinportenProperties();

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
		maskinportenProperties.setClientid("979bcb5d-d311-45b0-83b7-510d82c5a68d");
		maskinportenProperties.setAudience("https://ver2.maskinporten.no/");
		maskinportenProperties.setUrl(new URL("https://ver2.maskinporten.no/token"));
		keyStoreProperties.setType(System.getProperty("virksomhetssertifikat.type"));
		keyStoreProperties.setAlias(System.getProperty("virksomhetssertifikat.alias"));
		keyStoreProperties.setPassword(System.getProperty("virksomhetssertifikat.password"));
		keyStoreProperties.setPath(new FileSystemResource(System.getProperty("virksomhetssertifikat.path")));
	}

	@Test
	void shouldFetchTokenWhenSystemPropertiesSet() {
		MaskinportenTokenConsumer oidcTokenClient = new MaskinportenTokenConsumer(new AppCertificate(keyStoreProperties), maskinportenProperties, new RestTemplateBuilder());

		final OidcTokenResponse oidcTokenResponse = oidcTokenClient.fetchToken();
		System.out.println(oidcTokenResponse.getAccessToken());
	}
}