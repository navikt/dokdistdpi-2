package no.nav.dokdistdpi.sdist005;

import jakarta.jms.Queue;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.consumer.lederelection.LeaderElectionConsumer;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import no.nav.dokdistdpi.exception.technical.AbstractDokdistdpiTechnicalException;
import no.nav.dokdistdpi.slack.SlackService;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static jakarta.xml.bind.JAXBContext.newInstance;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

@Component
public class Sdist005Route extends RouteBuilder {

	private static final String ROUTEID = "sdist005";

	private static final String FUNCTIONAL_ERROR_HANDLER = "FUNCTIONAL_ERROR_HANDLER";
	private static final String TECHNICAL_ERROR_HANDLER = "TECHNICAL_ERROR_HANDLER";
	private static final String UNKNOWN_ERROR_HANDLER = "UNKNOWN_ERROR_HANDLER";
	private static final String LOGG_OG_SEND_SLACKMELDING_RUTE = "direct:loggOgSendSlackmelding";

	private final LeaderElectionConsumer leaderElectionConsumer;
	private final Sdist005Service sdist005Service;
	private final Queue qdist009;
	private final DokdistdpiProperties.Sdist005 sdist005Properties;
	private final SlackService slackService;

	public Sdist005Route(CamelContext context,
						 LeaderElectionConsumer leaderElectionConsumer,
						 Sdist005Service sdist005Service,
						 Queue qdist009,
						 DokdistdpiProperties dokdistDpiProperties,
						 SlackService slackService) {
		super(context);
		this.leaderElectionConsumer = leaderElectionConsumer;
		this.sdist005Service = sdist005Service;
		this.qdist009 = qdist009;
		this.sdist005Properties = dokdistDpiProperties.getSdist005();
		this.slackService = slackService;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void configure() throws Exception {

		onException(AbstractDokdistdpiFunctionalException.class, RuntimeCamelException.class)
				.id(FUNCTIONAL_ERROR_HANDLER)
				.handled(true)
				.log(ERROR, log, "Sdist005 feilet funksjonelt med feilmelding=${exception}.")
				.setBody(simple("Sdist005 feilet funksjonelt med exception=${exception.getClass().getName()}."))
				.to(LOGG_OG_SEND_SLACKMELDING_RUTE);

		onException(AbstractDokdistdpiTechnicalException.class, IOException.class)
				.id(TECHNICAL_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(ERROR, log, "Sdist005 feilet teknisk med feilmelding=${exception}.")
				.setBody(simple("Sdist005 feilet teknisk med exception=${exception.getClass().getName()}."))
				.to(LOGG_OG_SEND_SLACKMELDING_RUTE);

		onException(Exception.class)
				.id(UNKNOWN_ERROR_HANDLER)
				.handled(true)
				.logStackTrace(true)
				.log(ERROR, log, "Sdist005 feilet med ukjent feil og feilmelding=${exception}.")
				.setBody(simple("Sdist005 feilet med ukjent feil og exception=${exception.getClass().getName()}."))
				.to(LOGG_OG_SEND_SLACKMELDING_RUTE);

		from(LOGG_OG_SEND_SLACKMELDING_RUTE)
				.bean(slackService, "sendMelding(${body})");

		from(sdist005Properties.camelUri())
				.routeId(ROUTEID + "-dpiScheduler")
				.autoStartup(sdist005Properties.isAutostartup())
				.process(new Sdist005HeaderProcessor())
				.setExchangePattern(InOnly)
				.choice()
					.when(method(leaderElectionConsumer, "isLeader").isEqualTo(true))
						.setExchangePattern(InOnly)
						.bean(sdist005Service)
						.choice()
							.when(simple("${body.size}").isEqualTo(0))
								.log(INFO, log, "Sdist005 fant ingen feilede forsendelser")
						.otherwise()
							.split(simple("${body}"))
								.setProperty(PROPERTY_FORSENDELSE_ID, simple("${body.forsendelseId}"))
								.marshal(new JaxbDataFormat(newInstance(DistribuerTilKanal.class)))
								.convertBodyTo(String.class, UTF_8.toString())
								.to("jms:" + qdist009.getQueueName())
								.log(INFO, log, "sdist005 har lagt forsendelse med forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "} på kø til qdist009 for distribusjon av forsendelse til print")
							.end()
						.endChoice()
				.end();
	}
}
