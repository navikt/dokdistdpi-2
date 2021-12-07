package no.nav.dokdistdpi;

import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.ServiceRegistryProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
		ServiceuserProperties.class,
		KeyStoreProperties.class,
		MaskinportenProperties.class,
		ServiceRegistryProperties.class
})
@Configuration
public class CoreConfig {
}
