package no.nav.dokdistdpi.consumer.lederelection;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

import static java.time.Duration.ofSeconds;

@Slf4j
@Component
public class LederElectionConsumer {
	private final RestTemplate restTemplate;
	private final ObjectMapper mapper;

	public LederElectionConsumer(RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
		this.mapper = mapper;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(ofSeconds(20))
				.setConnectTimeout(ofSeconds(5))
				.build();
	}

	public boolean isLeader() {
		String electorPath = System.getenv("ELECTOR_PATH");
		if (electorPath == null) {
			log.warn("Kunne ikke bestemme lederpod på grunn av manglende systemvariabel ELECTOR_PATH.");
			return true;
		}

		try {
			String response = restTemplate.getForObject("http://" + electorPath, String.class);
			String leader = mapper.readTree(response).get("name").asText();
			String hostname = InetAddress.getLocalHost().getHostName();
			return hostname.equals(leader);
		} catch (Exception e) {
			log.warn(String.format("Kunne ikke bestemme lederpod. Feilmelding: %s", e.getMessage()), e);
			return true;
		}
	}
}
