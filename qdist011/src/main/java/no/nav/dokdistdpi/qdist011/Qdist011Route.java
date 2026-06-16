package no.nav.dokdistdpi.qdist011;

import com.ibm.msg.client.jakarta.jms.DetailedJMSException;
import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import no.nav.dokdistdpi.common.MDCHeaderProcessor;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.dpi.DpiMeldingsformidler;
import no.nav.dokdistdpi.consumer.rdist001.DokdistAdministrerForsendelseUpdater;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.exception.functional.BrukerHarIngenDigitalpostkasseException;
import no.nav.dokdistdpi.exception.functional.BrukerReservertMotDigitalpostkasseException;
import no.nav.dokdistdpi.exception.functional.ForsendelseStatusEkspedertKanIkkeDistribueresException;
import no.nav.dokdistdpi.exception.functional.UtenforKjernetidFunctionalException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.consumer.dpi.client.StatusType.OPPRETTET;
import static no.nav.dokdistdpi.consumer.dpi.client.StatusType.SENDT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.QDIST011_SERVICE_ID;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;
import static org.apache.camel.support.builder.PredicateBuilder.or;

@Component
public class Qdist011Route extends RouteBuilder {

	private final DokdistdpiProperties.Qdist011 qdist011Properties;
	private final Queue qdist011;
	private final Queue qdist011FunksjonellFeil;
	private final Queue qdist011UtenforKjernetid;
	private final Qdist011Service qdist011Service;
	private final DpiMeldingsformidler dpiMeldingsformidler;
	private final DistribuerTilPrintService distribuerTilPrintService;
	private final DokdistAdministrerForsendelseUpdater administrerForsendelseUpdater;

	@Autowired
	public Qdist011Route(DokdistdpiProperties dokdistDpiProperties,
						 Queue qdist011,
						 Queue qdist011FunksjonellFeil,
						 Queue qdist011UtenforKjernetid,
						 Qdist011Service qdist011Service,
						 DpiMeldingsformidler dpiMeldingsformidler,
						 DistribuerTilPrintService distribuerTilPrintService,
						 DokdistAdministrerForsendelseUpdater administrerForsendelseUpdater) {
		this.qdist011Properties = dokdistDpiProperties.getQdist011();
		this.qdist011 = qdist011;
		this.qdist011FunksjonellFeil = qdist011FunksjonellFeil;
		this.qdist011UtenforKjernetid = qdist011UtenforKjernetid;
		this.qdist011Service = qdist011Service;
		this.dpiMeldingsformidler = dpiMeldingsformidler;
		this.distribuerTilPrintService = distribuerTilPrintService;
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

		onException(ForsendelseStatusEkspedertKanIkkeDistribueresException.class)
				.handled(true).logExhaustedMessageBody(false)
				.log(INFO, log, "${exception} " + logForsendelseId());

		onException(UtenforKjernetidFunctionalException.class)
				.handled(true)
				.useOriginalMessage()
				.log(INFO, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist011UtenforKjernetid.getQueueName());

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

		onException(BrukerHarIngenDigitalpostkasseException.class, BrukerReservertMotDigitalpostkasseException.class)
				.log(WARN, "Bruker mangler digital postkasse og forsendelse med" + getIdsForLogging() + "distribuerer til PRINT.")
				.useOriginalMessage()
				.handled(true)
				.setBody(exchangeProperty(PROPERTY_FORSENDELSE_ID))
				.bean(distribuerTilPrintService)
				.end();

		from("jms:" + qdist011.getQueueName() + "?transacted=true&concurrentConsumers=1")
				.autoStartup(qdist011Properties.isAutostartup())
				.routeId(QDIST011_SERVICE_ID)
				.setExchangePattern(InOnly)
				.process(new MDCHeaderProcessor())
				.log(INFO, log, "qdist011 har mottatt forsendelse med " + logForsendelseId())
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/out/distribuertilkanal.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.bean(qdist011Service)
				.bean(dpiMeldingsformidler)
				.choice()
					.when(or(simple("${body.status}").isEqualTo(SENDT), simple("${body.status}").isEqualTo(OPPRETTET)))
						.log(INFO, log, "qdist011 har sendt forsendelse med " + getIdsForLogging() + " til DPI")
						.bean(administrerForsendelseUpdater, "updateStatusDigitalpostkasseInfoAndVarselInfo")
						.log(INFO, log, "qdist011 har oppdatert varselInfo, forsendelseStatus=OVERSENDT og avslutter behandling av forsendelse med " + getIdsForLogging())
					.otherwise()
						.log(WARN, log, "qdist011 kan ikke oppdatere status til OVERSENDT for forsendelse fra DPI med " + getIdsForLogging())
				.end();
		//@formatter:on
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
