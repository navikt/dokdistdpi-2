package no.nav.dokdistdpi.qdist014;

import no.nav.dokdistdpi.common.MDCHeaderProcessor;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.qdist014.map.ForretningsKvitteringMapper;
import no.nav.dokdistdpi.qdist014.metrics.Qdist014MetricsRoutePolicy;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import java.nio.charset.StandardCharsets;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_STATUS;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_KVITTERING_LEVERT;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

@Component
public class Qdist014Route extends RouteBuilder {

	private static final String SERVICE_ID = "qdist014";
	private static final String BEHANDLINGEN_AVSLUTTES = "Behandling av kvitteringen avsluttes, ";

	private final Qdist014Service qdist014Service;
	private final Queue qdist014;
	private final Queue qdist009;
	private final Queue qdist014FunksjonellFeil;
	private final Qdist014MetricsRoutePolicy qdist014MetricsRoutePolicy;
	private final ForretningsKvitteringMapper forretningsKvitteringMapper;
	private final OppdaterForsendelseStatus oppdaterForsendelseStatus;
	private final DpiKvitteringService dpiKvitteringService;

	@Autowired
	public Qdist014Route(CamelContext context, Qdist014Service qdist014Service,
						 Queue qdist014, Queue qdist009, Queue qdist014FunksjonellFeil,
						 Qdist014MetricsRoutePolicy qdist014MetricsRoutePolicy,
						 ForretningsKvitteringMapper forretningsKvitteringMapper,
						 OppdaterForsendelseStatus oppdaterForsendelseStatus,
						 DpiKvitteringService dpiKvitteringService) {
		super(context);
		this.qdist014Service = qdist014Service;
		this.qdist014 = qdist014;
		this.qdist009 = qdist009;
		this.qdist014FunksjonellFeil = qdist014FunksjonellFeil;
		this.qdist014MetricsRoutePolicy = qdist014MetricsRoutePolicy;
		this.forretningsKvitteringMapper = forretningsKvitteringMapper;
		this.oppdaterForsendelseStatus = oppdaterForsendelseStatus;
		this.dpiKvitteringService = dpiKvitteringService;
	}

	@Override
	public void configure() throws Exception {

		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0).log(log)
				.logExhaustedMessageBody(false)
				.logStackTrace(true).loggingLevel(ERROR));

		onException(AbstractDokdistdpiFunctionalException.class, ValidationException.class, IllegalArgumentException.class)
				.handled(true)
				.useOriginalMessage().log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist014FunksjonellFeil.getQueueName());

		from("jms:" + qdist014.getQueueName() + "?transacted=true")
				.routeId(SERVICE_ID)
				.routePolicy(qdist014MetricsRoutePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.process(new MDCHeaderProcessor())
				.log(INFO, log, "qdist014 har mottatt kvittering fra sdist003")
				.choice()
					.when(method(forretningsKvitteringMapper, "erKvitteringBehandlet").isEqualTo(true))
						.log(INFO, log, BEHANDLINGEN_AVSLUTTES)
					.endChoice()
					.when(method(dpiKvitteringService, "erStatusEkspedertOrReturOrFeilet").isEqualTo(true))
						.log(INFO, log, BEHANDLINGEN_AVSLUTTES + "forsendelseStatus=${exchangeProperty." + PROPERTY_FORSENDELSE_STATUS + "}")
					.endChoice()
				.otherwise()
					.bean(forretningsKvitteringMapper)
					.choice()
						.when(exchangeProperty(PROPERTY_KVITTERING_LEVERT).isEqualTo(KvitteringType.LEVERING))
						.bean(oppdaterForsendelseStatus)
						.log(INFO, log,"qdist014 har oppdatert forsendelse med " + getIdsForLogging() + "til EKSPEDERT")
						.endChoice()
					.otherwise()
						.bean(qdist014Service)
						.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
						.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
						.to("jms:" + qdist009.getQueueName())
						.log(INFO, log,"qdist014 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist009 for distribusjon av forsendelse")
					.endChoice()
				.end();
	}

	private static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "} og " +
				"konversasjonsId=${exchangeProperty." + PROPERTY_CONVERSATION_ID + "}";
	}
}
