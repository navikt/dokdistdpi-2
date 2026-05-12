package no.nav.dokdistdpi.certificate;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class KeyStoreConfig {

	@Bean
	AppCertificate keyStoreCredentials(KeyStoreProperties keyStoreProperties) {
		return new AppCertificate(keyStoreProperties, loadKeyStoreCredentialsJson(keyStoreProperties.credentials()));
	}

	private static KeyStoreCredentials loadKeyStoreCredentialsJson(String credentials) {
		Path credentialsJsonPath = Paths.get(credentials);
		if (!Files.exists(credentialsJsonPath)) {
			throw new IllegalArgumentException("credentials med path=" + credentials + " finnes ikke");
		}
		JsonMapper jsonMapper = JsonMapper.builder().build();
		try {
			return jsonMapper.readValue(credentialsJsonPath.toFile(), KeyStoreCredentials.class);
		} catch (RuntimeException e) {
			// Rethrower ikke exception for å ikke risikere at innhold dumpes til loggen
			throw new IllegalArgumentException("Klarte ikke lese credentials json");
		}
	}
}
