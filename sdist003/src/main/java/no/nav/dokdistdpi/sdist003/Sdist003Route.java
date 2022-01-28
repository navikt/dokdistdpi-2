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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Sdist003Route extends RouteBuilder {

	private static final String ROUTE_SDIST003_AVVIK_ID = "sdist003Avvik";
	public static final String ROUTE_SDIST003_AVVIK = "direct:" + ROUTE_SDIST003_AVVIK_ID;
	private static final String ROUTEID = "sdist003";
	public static final String SDIST003_NORMAL_ROUTE = "direct:sdist003-normal";

	private static final String FUNCTIONAL_ERROR_HANDLER = "FUNCTIONAL_ERROR_HANDLER";
	private static final String TECHNICAL_ERROR_HANDLER = "TECHNICAL_ERROR_HANDLER";
	private static final String UNKNOWN_ERROR_HANDLER = "UNKNOWN_ERROR_HANDLER";

	private final LederElectionConsumer lederElection;
	private final Sdist003Service sdist003Service;
	private final DpiClientProperties dpiClientProperties;

	@Autowired
	public Sdist003Route(CamelContext context,
						 LederElectionConsumer lederElection, Sdist003Service sdist003Service,
						 DpiClientProperties dpiClientProperties) {
		super(context);
		this.lederElection = lederElection;
		this.sdist003Service = sdist003Service;
		this.dpiClientProperties = dpiClientProperties;
	}

	@Override
	public void configure() {

		onException(AbstractDokdistdpiFunctionalException.class, RuntimeCamelException.class)
				.id(FUNCTIONAL_ERROR_HANDLER)
				.handled(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet funksjonelt" + ". ${exception}.")
				.to(ROUTE_SDIST003_AVVIK);

		onException(AbstractDokdistdpiTechnicalException.class, IOException.class)
				.id(TECHNICAL_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet teknisk" + ". ${exception}.")
				.to(ROUTE_SDIST003_AVVIK);

		onException(Exception.class)
				.id(UNKNOWN_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet med ukjent feil" + ". ${exception}.")
				.to(ROUTE_SDIST003_AVVIK);

		from("scheduler://dpiScheduler?delay=" + dpiClientProperties.getDpischeduler())
				.autoStartup(dpiClientProperties.isAutoStartup())
				.routeId(ROUTEID + "-dpiScheduler")
				.onCompletion()
					.to(SDIST003_NORMAL_ROUTE)
				.end()
				.to("log:avsluttes");

		from(SDIST003_NORMAL_ROUTE)
				.routeId("sdist003-normal")
				.process(new Sdist003HeaderProcessor())
				.setExchangePattern(ExchangePattern.InOnly)
				.choice()
					.when(method(lederElection, "isLeader").isEqualTo(true))
						.setExchangePattern(ExchangePattern.InOnly)
						.bean(sdist003Service)
						.choice()
							.when(simple("${body}").isEqualTo(null))
								.log(LoggingLevel.INFO, log, "Sdist003 fant ingen kvitteringer fra DPI.")
								.delay(dpiClientProperties.getPullinterval())
						.endChoice()
				.endChoice()
				.end();

		from(ROUTE_SDIST003_AVVIK)
				.routeId(ROUTE_SDIST003_AVVIK_ID)
				.log(LoggingLevel.OFF, "avvikshåndtering");
	}
}
