package no.nav.dokdistdpi.sdist005;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setConsumerIdToMdc;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.setOrGenerateCallIdToMdc;

public class Sdist005HeaderProcessor implements Processor {
	@Override
	public void process(Exchange exchange) {
		setOrGenerateCallIdToMdc(exchange);
		setConsumerIdToMdc(exchange);
	}
}
