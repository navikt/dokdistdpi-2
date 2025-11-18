package no.nav.dokdistdpi.certificate;

import java.util.StringJoiner;

public record KeyStoreCredentials(String alias, String password, String type) {
	@Override
	public String toString() {
		return new StringJoiner(", ", KeyStoreCredentials.class.getSimpleName() + "[", "]")
				.add("alias='" + alias + "'")
				.add("password='*****'")
				.add("type='" + type + "'")
				.toString();
	}
}
