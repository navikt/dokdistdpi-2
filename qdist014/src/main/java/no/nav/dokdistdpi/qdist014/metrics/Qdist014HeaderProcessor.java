package no.nav.dokdistdpi.qdist014.metrics;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setConsumerIdToMdc;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setOrGenerateCallIdToMdc;

public class Qdist014HeaderProcessor implements Processor {
	@Override
	public void process(Exchange exchange) {
		setOrGenerateCallIdToMdc(exchange);
		setConsumerIdToMdc(exchange);
	}
}
