package no.nav.dokdistdpi.qdist011.itest;

import com.github.tomakehurst.wiremock.admin.model.ListStubMappingsResult;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import no.nav.dokdistdpi.cloudstorage.DokDistDokumentFraBucket;
import no.nav.dokdistdpi.cloudstorage.EncryptedBucketStorage;
import no.nav.dokdistdpi.cloudstorage.JsonSerializer;
import no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.STS_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdpi.qdist011.TestUtil.classpathToString;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@ExtendWith(SpringExtension.class)
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

	@Autowired
	@Lazy
	private EncryptedBucketStorage encryptedBucketStorage;

	@Autowired
	private CacheManager cacheManager;
	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist011;

	@Autowired
	private Queue qdist011FunksjonellFeil;

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
		stubGetDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("dokumentinfov4/tkat020-happy.json");
		stubGetVarselInfo();
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPostSecurityToken();
		stubPutForsendelseStatusAndkonversasjonsId();
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", FORSENDELSE_ID, OK.value());
		stubPostMaskinporten();
		stubPostDPISend();
		stubGetDPIStatus();
		stubPutVarselInfo();
		stubPutAdministrerforsendelseOppdatertForsendelsestatusAndkonvId();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
			verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
			verify(1, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")));
			verify(1, postRequestedFor(urlEqualTo("/securitytoken?grant_type=client_credentials&scope=openid")));
			verify(1, postRequestedFor(urlEqualTo("/safgraphql")));
			verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
			verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse/oppdaterdigitalinfo")));
			verify(1, putRequestedFor(urlEqualTo("/rest/administrerforsendelse/oppdatervarselinfo")));
		});
	}

	@Test
	void shouldHandleForsendelseOversendtWhenDuplikatForsendelse() {
		stubAzure();
		stubGetDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("dokumentinfov4/tkat020-happy.json");
		stubGetVarselInfo();
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPostSecurityToken();
		stubPutOppdaterDigitalLeverandoerAndPostkasseadresse();
		stubPutOgOppdaterKonversasjonsId(OK.value());
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", FORSENDELSE_ID, OK.value());
		stubPostMaskinporten();
		stubPostDPIDuplicate();
		stubGetDPIStatus();
		stubPutVarselInfo();
		stubPutAdministrerforsendelseOppdatertForsendelsestatusAndkonvId();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
			verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
			verify(1, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
			verify(1, postRequestedFor(urlEqualTo("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")));
			verify(1, postRequestedFor(urlEqualTo("/securitytoken?grant_type=client_credentials&scope=openid")));
			verify(1, postRequestedFor(urlEqualTo("/safgraphql")));
			verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
			verify(1, putRequestedFor(urlEqualTo("/administrerforsendelse/oppdaterdigitalinfo")));
		});
	}

	@SneakyThrows
	@Test
	public void shouldThrowValideringsfeilException() {
		stubAzure();
		stubGetDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("dokumentinfov4/tkat020-happy.json");
		stubGetVarselInfo();
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPostSecurityToken();
		stubPutForsendelseStatusAndkonversasjonsId();
		stubPutOgOppdaterKonversasjonsId(OK.value());
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", FORSENDELSE_ID, OK.value());
		stubPostMaskinporten();
		stubPostDPISend(BAD_REQUEST.value());
		stubPutAdministrerforsendelseOppdatertForsendelsestatusAndkonvId();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist011FunksjonellFeil);
			assertNotNull(response);
		});
		verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
		verify(1, getRequestedFor(urlEqualTo("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)));
		verify(1, getRequestedFor(urlEqualTo("/varselinfo/" + VARSEL_TYPE_ID)));
		verify(1, postRequestedFor(urlEqualTo("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")));
		verify(1, postRequestedFor(urlEqualTo("/safgraphql")));
		verify(1, postRequestedFor(urlEqualTo("/message/out?kanal=dokdistdpi-t")));
	}

	@SneakyThrows
	@Test
	public void shouldThrowExceptionIfMaskineportenIsNull() {
		stubAzure();
		stubGetDigitalKontaktInformasjon(OK.value());
		stubGetDokumentTypeInfo("dokumentinfov4/tkat020-happy.json");
		stubGetVarselInfo();
		stubPostSafJournalpost("saf/safGraphQlResponse-happy.json");
		stubPostSecurityToken();
		stubPutForsendelseStatusAndkonversasjonsId();
		stubPutOgOppdaterKonversasjonsId(OK.value());
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", FORSENDELSE_ID, OK.value());
		stubPostMaskinportenFeil(BAD_REQUEST.value());
		stubPostDPISend();
		stubPutAdministrerforsendelseOppdatertForsendelsestatusAndkonvId();

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist011FunksjonellFeil);
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));

		});
		verify(1, postRequestedFor(urlEqualTo("/maskinporten")));
	}

	@SneakyThrows
	@Test
	public void shouldThrowAdministrerforsendelseNotFoundException() {
		stubAzure();
		stubGetHentForsendelse("__files/rdist001/getForsendelse-resending.json", FORSENDELSE_ID, NOT_FOUND.value());

		sendStringMessage(qdist011, classpathToString("__files/qdist011/qdist011-happy.xml"), null);
		ListStubMappingsResult stubs = listAllStubMappings();
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(1, getRequestedFor(urlEqualTo("/administrerforsendelse/" + FORSENDELSE_ID)));
		});
	}

	private void stubPostDPISend() {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(CREATED.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/dpi/dpi_out_status.json"))));
	}

	private void stubPostDPIDuplicate() {
		stubFor(post(urlEqualTo("/message/out?kanal=dokdistdpi-t"))
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/dpi/dpi_out_duplicate.json"))));
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
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/maskinporten/maskinporten_happy_response.json"))));

	}

	private void stubPostMaskinportenFeil(int status) {
		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse().withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/maskinporten/maskinporten_feil.json"))));

	}

	private void stubPutOgOppdaterKonversasjonsId(int statusValue) {
		stubFor(put(urlPathMatching("/administrerforsendelse?(.*?)"))
				.willReturn(aResponse().withStatus(statusValue).withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubPutForsendelseStatusAndkonversasjonsId() {
		stubFor(put(urlEqualTo("/administrerforsendelse?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=OVERSENDT&konversasjonsId=" + KONVERSASJON_ID))
				.willReturn(aResponse().withStatus(OK.value())));
	}

	private void stubPutOppdaterDigitalLeverandoerAndPostkasseadresse() {
		stubFor(put(urlEqualTo("/administrerforsendelse/oppdaterdigitalinfo"))
				.willReturn(aResponse().withStatus(OK.value())));
	}

	private void stubPostSecurityToken() {
		stubFor(post("/securitytoken?grant_type=client_credentials&scope=openid")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/sts/stsResponse-happy.json"))));
	}

	private void stubPostSafJournalpost(String bodyFileName) {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile(bodyFileName)));
	}

	private void stubGetVarselInfo() {
		stubFor(get(urlMatching("/varselinfo/" + VARSEL_TYPE_ID)).willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("varselinfov1/tkat021-happy.json")));
	}

	private void stubPutVarselInfo() {
		stubFor(put(urlMatching("/rest/administrerforsendelse/oppdatervarselinfo"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));
	}

	private void stubGetDokumentTypeInfo(String bodyFileName) {
		stubFor(get(urlMatching("/dokumenttypeinfo/" + DOKUMENTTYPE_ID_HOVEDDOK)).willReturn(aResponse().withStatus(OK
						.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile(bodyFileName)));
	}

	private void stubGetDigitalKontaktInformasjon(int status) {
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/digitalkontaktinformasjonv1/dki-happy.json"))));
	}

	private void stubPutAdministrerforsendelseOppdatertForsendelsestatusAndkonvId() {
		stubFor(put(urlMatching("/administrerforsendelse\\?forsendelseId=" + FORSENDELSE_ID + "\\&forsendelseStatus=OVERSENDT\\&konversasjonsId=.*"))
				.willReturn(aResponse().withStatus(OK.value())));
	}

	void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubGetHentForsendelse(String responsebody, String forsendelseId, int httpStatusvalue) {
		stubFor(get("/administrerforsendelse/" + forsendelseId).willReturn(aResponse().withStatus(httpStatusvalue)
				.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBody(classpathToString(responsebody))));
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
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
