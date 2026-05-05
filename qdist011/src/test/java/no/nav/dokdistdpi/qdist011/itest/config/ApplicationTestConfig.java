package no.nav.dokdistdpi.qdist011.itest.config;

import no.nav.dokdistdpi.azure.AzureProperties;
import no.nav.dokdistdpi.certificate.KeyStoreConfig;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.cloudstorage.EncryptedBucketStorage;
import no.nav.dokdistdpi.config.cache.CacheConfig;
import no.nav.dokdistdpi.config.prop.DokdistdpiProperties;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.NaisProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.resilience.annotation.EnableResilientMethods;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("itest")
@EnableResilientMethods
@EnableConfigurationProperties({
		ServiceuserProperties.class,
		MaskinportenProperties.class,
		MqGatewayProperties.class,
		DpiClientProperties.class,
		KeyStoreProperties.class,
		DokdistdpiProperties.class,
		AzureProperties.class,
		NaisProperties.class
})
@Import({CacheConfig.class,
		KeyStoreConfig.class,
		JmsItestConfig.class
})
@ComponentScan(basePackages = "no.nav.dokdistdpi")
public class ApplicationTestConfig {

	@Bean
	public EncryptedBucketStorage bucketStorage() {
		return mock(EncryptedBucketStorage.class);
	}
}
