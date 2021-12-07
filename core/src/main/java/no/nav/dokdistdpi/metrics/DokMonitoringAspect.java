package no.nav.dokdistdpi.metrics;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.function.Function;

@Aspect
@Incubating(since = "1.0.0")
@Slf4j
@SuppressWarnings("Duplicates")
public class DokMonitoringAspect {

	private final MeterRegistry registry;
	private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint;

	public DokMonitoringAspect(MeterRegistry registry) {
		this(registry, pjp ->
				Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
						"method", pjp.getStaticPart().getSignature().getName())
		);
	}

	private DokMonitoringAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
		this.registry = registry;
		this.tagsBasedOnJoinpoint = tagsBasedOnJoinpoint;
	}

	@Around("execution (@no.nav.dokdistdpi.metrics.Monitor * *.*(..))")
	public Object incrementMetrics(ProceedingJoinPoint pjp) throws Throwable {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		Monitor monitor = method.getAnnotation(Monitor.class);

		if (monitor.value().isEmpty()) {
			return pjp.proceed();
		}

		Timer.Sample sample = Timer.start(registry);
		try {
			return pjp.proceed();
		} finally {
			sample.stop(Timer.builder(monitor.value())
					.description(monitor.description().isEmpty() ? null : monitor.description())
					.tags(monitor.extraTags())
					.tags(tagsBasedOnJoinpoint.apply(pjp))
					.publishPercentileHistogram(monitor.histogram())
					.publishPercentiles(monitor.percentiles().length == 0 ? null : monitor.percentiles())
					.register(registry));
		}
	}
}
