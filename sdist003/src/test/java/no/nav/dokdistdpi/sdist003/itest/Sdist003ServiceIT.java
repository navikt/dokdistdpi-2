package no.nav.dokdistdpi.sdist003.itest;

import no.nav.dokdistdpi.sdist003.Sdist003Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.ParallelFlux;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Egen test for Sdist003 funksjonalitet
 */
public class Sdist003ServiceIT extends AbstractSdist003Itest {

	@Autowired
	private Sdist003Service sdist003Service;

	@Test
	void shouldSubscribeToSdist003AndRepeatOnce() {
		stubPostMaskinporten();
		stubDpiKvitteringPage0("8_leveringskvittering_1_feil.json");
		stubPostMottattKvitteringMultiple();

		ParallelFlux<Void> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();

		AtomicInteger atomicInteger = new AtomicInteger(0);
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(9, postRequestedFor(urlMatching("/message/in/.*/read")));
			String msg = receive(qdist014);
			if (msg != null) {
				assertThat(msg).containsAnyOf("\"type\":\"leveringskvittering\"", "\"type\":\"feil\"");
				atomicInteger.incrementAndGet();
			}
			assertThat(atomicInteger.get()).isEqualTo(9);
		});
	}

	@Test
	void shouldSubscribeToSdist003AndRepeatUntilDpiKvitteringerDeliversLessThanPageSize() {
		stubPostMaskinporten();
		stubDpiKvitteringPage(0, "0_10_leveringskvittering.json");
		stubDpiKvitteringPage(1, "1_10_leveringskvittering.json");
		stubDpiKvitteringPage(2, "2_5_leveringskvittering.json");
		stubPostMottattKvitteringMultiple();

		ParallelFlux<Void> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();

		AtomicInteger atomicInteger = new AtomicInteger(0);
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(25, postRequestedFor(urlMatching("/message/in/.*/read")));
			String msg = receive(qdist014);
			if (msg != null) {
				assertThat(msg).contains("\"type\":\"leveringskvittering\"");
				atomicInteger.incrementAndGet();
			}
			assertThat(atomicInteger.get()).isEqualTo(25);
		});
	}

	@Test
	void shouldDoNoProcessingWhenDpiKvitteringReturnsNoContent() {
		stubPostMaskinporten();
		stubDpiKvitteringStatus(NO_CONTENT);

		ParallelFlux<Void> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();

		await().during(2, TimeUnit.SECONDS).untilAsserted(() ->
				verify(0, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL))));
	}

	@Test
	void shouldDoNoProcessingWhenKvitteringPage0ReturnsProblem() {
		stubPostMaskinporten();
		stubDpiKvitteringProblemPage0(INTERNAL_SERVER_ERROR);

		ParallelFlux<Void> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();
		await().during(2, TimeUnit.SECONDS).untilAsserted(() ->
				verify(0, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL))));
	}
}
