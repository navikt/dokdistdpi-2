package no.nav.dokdistdpi.azure;

public interface TokenConsumer {
	String getClientCredentialToken(String scope);
}
