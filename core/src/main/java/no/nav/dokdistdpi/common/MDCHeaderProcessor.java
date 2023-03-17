package no.nav.dokdistdpi.common;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.language.xpath.XPathBuilder;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setConsumerIdToMdc;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setOrGenerateCallIdToMdc;

public class MDCHeaderProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		setOrGenerateCallIdToMdc(exchange);
		setForsendelseIdAsProperty(exchange);
		setConsumerIdToMdc(exchange);
	}

	private void setForsendelseIdAsProperty(Exchange exchange) {
		String forsendelseId = XPathBuilder.xpath("//forsendelseId/text()").evaluate(exchange, String.class);
		if (forsendelseId == null || forsendelseId.trim().isEmpty()) {
			return;
		}
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);
	}
}
