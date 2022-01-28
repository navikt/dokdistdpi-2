package no.nav.dokdistdpi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@ComponentScan
@Configuration
public class CoreConfig {
	@Bean
	Clock clock() {
		return Clock.system(DEFAULT_ZONE_ID);
	}
}
