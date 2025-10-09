package no.nav.dokdistdpi;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
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

	@Bean
	public MethodsClient slackClient(DokdistdpiProperties dokdistdpiProperties) {
		return Slack.getInstance().methods(dokdistdpiProperties.getSlack().getToken());
	}
}