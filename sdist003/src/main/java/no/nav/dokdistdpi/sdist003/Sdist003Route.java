package no.nav.dokdistdpi.sdist003;

import no.nav.dokdistdpi.common.MDCHeaderProcessor;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.consumer.lederelection.LederElectionConsumer;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import java.io.IOException;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HENT_KVITTERING_STATUS_CODE;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@Component
public class Sdist003Route extends RouteBuilder {

	private static final String JURIDISKLOGG_ROUTE_ID = "juridiskLogg";
	public static final String ROUTE_JURIDISKLOGG = "direct:" + JURIDISKLOGG_ROUTE_ID;
	public static final String ROUTE_QDIST014_ID = "qdist014route";
	public static final String ROUTE_QDIST014 = "direct:" + ROUTE_QDIST014_ID;
	private final String ROUTEID = "SDIST003";
	private final String FUNCTIONAL_ERROR_HANDLER = "FUNCTIONAL_ERROR_HANDLER";
	private final String TECHNICAL_ERROR_HANDLER = "TECHNICAL_ERROR_HANDLER";
	private final String UNKNOWN_ERROR_HANDLER = "UNKNOWN_ERROR_HANDLER";

	private final LederElectionConsumer lederElection;
	private final Queue qdist014;
	private final Sdist003Service sdist003Service;
	private final DpiClientProperties dpiClientProperties;
	private final LagreJuridiskLoggService lagreJuridiskLoggService;

	public Sdist003Route(CamelContext context,
						 LederElectionConsumer lederElection, Queue qdist014, Sdist003Service sdist003Service,
						 DpiClientProperties dpiClientProperties, LagreJuridiskLoggService lagreJuridiskLoggService) {
		super(context);
		this.lederElection = lederElection;
		this.qdist014 = qdist014;
		this.sdist003Service = sdist003Service;
		this.dpiClientProperties = dpiClientProperties;
		this.lagreJuridiskLoggService = lagreJuridiskLoggService;
	}

	@Override
	public void configure() {

		onException(AbstractDokdistdpiFunctionalException.class, RuntimeCamelException.class)
				.id(FUNCTIONAL_ERROR_HANDLER)
				.handled(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet funksjonelt" + ". ${exception}.")
				.to("direct:sdist003Avvik");

		onException(AbstractDokdistdpiTechnicalException.class, IOException.class)
				.id(TECHNICAL_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet teknisk" + ". ${exception}.")
				.to("direct:sdist003Avvik");

		onException(Exception.class)
				.id(UNKNOWN_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet med ukjent feil" + ". ${exception}.")
				.to("direct:sdist003Avvik");

		from("scheduler://dpiScheduler?delay=" + dpiClientProperties.getDpischeduler())
				.autoStartup(dpiClientProperties.isAutoStartup())
				.routeId(ROUTEID + "-dpiScheduler")
				.choice()
					.when(method(lederElection, "isLeader").isEqualTo(true))
						.process(new MDCHeaderProcessor())
						.setExchangePattern(ExchangePattern.InOnly)
						.bean(sdist003Service)
						.choice()
							.when(exchangeProperty(HENT_KVITTERING_STATUS_CODE).isEqualTo(NO_CONTENT))
							.log(LoggingLevel.INFO, log, "Sdist003 fant ingen kvitteringer fra DPI.")
							.delay(dpiClientProperties.getPullinterval())
						.endChoice()
				.endChoice()
				.end();

		from("direct:sdist003Avvik")
				.routeId(ROUTEID + "-avvik")
				.log(LoggingLevel.OFF, "avvikshåndtering");
	}
}
