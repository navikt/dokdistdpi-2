package no.nav.dokdistdpi.sdist003.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdpi.sdist003.itest.config.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.util.Loggers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public abstract class AbstractSdist003Itest {

	static final String MASKINPORTEN_URL = "/maskinporten";
	static final String DPI_BEKREFT_URL = "/message/in/ff88849c-e281-4809-8555-7cd54952b916/read";
	static final String DPI_KVITTERINGER_URL = "/message/in?kanal=dokdistdpi-t&page_size=10";

	@Autowired
	protected JmsTemplate jmsTemplate;

	@Autowired
	protected Queue qdist014;

	@BeforeEach
	void setUp() {
		Loggers.useSl4jLoggers();
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	protected static void stubDpiKvitteringPage0(String filename) {
		stubDpiKvitteringPage(0, filename);
	}

	protected static void stubDpiKvitteringPage(int page, String filename) {
		stubFor(get(urlEqualTo(DPI_KVITTERINGER_URL + "&page=" + page))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("kvittering/" + filename)));
	}

	protected static void stubDpiKvitteringProblemPage0(HttpStatus httpStatus) {
		stubFor(get(urlEqualTo(DPI_KVITTERINGER_URL + "&page=0"))
				.willReturn(aResponse()
						.withStatus(httpStatus.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
						.withBody("""
								{
								  "type": "about:blank",
								  "title": "Teknisk feil",
								  "status": %d,
								  "detail": "Noe feilet",
								  "instance": "https://docs.digdir.no/"
								}
								""".formatted(httpStatus.value()))));
	}

	protected static void stubDpiKvitteringStatus(HttpStatusCode status) {
		stubFor(get(urlEqualTo(DPI_KVITTERINGER_URL))
				.willReturn(aResponse()
						.withStatus(status.value())));
	}

	protected static void stubPostMottattKvitteringMultiple() {
		stubFor(post(urlMatching("/message/in/.*/read"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	protected static void stubPostMaskinporten() {
		stubFor(post(urlMatching(MASKINPORTEN_URL))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_happy_response.json")));
	}

	@SuppressWarnings("unchecked")
	protected <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}
}
