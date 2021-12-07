package no.nav.dokdistdpi.metrics;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.lang.NonNullApi;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.exception.functional.AbstractDokdistdpiFunctionalException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.MDC_REQUEST_ID;

@Aspect
@NonNullApi
@Incubating(since = "1.0.0")
@Slf4j
public class DokTimedAspect {

    private final MeterRegistry registry;
    private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint;

    public DokTimedAspect(MeterRegistry registry) {
        this(registry, pjp ->
                Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
                        "method", pjp.getStaticPart().getSignature().getName())
        );
    }

    public DokTimedAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
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
        } catch (Exception e) {

            if (monitor.logExceptions()) {
                logException(method, e);
            }

            if (monitor.createErrorMetric()) {
                Counter.builder(monitor.value() + ".exception")
                        .tags("error.type", isFunctionalException(method, e) ? "functional" : "technical")
                        .tags("exception.name", e.getClass().getSimpleName())
                        .tags(monitor.extraTags())
                        .tags(tagsBasedOnJoinpoint.apply(pjp))
                        .register(registry)
                        .increment();
            }

            throw e;

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

    private boolean isFunctionalException(Method method, Exception e) {
        return asList(method.getExceptionTypes()).contains(e.getClass()) || isFunctionalException(e);
    }

    private void logException(Method method, Exception e) {
        String mdcRequestId = (MDC.get(MDC_REQUEST_ID) == null) ? "" : (MDC.get(MDC_REQUEST_ID) + " ");

        if (isFunctionalException(method, e)) {
            log.warn(mdcRequestId + e.getMessage(), e);
        } else {
            log.error(mdcRequestId + e.getMessage(), e);
        }
    }

    private boolean isFunctionalException(Throwable e) {
        return e instanceof AbstractDokdistdpiFunctionalException;
    }
}
