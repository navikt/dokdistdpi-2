package no.nav.dokdistdpi.consumer.lederelection;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	private final ObjectMapper mapper;

	public LeaderElectionConsumer(WebClient.Builder webClientBuilder,
								  ObjectMapper mapper,
								  @Value("${elector.path}") String electorPath) {
		this.webClient = webClientBuilder
				.baseUrl(electorPath.startsWith("http") ? electorPath : "http://" + electorPath)
				.build();
		this.mapper = mapper;
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
						String leader = mapper.readTree(response).get("name").asText();
						// Kommer warning her i IntelliJ men sannsynligvis ikke nødvendig å publishe denne på en egen scheduler
						// pga blir ikke kalt ofte
						String hostname = InetAddress.getLocalHost().getHostName();
						return hostname.equals(leader);
					} catch (Exception e) {
						log.error("Kunne ikke bestemme lederpod. Feilmelding: {}", e.getMessage());
						return false;
					}
				});
	}
}
