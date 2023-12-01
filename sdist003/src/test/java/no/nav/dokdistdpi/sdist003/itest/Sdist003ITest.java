package no.nav.dokdistdpi.sdist003.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdpi.consumer.lederelection.LederElectionConsumer;
import no.nav.dokdistdpi.sdist003.itest.config.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdpi.sdist003.TestUtil.classpathFilesToString;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Sdist003ITest {

	private static final String BESTILLING_ID = "ff88849c-e281-4809-8555-7cd54952b916";
	static final String DPI_BEKREFT_URL = "/message/in/" + BESTILLING_ID + "/read";
	static final String MASKINPORTEN_URL = "/maskinporten";
	static final String DPI_KVITTERINGER_URL = "/message/in?kanal=dokdistdpi-t&page_size=10";

	@Value("${leder.host}")
	private String lederHost;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist014;

	@Autowired
	private LederElectionConsumer lederElection;

	@BeforeEach
	void setUp() {
		System.setProperty("ELECTOR_PATH", lederHost);
		lederElection = mock(LederElectionConsumer.class);
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void shouldGetKvitteringFromDpiAccessPoint() {
		when(lederElection.isLeader()).thenReturn(true);
		stubGetKvittering();
		stubPostMottattKvittering();
		stubPostMaskinporten();

		String expectedMessage = classpathFilesToString("kvittering/feil_kvittering_sbd.json");
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist014);
			assertThat(message).isEqualToIgnoringWhitespace(expectedMessage);
			verify(1, postRequestedFor(urlEqualTo(DPI_BEKREFT_URL)));
		});
	}

	private void stubGetKvittering() {
		stubFor(get(urlEqualTo(DPI_KVITTERINGER_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("kvittering/feil_forretningsmelding.json")));
	}

	private void stubPostMottattKvittering() {
		stubFor(post(urlMatching(DPI_BEKREFT_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubPostMaskinporten() {
		stubFor(post(urlMatching(MASKINPORTEN_URL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_happy_response.json")));

	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

}
