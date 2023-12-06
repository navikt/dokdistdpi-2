package no.nav.dokdistdpi.sdist003.itest;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdpi.sdist003.TestUtil.classpathFilesToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;


public class Sdist003ITest extends AbstractSdist003Itest {
	@Test
	public void shouldGetKvitteringFromDpiAccessPoint() {
		when(lederElection.isLeader()).thenReturn(true);
		stubGetKvittering();
		stubPostMottattKvittering();
		stubPostMaskinporten();

		String expectedMessage = classpathFilesToString("kvittering/feil_kvittering_sbd.json");
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, postRequestedFor(urlMatching(DPI_BEKREFT_URL)));
			String message = receive(qdist014);
			assertThat(message).isEqualToIgnoringWhitespace(expectedMessage);
		});
	}
}
