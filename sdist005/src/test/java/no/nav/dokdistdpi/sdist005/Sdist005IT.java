package no.nav.dokdistdpi.sdist005;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistdpi.consumer.lederelection.LederElectionConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.Queue;
import javax.xml.bind.JAXBElement;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.STS_CACHE;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class Sdist005IT {

	private static final String BESTILLING_ID = "ff88849c-e281-4809-8555-7cd54952b916";
	private static final String FORSENDELSE_ID = "5265784";
	private static final String NY_FORSENDELSE_ID = "5265785";

	@Value("${leder.host}")
	private String lederHost;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Queue qdist009;
	@Autowired
	private LederElectionConsumer lederElection;
	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void setUp() {
		System.setProperty("ELECTOR_PATH", lederHost);
		lederElection = mock(LederElectionConsumer.class);
		cacheManager.getCache(MASKINPORTEN_CACHE).clear();
		cacheManager.getCache(STS_CACHE).clear();
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void shouldGetKvitteringFromDpiAccessPoint() {
		when(lederElection.isLeader()).thenReturn(true);
		stubPostMaskinporten();
		stubHentUekspedertForsendelse();
		stubHentForsendelseStatus(BESTILLING_ID);
		stubHentForsendelse(FORSENDELSE_ID);
		stubPostPersisterForsendelse();
		stubPutFeilregistrerforsendelse();
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID);

		await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});

		verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
		verify(1, getRequestedFor(urlEqualTo("/message/out/" + BESTILLING_ID + "/statuses")));
	}

	private void stubPostMaskinporten() {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_happy_response.json")));
	}

	private void stubHentUekspedertForsendelse() {
		stubFor(get(urlMatching("/administrerforsendelse/henteuekspederforsendelse/SDP/6"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/henteuekspedertforsendelse-dpi.json")));
	}

	private void stubHentForsendelseStatus(String bestillingsId) {
		stubFor(get(urlMatching("/message/out/" + bestillingsId + "/statuses"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dpi/status-feilet.json")));
	}

	private void stubHentForsendelse(String forsendelseId) {
		stubFor(get(urlMatching("/administrerforsendelse/" + forsendelseId))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/hentForsendelseresponse-happy.json")));
	}

	private void stubPostPersisterForsendelse() {
		stubFor(post(urlEqualTo("/administrerforsendelse"))
				.willReturn(aResponse()
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withStatus(OK.value())
						.withBodyFile("rdist001/persisterForsendelseResponse-happy.json")));
	}

	private void stubPutFeilregistrerforsendelse() {
		stubFor(put("/administrerforsendelse/feilregistrerforsendelse")
				.willReturn(aResponse().withStatus(OK.value())));
	}

	private void stubPutOppdaterForsendelse(String forsendelseStatus, String forsendelseId) {
		stubFor(put("/administrerforsendelse?forsendelseId=" + forsendelseId + "&forsendelseStatus=" + forsendelseStatus)
				.willReturn(aResponse().withStatus(OK.value())));
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	@SuppressWarnings("unchecked")
	private <T> T receiveMessage(String message) {
		Object response = jmsTemplate.receiveAndConvert(message);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}
}
