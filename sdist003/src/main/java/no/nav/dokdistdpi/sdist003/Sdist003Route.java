package no.nav.dokdistdpi.sdist003;

import no.nav.dokdistdpi.config.prop.DokdistDpiProperties;
import no.nav.dokdistdpi.consumer.lederelection.LederElectionConsumer;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Sdist003Route extends RouteBuilder {

	private static final String ROUTEID = "sdist003";

	private static final String FUNCTIONAL_ERROR_HANDLER = "FUNCTIONAL_ERROR_HANDLER";
	private static final String TECHNICAL_ERROR_HANDLER = "TECHNICAL_ERROR_HANDLER";
	private static final String UNKNOWN_ERROR_HANDLER = "UNKNOWN_ERROR_HANDLER";

	private final LederElectionConsumer lederElection;
	private final Sdist003Service sdist003Service;
	private final DokdistDpiProperties.Sdist003 sdist003Properties;

	@Autowired
	public Sdist003Route(CamelContext context,
						 LederElectionConsumer lederElection, Sdist003Service sdist003Service,
						 DokdistDpiProperties dokdistDpiProperties) {
		super(context);
		this.lederElection = lederElection;
		this.sdist003Service = sdist003Service;
		this.sdist003Properties = dokdistDpiProperties.getSdist003();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void configure() {

		onException(AbstractDokdistdpiFunctionalException.class, RuntimeCamelException.class)
				.id(FUNCTIONAL_ERROR_HANDLER)
				.handled(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet funksjonelt" + ". ${exception}.");

		onException(AbstractDokdistdpiTechnicalException.class, IOException.class)
				.id(TECHNICAL_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet teknisk" + ". ${exception}.");

		onException(Exception.class)
				.id(UNKNOWN_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(LoggingLevel.ERROR, log, "Sdist003 feilet med ukjent feil" + ". ${exception}.");

		from(sdist003Properties.camelUri())
				.routeId(ROUTEID + "-dpiScheduler")
				.autoStartup(sdist003Properties.isAutostartup())
				.process(new Sdist003HeaderProcessor())
				.setExchangePattern(ExchangePattern.InOnly)
				.choice()
					.when(method(lederElection, "isLeader").isEqualTo(true))
						.setExchangePattern(ExchangePattern.InOnly)
						.bean(sdist003Service)
						.choice()
							.when(simple("${body}").isEqualTo(null))
								.log(LoggingLevel.INFO, log, "Sdist003 fant ingen kvitteringer fra DPI.")
								.setProperty(Exchange.SCHEDULER_POLLED_MESSAGES, constant(false))
						.endChoice()
				.end();
	}
}
