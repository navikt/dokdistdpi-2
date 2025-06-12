package no.nav.dokdistdpi.sdist003;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObject;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.dpi.client.DpiClient;
import no.nav.dokdistdpi.consumer.dpi.client.HentKvitteringResponse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JmsTechnicalException;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import no.nav.dokdistdpi.slack.SlackService;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;

@Slf4j
@Component
public class Sdist003Service {

	private static final int MAX_PAGES = 100;
	private final DpiClientProperties dpiClientProperties;
	private final DpiClient dpiClient;
	private final ProducerTemplate producerTemplate;
	private final Queue qdist014;
	private final ObjectMapper dpiObjectMapper;
	private final SlackService slackService;

	public Sdist003Service(DpiClientProperties dpiClientProperties,
						   DpiClient dpiClient,
						   ProducerTemplate producerTemplate,
						   Queue qdist014,
						   ObjectMapper dpiObjectMapper,
						   SlackService slackService) {
		this.dpiClientProperties = dpiClientProperties;
		this.dpiClient = dpiClient;
		this.producerTemplate = producerTemplate;
		this.qdist014 = qdist014;
		this.dpiObjectMapper = dpiObjectMapper;
		this.slackService = slackService;
	}

	public Flux<String> behandleKvitteringer() {
		return hentKvitteringer()
				.map(this::getForretningsmeldingFromJwt)
				.map(this::mapPayloadToSimpleSbd)
				.flatMap(kvittering -> wrapBlocking(() -> sendQdist014Melding(kvittering)))
				.flatMap(this::markerKvitteringMottatt)
				.onErrorResume(e -> {
					var feilmelding = "Sdist003 feilet under behandling av kvitteringer med feilmelding=%s".formatted(e.getMessage());
					log.error(feilmelding, e);
					slackService.sendMelding("Sdist003 feilet under behandling av kvitteringer med exception=%s".formatted(e.getClass().getName()));
					return Mono.empty();
				});
	}

	public Flux<HentKvitteringResponse> hentKvitteringer() {
		AtomicInteger page = new AtomicInteger(0);
		return Flux.defer(() -> dpiClient.hentKvitteringerAsync(page.getAndIncrement()))
				.repeatWhen(kvitteringer -> kvitteringer
						.doOnNext(antallKvitteringer -> log.info("Sdist003 hentet antallKvitteringer={} fra DPI, page={}", antallKvitteringer, page.get() - 1))
						.takeWhile(antallKvitteringer -> scheduleNewPagePredicate(antallKvitteringer, page)));
	}

	private boolean scheduleNewPagePredicate(Long antallKvitteringer, AtomicInteger page) {
		return antallKvitteringer == dpiClientProperties.getPagesize() && page.get() < MAX_PAGES;
	}

	private String getForretningsmeldingFromJwt(HentKvitteringResponse hentKvitteringResponse) {
		try {
			return JOSEObject.parse(hentKvitteringResponse.getForretningsmelding()).getPayload().toString();
		} catch (ParseException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	private Kvittering mapPayloadToSimpleSbd(String forretningsmeldingPayload) {
		try {
			Kvittering kvittering = new Kvittering(dpiObjectMapper.readValue(forretningsmeldingPayload, SimpleStandardBusinessDocument.class), forretningsmeldingPayload);
			String konversasjonId = kvittering.simpleSbd().getConversationId();
			String type = kvittering.simpleSbd().getType();
			log.info("Sdist003 har mottatt kvittering fra dpi aksesspunkt. konversasjonId={} og type={}", konversasjonId, type);
			return kvittering;
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe kvittering forretningsmelding payload til SimpleStandardBusinessDocument", e);
		}
	}

	private Kvittering sendQdist014Melding(Kvittering kvittering) {
		try {
			String konversasjonId = kvittering.simpleSbd().getConversationId();
			producerTemplate.sendBodyAndHeader("jms:" + qdist014.getQueueName(), kvittering.forretningsmelding(), CALL_ID, konversasjonId);
			log.info("Sdist003 har skrevet melding på qdist014. konversasjonId={}", konversasjonId);
			return kvittering;
		} catch (JMSException e) {
			throw new JmsTechnicalException("Kunne ikke skrive melding til qdist014", e);
		}
	}

	// https://projectreactor.io/docs/core/snapshot/reference/faq.html#faq.wrap-blocking
	private static <T> Mono<T> wrapBlocking(Callable<T> callable) {
		return Mono.fromCallable(callable)
				.subscribeOn(Schedulers.boundedElastic());
	}

	private Mono<String> markerKvitteringMottatt(Kvittering kvittering) {
		return dpiClient.markerKvitteringMottattAsync(kvittering.simpleSbd().getDokumentKonversasjonId())
				.doOnSuccess(unused -> log.info("Sdist003 har markert innkommende forsendelse som mottatt av avsender. konversasjonId={}, messageId={}",
						kvittering.simpleSbd().getConversationId(), kvittering.simpleSbd().getDokumentKonversasjonId()));
	}

}
