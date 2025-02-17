package no.nav.dokdistdpi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@Configuration
@EnableScheduling
public class CoreConfig {

	@Bean
	Clock clock() {
		return Clock.system(DEFAULT_ZONE_ID);
	}

}