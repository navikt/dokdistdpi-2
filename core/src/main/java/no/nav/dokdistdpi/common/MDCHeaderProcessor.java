package no.nav.dokdistdpi.common;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.UUID;

import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.isBlank;

public class MDCHeaderProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		setOrGenerateCallIdToMdc(exchange);
		setForsendelseIdAsProperty(exchange);
		setConsumerIdToMdc(exchange);
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
			return;
		}
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
	}

	private void setConsumerIdToMdc(Exchange exchange) {
		String consumerId = exchange.getIn().getHeader(NAV_CONSUMER_ID, String.class);
		if (!StringUtils.isBlank(consumerId)) {
			MDC.put(NAV_CONSUMER_ID, consumerId);
		}
	}
}
