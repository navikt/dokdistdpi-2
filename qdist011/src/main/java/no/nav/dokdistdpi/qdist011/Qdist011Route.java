package no.nav.dokdistdpi.qdist011;

import com.ibm.msg.client.jms.DetailedJMSException;
import no.nav.dokdistdpi.common.IdsProcessor;
import no.nav.dokdistdpi.consumer.dpi.DpiMeldingsformidler;
import no.nav.dokdistdpi.consumer.rdist001.DokdistAdministrerForsendelseUpdater;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusExpedertKanIkkeDistribuereException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.QDIST011_SERVICE_ID;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * @author Tsigab A. Gebremedhin, NAV
 */
@Component
public class Qdist011Route extends RouteBuilder {

	private final Queue qdist011;
	private final Queue qdist011FunksjonellFeil;
	private final Qdist011Service qdist011Service;
	private final DpiMeldingsformidler dpiMeldingsformidler;
	private final Qdist011MetricsRoutePolicy routePolicy;
	private final DokdistAdministrerForsendelseUpdater administrerForsendelseUpdater;

	@Autowired
	public Qdist011Route(Queue qdist011, Queue qdist011FunksjonellFeil,
						 Qdist011Service qdist011Service, DpiMeldingsformidler dpiMeldingsformidler,
						 Qdist011MetricsRoutePolicy routePolicy, DokdistAdministrerForsendelseUpdater administrerForsendelseUpdater) {
		this.qdist011 = qdist011;
		this.qdist011FunksjonellFeil = qdist011FunksjonellFeil;
		this.qdist011Service = qdist011Service;
		this.dpiMeldingsformidler = dpiMeldingsformidler;
		this.routePolicy = routePolicy;
		this.administrerForsendelseUpdater = administrerForsendelseUpdater;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(true)
				.loggingLevel(ERROR));

		onException(ForsendelseStatusExpedertKanIkkeDistribuereException.class)
				.handled(true).logExhaustedMessageBody(false)
				.log(INFO, log, "${exception}" + logForsendelseId());

		onException(AbstractDokdistdpiFunctionalException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.logExhaustedMessageBody(false)
				.log(WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist011FunksjonellFeil.getQueueName());

		//Egen håndtering av denne type feil for å unngå logging av hele brev i prod
		onException(DetailedJMSException.class)
				.log(WARN, "DetailedJMSException oppstått i qdist011 for forsendelse med " + getIdsForLogging() + ". Melding sendt til funksjonell feilkø.")
				.useOriginalMessage()
				.logExhaustedMessageBody(false)
				.logExhaustedMessageHistory(false)
				.logStackTrace(false)
				.handled(true)
				.to("jms:" + qdist011FunksjonellFeil.getQueueName());

		from("jms:" + qdist011.getQueueName() + "?transacted=true&concurrentConsumers=1")
				.routeId(QDIST011_SERVICE_ID)
				.routePolicy(routePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.process(new IdsProcessor())
				.log(INFO, log, "qdist011 har mottatt forsendelse med " + logForsendelseId())
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.bean(qdist011Service)
				.bean(dpiMeldingsformidler)
				.choice()
				.when(simple("${body}").isEqualTo(CREATED))
					.log(LoggingLevel.INFO, log, "qdist011 har sendt forsendelse med " + getIdsForLogging() + " til DPI")
					.bean(administrerForsendelseUpdater, "updateStatusAndConversationId")
					.log(LoggingLevel.INFO, log, "qdist011 har oppdatert dokdistDb med forsendelseStatus=OVERSENDT og konversasjonId=${exchangeProperty." + PROPERTY_CONVERSATION_ID + "} og avslutter behandling av forsendelse med " + getIdsForLogging())
				.endChoice()
				.end();
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"konversasjonId=${exchangeProperty." + PROPERTY_CONVERSATION_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}

	public static String logForsendelseId() {
		return "forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
