package no.nav.dokdistdpi.common;

import no.nav.dokdistdpi.exception.functional.ForsendelseManglerForsendelseIdFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.slf4j.MDC;

import java.util.UUID;

import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.isBlank;

public class IdsProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		setOrGenerateCallIdToMdc(exchange);
	}

	private void setOrGenerateCallIdToMdc(Exchange exchange) {
		final String callId = exchange.getIn().getHeader(CALL_ID, String.class);
		if (isNull(callId) || isBlank(callId)) {
			String newCallId = UUID.randomUUID().toString();
			exchange.getIn().setHeader(CALL_ID, newCallId);
			MDC.put(CALL_ID, newCallId);
		} else {
			MDC.put(CALL_ID, callId);
		}
	}

	private void setForsendelseIdAsProperty(Exchange exchange) {
		String forsendelseId = XPathBuilder.xpath("//forsendelseId/text()").evaluate(exchange, String.class);
		if (forsendelseId == null || forsendelseId.trim().isEmpty()) {
			throw new ForsendelseManglerForsendelseIdFunctionalException(exchange.getFromRouteId() + " har mottatt forsendelse uten påkrevd felt forsendelseId");
		}
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
	}
}
