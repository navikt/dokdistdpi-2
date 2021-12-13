package no.nav.dokdistdpi.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricConfig {
    @Bean
    DokMonitoringAspect timedAspect(MeterRegistry meterRegistry) {
        return new DokMonitoringAspect(meterRegistry);
    }
}
