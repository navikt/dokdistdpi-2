package no.nav.dokdistdpi.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
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
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(credentialsJsonPath.toFile(), KeyStoreCredentials.class);
		} catch (IOException e) {
			// Rethrower ikke exception for å ikke risikere at innhold dumpes til loggen
			throw new IllegalArgumentException("Klarte ikke lese credentials json");
		}
	}
}
