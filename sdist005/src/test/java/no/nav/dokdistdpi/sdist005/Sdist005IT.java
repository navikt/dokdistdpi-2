package no.nav.dokdistdpi.sdist005;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToDateTime;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Sdist005IT {

	private static final String KONVERSASJON_ID = "37efbd4c-413d-4e2c-bbc5-257ef4a65a45";
	private static final String FORSENDELSE_ID = "5265784";

	private static final String OPPRETTFORSENDELSE_URL = "/rest/v1/administrerforsendelse";
	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String HENTUEKSPEDERTEFORSENDELSER_URL = "/rest/v1/administrerforsendelse/hentuekspederteforsendelser/%s/%s";
	private static final String FEILREGISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/feilregistrerforsendelse";

	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Queue qdist009;

	@Test
	public void shouldGetKvitteringFromDpiAccessPoint() throws UnknownHostException {
		stubLeaderElection();
		stubAzure();
		stubPostMaskinporten();
		stubHentUekspederteForsendelser();
		stubHentForsendelseStatus(KONVERSASJON_ID);
		stubHentForsendelse();
		stubPostOpprettForsendelse();
		stubPutFeilregistrerforsendelse();
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();

		await().atMost(15, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
			verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
			verify(1, getRequestedFor(urlEqualTo("/message/out/" + KONVERSASJON_ID + "/statuses")));
			verify(putRequestedFor(urlEqualTo(FEILREGISTRERFORSENDELSE_URL))
					.withRequestBody(matchingJsonPath("$.detaljer", equalTo("Bad Gateway mot postkasse")))
					.withRequestBody(matchingJsonPath("$.tidspunkt", equalToDateTime("2022-03-30T10:21:59"))));
		});
	}

	private void stubPostMaskinporten() {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_happy_response.json")));
	}

	private void stubPutOppdaterDigitalLeverandoerAndPostkasseadresse() {
		stubFor(put(urlEqualTo(OPPDATERFORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubHentUekspederteForsendelser() {
		stubFor(get(urlMatching(format(HENTUEKSPEDERTEFORSENDELSER_URL, "SDP", 6)))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/henteuekspedertforsendelse-dpi.json")));
	}

	private void stubHentForsendelseStatus(String bestillingsId) {
		stubFor(get(urlMatching("/message/out/" + bestillingsId + "/statuses"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dpi/status-feilet.json")));
	}

	private void stubHentForsendelse() {
		stubFor(get(urlMatching(HENTFORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/hentForsendelseresponse-happy.json")));
	}

	private void stubPostOpprettForsendelse() {
		stubFor(post(urlEqualTo(OPPRETTFORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/opprettForsendelseResponse-happy.json")));
	}

	private void stubPutFeilregistrerforsendelse() {
		stubFor(put(FEILREGISTRERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubLeaderElection() throws UnknownHostException {
		stubFor(get("/leaderelection")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("""
								{"name":"%s","last_update":"2023-12-13T09:46:08Z"}
								""".formatted(InetAddress.getLocalHost().getHostName()))));
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
