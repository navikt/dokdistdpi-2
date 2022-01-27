package no.nav.dokdistdpi.qdist014.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.ValidationException;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.metrics.MetricLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistdpi.metrics.MetricLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistdpi.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistdpi.metrics.MetricLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistdpi.metrics.MetricLabels.TYPE_TECHNICAL_EXCEPTION;

@Component
public class Qdist014MetricsRoutePolicy extends RoutePolicySupport {
	private final MeterRegistry registry;
	private Timer.Sample timer;

	private static final String EXCEPTION_COUNTER = "dok_metric_exception_total";
	private static final String QDIST014_PROCESS_TIMER = "dok_route_latency_histogram";
	private static final String QDIST014__PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist008";
	private static final String QDIST014_START = "Qdist014_start";
	public static final String SERVICE_ID = "qdist014";

	@Autowired
	public Qdist014MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start();
		registry.counter(QDIST014_START).increment();
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		timer.stop(Timer.builder(QDIST014_PROCESS_TIMER)
				.description(QDIST014__PROCESS_TIMER_DESCRIPTION)
				.tags(LABEL_PROCESS, SERVICE_ID)
				.publishPercentileHistogram(true)
				.register(registry));

		if (exception != null) {
			if (isFunctionalException(exception)) {
				registry.counter(EXCEPTION_COUNTER,
						LABEL_ERROR_TYPE, TYPE_FUNCTIONAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						LABEL_PROCESS, SERVICE_ID).increment();
			} else {
				registry.counter(EXCEPTION_COUNTER,
						LABEL_ERROR_TYPE, TYPE_TECHNICAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						LABEL_PROCESS, SERVICE_ID).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistdpiFunctionalException) || (e instanceof ValidationException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() != null) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
