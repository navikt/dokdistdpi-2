package no.nav.dokdistdpi;

import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties({
		ServiceuserProperties.class,
		MaskinportenProperties.class,
		MqGatewayProperties.class
})
@EnableRetry
@EnableScheduling
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
