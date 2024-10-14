package no.nav.dokdistdpi;

import no.nav.dokdistdpi.azure.AzureProperties;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.OAuthEnabledWebClientConfig;
import no.nav.dokdistdpi.config.WebClientConfig;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.config.prop.DokdistmellomlagerProperties;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@EnableConfigurationProperties({
		ServiceuserProperties.class,
		MaskinportenProperties.class,
		MqGatewayProperties.class,
		DpiClientProperties.class,
		KeyStoreProperties.class,
		DokdistmellomlagerProperties.class,
		DokdistdpiProperties.class,
		AzureProperties.class,
})
@Import({CoreConfig.class,
		WebClientConfig.class,
		OAuthEnabledWebClientConfig.class
})
@EnableRetry
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
