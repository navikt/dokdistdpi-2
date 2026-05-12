package no.nav.dokdistdpi.consumer.lederelection;

import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.InetAddress;

@Slf4j
@Component
public class LeaderElectionConsumer {
	private final WebClient webClient;
	private final JsonMapper jsonMapper;

	public LeaderElectionConsumer(WebClient.Builder webClientBuilder,
								  JsonMapper jsonMapper,
								  @Value("${elector.path}") String electorPath) {
		this.webClient = webClientBuilder
				.baseUrl(electorPath.startsWith("http") ? electorPath : "http://" + electorPath)
				.build();
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Brukes av Sdist005
	 *
	 * @return true hvis denne podden er leader, ellers false
	 */
	public boolean isLeader() {
		return Boolean.TRUE.equals(isLeaderAsync().block());
	}

	public Mono<Boolean> isLeaderAsync() {
		return webClient.get()
				.retrieve()
				.bodyToMono(String.class)
				.map(response -> {
					try {
						String leader = jsonMapper.readTree(response).get("name").asText();
						String hostname = InetAddress.getLocalHost().getHostName();
						return hostname.equals(leader);
					} catch (Exception e) {
						log.error("Kunne ikke bestemme lederpod. Feilmelding: {}", e.getMessage());
						return false;
					}
				});
	}
}
