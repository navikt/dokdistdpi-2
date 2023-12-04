package no.nav.dokdistdpi.qdist014.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistdpi.qdist014.itest.config.ApplicationTestConfig;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist014IT {

	private static final String FORSENDELSE_ID = "1720847";
	private static final String KONVERSASJON_ID = "37efbd4c-413d-4e2c-bbc5-257ef4a65a45";
	private static final String OPPSLAGSNOEKKEL_KONVERSASJONSID = "konversasjonsId";
	private static String CALL_ID;

	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/%s";
	private static final String FINNFORSENDELSE_URL = "/rest/v1/administrerforsendelse/finnforsendelse/%s/%s";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String FEILREGISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/feilregistrerforsendelse";
	private static final String JURIDISK_LOGG_URL = "/juridisklogg/api/rest/logg";

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist014;

	@Autowired
	private Queue qdist014FunksjonellFeil;
	@Autowired
	private Queue qdist009;

	@Autowired
	private Queue backoutQueue;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
		stubAzure();
		stubPostJuridiskLogg();
	}

	@Test
	void shouldOppdaterForsendelToEkspedertWhenSDPKvitteringErLevering() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/leveringskvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
			verify(2, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, KONVERSASJON_ID))));
			verify(2, getRequestedFor(urlEqualTo(format(HENTFORSENDELSE_URL, FORSENDELSE_ID))));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	void shouldOppretteNyForsendelseOgSendTilQdist009WhenSDPKvitteringErVarslingfeilet() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});
	}

	@Test
	void shouldOppretteNyForsendelseOgSendTilQdist009WhenSDPFeilKvittering() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/feilkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});
	}

	@Test
	void shouldProcessForsendelseWithForsendelseStatusErOversendt() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});

		verifyAndCountDpiForsendelse(1, KONVERSASJON_ID);
	}

	@Test
	void shouldProcessForsendelseWithForsendelseStatusErBekreftet() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		await().atMost(10, SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});

		verifyAndCountDpiForsendelse(1, KONVERSASJON_ID);
	}

	@Test
	void shouldThrowInvalidExceptionWhenForsendelseStatusErKlarForDist() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-feil.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/leveringskvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});

		verify(2, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, KONVERSASJON_ID))));
		verify(2, getRequestedFor(urlEqualTo(format(HENTFORSENDELSE_URL, FORSENDELSE_ID))));

	}

	@Test
	void shouldEndAndLogWhenForsendelseStatusErEkspedert() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-ekspedert.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
			verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, KONVERSASJON_ID))));
			verify(1, getRequestedFor(urlEqualTo(format(HENTFORSENDELSE_URL, FORSENDELSE_ID))));
		});
	}

	@Test
	void shouldThrowFunctionalExceptionWhenFinnForsendelseReturnsNull() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-feil.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", null, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});

		verify(1, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, KONVERSASJON_ID))));
	}

	@Test
	void shouldThrowTechnicalExceptionWhenServerErrorHappens() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", INTERNAL_SERVER_ERROR.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});

		verify(2, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, KONVERSASJON_ID))));
		verify(2, getRequestedFor(urlEqualTo(format(HENTFORSENDELSE_URL, FORSENDELSE_ID))));
	}

	@Test
	void shouldThrowFunctionalExceptionWhenKvitteringenErNeitherSDPKvitteringNorSDPFeil() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/non-kvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});
	}

	@Test
	void shouldThrowFunctionalExceptionWhenOpprettForsendelseReturnsForsendelseIdNull() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		stubPostOpprettForsendelse("rdist001/opprettForsendelseResponse-null.json", OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});
	}

	@Test
	void shouldThrowFunctionalExceptionWhenMottakskvittering() throws IOException {
		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/mottakskvittering.json"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
			verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		});
	}

	private void verifyAndCountDpiForsendelse(int count, String konversasjonsId) {
		verify(1, postRequestedFor(urlEqualTo(JURIDISK_LOGG_URL)));
		verify(2, getRequestedFor(urlEqualTo(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, konversasjonsId))));
		verify(2, getRequestedFor(urlEqualTo(format(HENTFORSENDELSE_URL, FORSENDELSE_ID))));
		verify(count, postRequestedFor(urlMatching("/rest/v1/administrerforsendelse")));
		verify(count, putRequestedFor(urlMatching(FEILREGISTRERFORSENDELSE_URL)));
		verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	private void stubPutOppdaterDigitalLeverandoerAndPostkasseadresse() {
		stubFor(put(urlEqualTo(OPPDATERFORSENDELSE_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) throws IOException {
		stubFor(get(format(HENTFORSENDELSE_URL, forsendelseId))
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	private void stubPutFeilregistrerforsendelse(int httpStatusValue) {
		stubFor(put(FEILREGISTRERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusValue)));
	}

	void stubGetFinnForsendelse(String responseBody, String konversasjonsId, int httpStatusValue) throws IOException {
		stubFor(get(format(FINNFORSENDELSE_URL, OPPSLAGSNOEKKEL_KONVERSASJONSID, konversasjonsId))
				.willReturn(aResponse()
						.withStatus(httpStatusValue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPostOpprettForsendelse(String responseBody, int httpStatusValue) {
		stubFor(post(urlMatching("/rest/v1/administrerforsendelse"))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBodyFile(responseBody)));
	}

	void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubPostJuridiskLogg() {
		stubFor(post(urlEqualTo(JURIDISK_LOGG_URL))
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(HttpStatus.OK.value())
						.withBodyFile("juridisklogg/juridiskloggresponse.json")));
	}

	private static String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return IOUtils.toString(inputStream, UTF_8);
	}

	private void sendStringMessage(Queue queue, final String message) {
		sendStringMessage(queue, message, null);
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = session.createTextMessage();
			msg.setText(message);
			if (callId != null) {
				msg.setStringProperty(CALL_ID, callId);
			}
			return msg;
		});
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
