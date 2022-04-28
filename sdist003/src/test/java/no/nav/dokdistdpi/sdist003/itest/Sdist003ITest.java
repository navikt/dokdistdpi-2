package no.nav.dokdistdpi.sdist003.itest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.lederelection.LederElectionConsumer;
import no.nav.dokdistdpi.sdist003.Sdist003Service;
import no.nav.dokdistdpi.sdist003.TestUtil;
import no.nav.dokdistdpi.sdist003.itest.config.ApplicationTestConfig;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.Queue;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.STS_CACHE;
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
public class Sdist003ITest {

	private static final String BESTILLING_ID = "ff88849c-e281-4809-8555-7cd54952b916";

	@Value("${leder.host}")
	private String lederHost;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist014;

	@Autowired
	private ProducerTemplate producerTemplate;

	@Autowired
	private DpiClient dpiClient;
	@Autowired
	private MaskinportenTokenConsumer maskinportenToken;

	@Autowired
	private LederElectionConsumer lederElection;
	@Autowired
	private Sdist003Service sdist003Service;
	@Autowired
	private CacheManager cacheManager;
	@Autowired
	private MeterRegistry meterRegistry;
	private ObjectMapper objectMapper;

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
		stubGetKvittering();
		stubPostMottattKvittering();
		stubPostMaskinporten();
		stubPostJuridiskLogg(HttpStatus.OK, "__files/juridisklogg/juridiskloggresponse.json");
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist014);
			assertNotNull(message);
		});

		verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
		verify(1, getRequestedFor(urlEqualTo("/message/in?kanal=dokdistdpi-t")));
	}

	private void stubPostJuridiskLogg(HttpStatus status, String filePath) {
		stubFor(post(urlEqualTo("/juridisklogg"))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(status.value())
						.withBody(TestUtil.classpathToString(filePath))));
	}

	private void stubGetKvittering() {
		stubFor(get(urlEqualTo("/message/in?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(TestUtil.classpathToString("__files/kvittering/feil_forretningsmelding.json"))));
	}

	private void stubPostMottattKvittering() {
		stubFor(post(urlMatching("/message/in/" + BESTILLING_ID + "/read"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubPostMaskinporten() {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(TestUtil.classpathToString("__files/maskinporten/maskinporten_happy_response.json"))));

	}

	private void stubPostMaskinportenFeil(int status) {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse().withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(TestUtil.classpathToString("__files/maskinporten/maskinporten_feil.json"))));

	}

	private void stubPostSecurityToken() {
		stubFor(post("/securitytoken?grant_type=client_credentials&scope=openid")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(TestUtil.classpathToString("__files/sts/stsResponse-happy.json"))));
	}

	private void stubPostHentKvittering(int httpStatusValue, String responseBody) {
		stubFor(post(urlEqualTo("/pull"))
				.willReturn(aResponse()
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML_VALUE)
						.withStatus(httpStatusValue)
						.withBodyFile(responseBody)));
	}

	private static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);

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
