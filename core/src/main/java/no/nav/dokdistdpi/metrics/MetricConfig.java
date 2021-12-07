package no.nav.dokdistdpi.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricConfig {

    @Bean
    DokTimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new DokTimedAspect(meterRegistry);
    }
}
