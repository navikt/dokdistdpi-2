package no.nav.dokdistdpi.qdist014.itest.config;

import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.cache.CacheConfig;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.s3storage.AmazonS3Storage;
import no.nav.dokdistdpi.s3storage.Storage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserProperties.class,
		MaskinportenProperties.class,
		MqGatewayProperties.class,
		DpiClientProperties.class,
		KeyStoreProperties.class
})
@Import({CacheConfig.class,
		JmsItestConfig.class,
})
@ComponentScan(basePackages = "no.nav.dokdistdpi")
public abstract class ApplicationTestConfig {

	@Bean
	public AmazonS3 s3() {
		return mock(AmazonS3.class);
	}

	@Bean
	public Storage storage(AmazonS3 s3) {
		return new AmazonS3Storage(s3);
	}

}
