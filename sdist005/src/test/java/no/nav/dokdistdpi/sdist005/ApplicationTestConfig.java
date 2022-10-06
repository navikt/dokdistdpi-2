package no.nav.dokdistdpi.sdist005;

import no.nav.dokdistdpi.azure.AzureProperties;
import no.nav.dokdistdpi.azure.TokenConsumer;
import no.nav.dokdistdpi.azure.TokenResponse;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.cache.CacheConfig;
import no.nav.dokdistdpi.config.prop.DokdistDpiProperties;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserProperties.class,
		MaskinportenProperties.class,
		MqGatewayProperties.class,
		DpiClientProperties.class,
		KeyStoreProperties.class,
		DokdistDpiProperties.class,
		AzureProperties.class
})
@Import({CacheConfig.class,
		JmsItestConfig.class
})
@ComponentScan(basePackages = "no.nav.dokdistdpi")
public abstract class ApplicationTestConfig {

	static class Config {
		@Bean
		@Primary
		TokenConsumer azureTokenConsumer() {
			return (String) -> TokenResponse.builder()
					.access_token("dummy")
					.build();
		}
	}
}
