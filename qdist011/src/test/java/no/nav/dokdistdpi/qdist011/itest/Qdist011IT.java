package no.nav.dokdistdpi.qdist011.itest;

import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import lombok.SneakyThrows;
import no.nav.dokdistdpi.cloudstorage.DokDistDokumentFraBucket;
import no.nav.dokdistdpi.cloudstorage.EncryptedBucketStorage;
import no.nav.dokdistdpi.cloudstorage.JsonSerializer;
import no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdpi.qdist011.TestUtil.classpathToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist011IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENTTYPE_ID_HOVEDDOK = "dokumenttypeIdHoveddok";
	private static final String VARSEL_TYPE_ID = "SDP_000004";
	private static final String KONVERSASJON_ID = "601a9fcd-8bae-4076-a2d7-37f9dd17e050";

	public static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK = "dokumentObjektReferanseHoveddok";
	public static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 = "dokumentObjektReferanseVedlegg1";
	public static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 = "dokumentObjektReferanseVedlegg2";
	public static final String HOVEDDOK_TEST_CONTENT = "HOVEDDOK_TEST_CONTENT";
	public static final String VEDLEGG1_TEST_CONTENT = "VEDLEGG1_TEST_CONTENT";
	public static final String VEDLEGG2_TEST_CONTENT = "VEDLEGG2_TEST_CONTENT";
	public static String CALL_ID;

	private static final String HENTFORSENDELSE_URL = "/rest/v1/administrerforsendelse/" + FORSENDELSE_ID;
	private static final String OPPDATERVARSELINFO_URL = "/rest/v1/administrerforsendelse/oppdatervarselinfo";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String TKAT020_URL = "/rest/dokumenttypeinfo/";
	private static final String TKAT021_URL = "/rest/varselinfo/";

	@Autowired
	@Lazy
	private EncryptedBucketStorage encryptedBucketStorage;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist011;

	@Autowired
	private Queue qdist011FunksjonellFeil;

	@Autowired
	private Queue backoutQueue;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();
		stubNaisTexasToken();

		when(encryptedBucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK), anyString()))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraBucket.builder().pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build()));
		when(encryptedBucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1), anyString()))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraBucket.builder().pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build()));
		when(encryptedBucketStorage.downloadObject(eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2), anyString()))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraBucket.builder().pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build()));
	}

	@SneakyThrows
	@Test
	public void shouldProcessForsendelseOgSendTilDigitalPost() {
		stubAzure();
		stubGetDigipostDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-happy.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinporten();
		stubPostDPISend();
		stubGetDPIStatus();
		stubPutVarselInfo();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(1, getRequestedFor(urlEqualTo(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK)));
			verify(1, getRequestedFor(urlEqualTo(TKAT021_URL + VARSEL_TYPE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")));
			verify(1, postRequestedFor(urlEqualTo("/saf/graphql")));
			verify(exactly(1), postRequestedFor(urlPathEqualTo("/maskinporten")));
			verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
					.withRequestBody(containing("Content-Type: application/octet-stream")
							.and(containing("Content-Type: text/plain"))));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERVARSELINFO_URL)));
		});
	}

	@SneakyThrows
	@Test
	public void shouldProcessForsendelseWhenVarselIsNull() {
		stubAzure();
		stubGetEBoksDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-null.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubPutVarselInfo();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinporten();
		stubPostDPISend();
		stubGetDPIStatus();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(1, getRequestedFor(urlEqualTo(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK)));
			verify(1, getRequestedFor(urlEqualTo(TKAT021_URL + VARSEL_TYPE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")));
			verify(1, postRequestedFor(urlEqualTo("/saf/graphql")));
			verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
			verify(0, putRequestedFor(urlEqualTo(OPPDATERVARSELINFO_URL)));
		});
	}

	@Test
	void shouldHandleForsendelseOversendtWhenDuplikatForsendelse() {
		stubAzure();
		stubGetDigipostDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-happy.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinporten();
		stubPostDPIDuplicate();
		stubGetDPIStatus();
		stubPutVarselInfo();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
			verify(1, getRequestedFor(urlEqualTo(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK)));
			verify(1, getRequestedFor(urlEqualTo(TKAT021_URL + VARSEL_TYPE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")));
			verify(1, postRequestedFor(urlEqualTo("/saf/graphql")));
			verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
			verify(1, putRequestedFor(urlEqualTo(OPPDATERVARSELINFO_URL)));
		});
	}

	@SneakyThrows
	@Test
	public void shouldThrowValideringsfeilException() {
		stubAzure();
		stubGetDigipostDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-happy.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinporten();
		stubPostDPISend(BAD_REQUEST.value());

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist011FunksjonellFeil);
			assertNotNull(response);
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(1, getRequestedFor(urlEqualTo(TKAT021_URL + VARSEL_TYPE_ID)));
		verify(1, postRequestedFor(urlEqualTo("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")));
		verify(1, postRequestedFor(urlEqualTo("/saf/graphql")));
		verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
	}

	@SneakyThrows
	@Test
	public void shouldThrowExceptionIfMaskinportenIsNull() {
		stubAzure();
		stubGetDigipostDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-happy.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinportenFeil(BAD_REQUEST.value());
		stubPostDPISend();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist011FunksjonellFeil);
			assertThat(response).contains("<forsendelseId>33333</forsendelseId>");
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	@SneakyThrows
	@Test
	public void shouldThrowAdministrerforsendelseNotFoundException() {
		stubAzure();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", NOT_FOUND.value());

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		});
	}

	@SneakyThrows
	@Test
	public void shouldThrowTechnicalExceptionWhenDigitalKontaktInfoIsNotAccessable() {
		stubAzure();
		stubGetDigipostDigitalKontaktInformasjon(INTERNAL_SERVER_ERROR.value());
		stubGetDokumentTypeInfo("tkat020-happy.json");
		stubGetVarselInfo("tkat021-happy.json");
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPutOppdaterForsendelse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", OK.value());
		stubPostMaskinporten();
		stubPostDPISend();
		stubGetDPIStatus();
		stubPutVarselInfo();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String message = receive(backoutQueue);
			assertNotNull(message);
		});

		verify(1, getRequestedFor(urlEqualTo(HENTFORSENDELSE_URL)));
		verify(1, getRequestedFor(urlEqualTo(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(1, getRequestedFor(urlEqualTo(TKAT021_URL + VARSEL_TYPE_ID)));
		verify(3, postRequestedFor(urlEqualTo("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")));
		verify(0, postRequestedFor(urlEqualTo("/safgraphql")));
		verify(0, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
	}

	private void stubPostDPISend() {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(CREATED.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dpi/dpi_out_status.json")));
	}

	private void stubPostDPIDuplicate() {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dpi/dpi_out_duplicate.json")));
	}

	private void stubGetDPIStatus() {
		stubFor(get(urlEqualTo("/message/out/" + KONVERSASJON_ID + "/statuses"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
						.withBodyFile("dpi/dpi_forsendelse_status.json")));
	}

	private void stubPostDPISend(int status) {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
						.withBodyFile("dpi/dpi_out_status400.json")));
	}

	private void stubPostMaskinporten() {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_happy_response.json")));
	}

	private void stubPostMaskinportenFeil(int status) {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("maskinporten/maskinporten_feil.json")));
	}

	private void stubPutOppdaterForsendelse() {
		stubFor(put(urlEqualTo("/rest/v1/administrerforsendelse/oppdaterforsendelse"))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	void stubNaisTexasToken() {
		stubFor(post("/texas-token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("nais-texas/texas_response.json")));
	}

	private void stubPostSafJournalpost(String bodyFileName) {
		stubFor(post(urlMatching("/saf/graphql"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyFileName)));
	}

	private void stubGetVarselInfo(String path) {
		stubFor(get(urlMatching(TKAT021_URL + VARSEL_TYPE_ID))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokmet/" + path)));
	}

	private void stubPutVarselInfo() {
		stubFor(put(urlMatching(OPPDATERVARSELINFO_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubGetDokumentTypeInfo(String bodyFileName) {
		stubFor(get(urlMatching(TKAT020_URL + DOKUMENTTYPE_ID_HOVEDDOK))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokmet/" + bodyFileName)));
	}

	private void stubGetDigipostDigitalKontaktInformasjon(int status) {
		stubFor(post("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("digitalkontaktinformasjonv1/dki-digipost-happy.json")));
	}

	private void stubGetEBoksDigitalKontaktInformasjon(int status) {
		stubFor(post("/digdir/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("digitalkontaktinformasjonv1/dki-eboks.json")));
	}

	void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubGetHentForsendelse(String responsebody, int httpStatusvalue) {
		stubFor(get(HENTFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(httpStatusvalue)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString(responsebody))));
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = session.createTextMessage();
			msg.setText(message);
			if (callId != null) {
				msg.setStringProperty("callId", callId);
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
