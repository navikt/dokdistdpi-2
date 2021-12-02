package no.nav.dokdistdpi;

import no.nav.dokdistdpi.config.ServiceuserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
		ServiceuserProperties.class
})
public class CoreConfig {
}
