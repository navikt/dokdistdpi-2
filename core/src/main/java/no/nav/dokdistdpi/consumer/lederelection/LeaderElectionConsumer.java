package no.nav.dokdistdpi.consumer.lederelection;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;

@Slf4j
@Component
public class LeaderElectionConsumer {
	private final RestClient restClient;
	private final ObjectMapper mapper;

	public LeaderElectionConsumer(RestClient.Builder restClientBuilder,
								  ObjectMapper mapper,
								  @Value("${elector.path}") String electorPath) {
		this.restClient = restClientBuilder
				.baseUrl(electorPath.startsWith("http") ? electorPath : "http://" + electorPath)
				.build();
		this.mapper = mapper;
	}

	public boolean isLeader() {
		try {
			String response = restClient.get()
					.retrieve()
					.body(String.class);
			String leader = mapper.readTree(response).get("name").asText();
			String hostname = InetAddress.getLocalHost().getHostName();
			return hostname.equals(leader);
		} catch (Exception e) {
			log.error("Kunne ikke bestemme lederpod. Feilmelding: {}", e.getMessage());
			return false;
		}
	}
}
