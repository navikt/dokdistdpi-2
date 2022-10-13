package no.nav.dokdistdpi.azure;

public interface TokenConsumer {
	TokenResponse getClientCredentialToken(String scope);
}
