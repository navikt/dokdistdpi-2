package no.nav.dokdistdpi.sdist003.itest;

import no.nav.dokdistdpi.sdist003.Sdist003Service;
import no.nav.dokdistdpi.slack.SlackService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Egen test for Sdist003 funksjonalitet
 */
public class Sdist003ServiceIT extends AbstractSdist003Itest {

	@Autowired
	private Sdist003Service sdist003Service;

	@MockitoBean
	private SlackService slackServiceMock;

	@Test
	void shouldProcessPage0With8LeveringskvitteringAnd1Feil() {
		stubPostMaskinporten();
		stubDpiKvitteringPage0("8_leveringskvittering_1_feil.json");
		stubPostMottattKvitteringMultiple();

		Flux<String> kvitteringer = sdist003Service.behandleKvitteringer();
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
			verifyNoInteractions(slackServiceMock);
		});
	}

	@Test
	void shouldProcess3PagesWithLeveringskvittering() {
		stubPostMaskinporten();
		stubDpiKvitteringPage(0, "0_10_leveringskvittering.json");
		stubDpiKvitteringPage(1, "1_10_leveringskvittering.json");
		stubDpiKvitteringPage(2, "2_5_leveringskvittering.json");
		stubPostMottattKvitteringMultiple();

		Flux<String> kvitteringer = sdist003Service.behandleKvitteringer();
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
			verifyNoInteractions(slackServiceMock);
		});
	}

	@Test
	void shouldDoNoProcessingWhenDpiKvitteringReturnsNoContent() {
		stubPostMaskinporten();
		stubDpiKvitteringStatus(NO_CONTENT);

		Flux<String> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();

		await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(0, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL)));
			verifyNoInteractions(slackServiceMock);
		});
	}

	@Test
	void shouldDoNoProcessingWhenKvitteringPage0ReturnsProblem() {
		stubPostMaskinporten();
		stubDpiKvitteringProblemPage0(INTERNAL_SERVER_ERROR);

		Flux<String> kvitteringer = sdist003Service.behandleKvitteringer();
		kvitteringer.subscribe();
		await().during(2, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(0, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL)));
			Mockito.verify(slackServiceMock, Mockito.times(1))
					.sendMelding("Sdist003 feilet under behandling av kvitteringer med exception=no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException");
		});
	}
}
