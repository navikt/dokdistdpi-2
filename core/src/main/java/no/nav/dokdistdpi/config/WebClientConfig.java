package no.nav.dokdistdpi.config;

import no.nav.dokdistdpi.consumer.naistoken.NaisTexasConsumer;
import no.nav.dokdistdpi.consumer.naistoken.NaisTexasWebClientRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static java.time.Duration.ofMinutes;
import static java.time.temporal.ChronoUnit.SECONDS;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder
				.clone()
				.clientConnector(new ReactorClientHttpConnector(nettyProxyHttpClient()))
				.build();
	}

	@Bean
	public WebClient texasAuthorizedWebClient(NaisTexasConsumer naisTexasConsumer) {
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(nettyProxyHttpClient()))
				.filter(new NaisTexasWebClientRequestInterceptor(naisTexasConsumer))
				.build();
	}

	@Bean
	public WebClient webClientLongResponseTimeout(WebClient.Builder webClientBuilder) {
		HttpClient httpClient = HttpClient.create()
				.responseTimeout(ofMinutes(10))
				.proxyWithSystemProperties();
		return webClientBuilder
				.clone()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	@Bean
	HttpClient nettyProxyHttpClient() {
		return HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
	}
}
