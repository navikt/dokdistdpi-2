package no.nav.dokdistdpi.qdist014.itest;

import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus;
import no.nav.dokdistdpi.qdist014.itest.config.ApplicationTestConfig;
import no.nav.dokdistdpi.qdist014.map.Testutil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.listAllStubMappings;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.STS_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.EKSPEDERT;
import static no.nav.dokdistdpi.consumer.rdist001.domain.ForsendelseStatus.KLAR_FOR_DIST;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist014IT {

	private static final String FORSENDELSE_ID = "1720847";
	private static final String NY_FORSENDELSE_ID = "33333";
	private static final String KONVERSASJON_ID = "37efbd4c-413d-4e2c-bbc5-257ef4a65a45";
	private static final String BESTILLING_ID = "ff88849c-e281-4809-8555-7cd54952b916";
	private static final String KONVERSASJON_ID_VAR = "2049057a-9b53-41bb-9cc3-d10f55fa0f87";
	private static String CALL_ID;

	@Autowired
	private CacheManager cacheManager;

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

		cacheManager.getCache(TKAT020_CACHE).clear();
		cacheManager.getCache(TKAT021_CACHE).clear();
		cacheManager.getCache(MASKINPORTEN_CACHE).clear();
		cacheManager.getCache(STS_CACHE).clear();
	}

	@Test
	void shouldOppdaterForsendelToEkspedertWhenSDPKvitteringErLevering() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(ForsendelseStatus.EKSPEDERT.name(), FORSENDELSE_ID, OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/leveringskvittering.json"));

		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
			verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=1720847&forsendelseStatus=EKSPEDERT")));
		});
	}

	@Test
	void shouldOppretteNyForsendelseOgSendTilQdist009WhenSDPKvitteringErVarslingfeilet() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});
	}

	@Test
	void shouldOppretteNyForsendelseOgSendTilQdist009WhenSDPFeilKvittering() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutFeilregistrerforsendelse(OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/feilkvittering.json"));

		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});
	}

	@Test
	void shouldProcessForsendelseWithForsendelseStatusErOversendt() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));

		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});
		verifyAndCountDpiForsendelse(1, KONVERSASJON_ID, KLAR_FOR_DIST.name());

	}

	@Test
	void shouldProcessForsendelseWithForsendelseStatusErBekreftet() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(qdist009);
			assertNotNull(message);
		});
		verifyAndCountDpiForsendelse(1, KONVERSASJON_ID, KLAR_FOR_DIST.name());
	}

	@Test
	void shouldThrowInvalidExceptionWhenForsendelseStatusErKlarForDist() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-feil.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/leveringskvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		assertEquals(5, stubs.getMappings().size());
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);
		});

		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));

	}


	@Test
	void shouldEndAndLogWhenForsendelseStatusErEkspedert() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-ekspedert.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(EKSPEDERT.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		assertEquals(5, stubs.getMappings().size());
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		});
	}

	@Test
	void shouldThrowsFunctionlaExceptionWhenFinnForsendelseReturnsNull() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-feil.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", null, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist014FunksjonellFeil);
			assertNotNull(response);

		});
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
	}

	@Test
	void throwsTechnicalExceptionWhenServerErrorHappens() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", HttpStatus.INTERNAL_SERVER_ERROR.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/varslingfeiletkvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();
		assertEquals(5, stubs.getMappings().size());
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertNotNull(response);
		});

		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
	}


	@Test
	void throwsFunctionalExceptionWhenKvitteringenErNeitherSDPKvitteringNorSDPFeil() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, classpathToString("__files/kvitteringer/non-kvittering.json"));
		ListStubMappingsResult stubs = listAllStubMappings();

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertNotNull(response);
		});
	}


	@Test
	void throwsFunctionalExceptionWhenKvitteringenBodyErNull() throws IOException {
		stubGetFinnForsendelse("__files/rdist001/finnForsendelseresponse-happy.json", KONVERSASJON_ID_VAR, OK.value());
		stubGetHentForsendelse("__files/rdist001/hentForsendelseresponse-happy.json", FORSENDELSE_ID, OK.value());
		//Oversendt og bekreftet er gyldig status.
		stubPostPersisterForsendelse("__files/rdist001/persisterForsendelseResponse-happy.json", OK.value());
		stubPutOppdaterForsendelse(KLAR_FOR_DIST.name(), NY_FORSENDELSE_ID, OK.value());
		stubPutFeilregistrerforsendelse(OK.value());

		sendStringMessage(qdist014, null);
		ListStubMappingsResult stubs = listAllStubMappings();
		assertEquals(5, stubs.getMappings().size());
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			//TextMessage response = receiveTextMessage(qdist014FunksjonellFeil);
			//assertNotNull(response);
		});

	}

	private void stubGetDPIKvittering(String filepath) {
		stubFor(get(urlEqualTo("/message/in/dokdist-t"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(filepath)));
	}

	private void stubPostBekreftet(String filepath) {
		stubFor(get(urlEqualTo("/message/in/dokdist-t"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(filepath)));
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
						.withBody(Testutil.classpathToString("__files/maskinporten/maskinporten_happy_response.json"))));

	}

	private void stubPostDPISend(int status) {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
						.withBodyFile("dpi/dpi_out_status400.json")));
	}

	private void verifyAndCountDpiForsendelse(int count, String konversasjonsId, String forsendelseStatus) {
		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + konversasjonsId)));
		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(count, postRequestedFor(urlMatching("/administrerforsendelse")));
		verify(count, putRequestedFor(urlMatching("/administrerforsendelse/feilregistrerforsendelse")));
		verify(count, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + NY_FORSENDELSE_ID + "&forsendelseStatus=" + forsendelseStatus)));

	}

	private void verifyAndCountDpiForsendelse(int count, String forsendelseStatus) {
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/finnforsendelse?konversasjonsId=" + KONVERSASJON_ID)));
		verify(2, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(count, postRequestedFor(urlMatching("/administrerforsendelse")));
		verify(count, putRequestedFor(urlMatching("/administrerforsendelse/feilregistrerforsendelse")));
		verify(count, putRequestedFor(urlEqualTo("/administrerforsendelse?forsendelseId=" + NY_FORSENDELSE_ID + "&forsendelseStatus=" + forsendelseStatus)));

	}

	private void stubPutOppdaterForsendelse(String forsendelseStatus, String forsendelseId, int httpStatusvalue) {
		stubFor(put("/administrerforsendelse?forsendelseId=" + forsendelseId + "&forsendelseStatus=" + forsendelseStatus)
				.willReturn(aResponse().withStatus(httpStatusvalue)));
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) throws IOException {
		stubFor(get("/administrerforsendelse/" + forsendelseId).willReturn(aResponse().withStatus(httpStatusvalue)
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBody(classpathToString(responsebody))));
	}

	private void stubPutFeilregistrerforsendelse(int httpStatusValue) {
		stubFor(put("/administrerforsendelse/feilregistrerforsendelse")
				.willReturn(aResponse().withStatus(httpStatusValue)));
	}

	void stubGetFinnForsendelse(String responseBody, String konversasjonsId, int httpStatusValue) throws IOException {
		stubFor(WireMock.get("/administrerforsendelse/finnforsendelse?konversasjonsId=" + konversasjonsId)
				.willReturn(aResponse().withStatus(httpStatusValue)
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responseBody))));
	}

	private void stubPostPersisterForsendelse(String responseBody, int httpStatusValue) throws IOException {
		stubFor(post(urlEqualTo("/administrerforsendelse"))
				.willReturn(aResponse()
						.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withStatus(httpStatusValue)
						.withBody(classpathToString(responseBody))));
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
			TextMessage msg = new ActiveMQTextMessage();
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

	protected TextMessage receiveTextMessage(final Queue queue) {
		return (TextMessage) jmsTemplate.receive(queue);
	}

	private String getRequestAsJson(String filename) throws IOException {

		File file = new ClassPathResource(filename).getFile();
		byte[] data = new byte[(int) file.length()];
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(data);
		fileInputStream.close();
		return new String(data);
	}

}
