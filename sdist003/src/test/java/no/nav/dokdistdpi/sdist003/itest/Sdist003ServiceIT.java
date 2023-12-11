package no.nav.dokdistdpi.sdist003.itest;

import no.nav.dokdistdpi.sdist003.Sdist003Service;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Egen test for Sdist003 uten scheduler funksjonalitet
 */
public class Sdist003ServiceIT extends AbstractSdist003Itest {

	@Autowired
	private Sdist003Service sdist003Service;

	@Test
	void shouldDoNoProcessingWhenDpiKvitteringReturnsNoContent() {
		stubPostMaskinporten();
		stubGetKvittering(NO_CONTENT);

		sdist003Service.hentKvitteringOgBekreft(createExchange());

		verify(0, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL)));
	}

	private static DefaultExchange createExchange() {
		return new DefaultExchange(new DefaultCamelContext());
	}
}
