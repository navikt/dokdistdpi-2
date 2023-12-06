package no.nav.dokdistdpi.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.DelegatingReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveMaskinportenJwtBearerTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.JWT_BEARER;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE;
import static org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256;

@Configuration
public class OAuthEnabledWebClientConfig {

	public static final String MASKINPORTEN_CLIENT_REGISTRATION = "maskinporten";

	@Bean
	WebClient oauth2WebClient(ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		var oAuth2AuthorizedClientExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);

		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.filter(oAuth2AuthorizedClientExchangeFilterFunction)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
			ReactiveClientRegistrationRepository reactiveClientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService,
			ReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider
	) {
		var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(reactiveClientRegistrationRepository, reactiveOAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(reactiveOAuth2AuthorizedClientProvider);
		return authorizedClientManager;
	}

	@Bean
	ReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider(MaskinportenProperties maskinportenProperties) throws ParseException {
		WebClient reactiveProxyTokenWebClient = createReactiveProxyTokenWebClient();
		return new DelegatingReactiveOAuth2AuthorizedClientProvider(createMaskinportenJwtBearerReactiveOAuth2AuthorizedClientProvider(reactiveProxyTokenWebClient, maskinportenProperties));
	}

	/**
	 * @return ReactiveOAuth2AuthorizedClientProvider som støtter JWT grant token for maskinporten
	 */
	private static JwtBearerReactiveOAuth2AuthorizedClientProvider createMaskinportenJwtBearerReactiveOAuth2AuthorizedClientProvider(WebClient reactiveProxyTokenWebClient, MaskinportenProperties maskinportenProperties) throws ParseException {
		JWK maskinportenClientJwk = RSAKey.parse(maskinportenProperties.getClientJwk());
		JWKSource<SecurityContext> maskinportenClientJwkSource = new ImmutableJWKSet<>(new JWKSet(maskinportenClientJwk));

		var jwtBearerReactiveOAuth2AuthorizedClientProvider = new JwtBearerReactiveOAuth2AuthorizedClientProvider();
		var webClientReactiveJwtBearerTokenResponseClient = new WebClientReactiveMaskinportenJwtBearerTokenResponseClient();
		webClientReactiveJwtBearerTokenResponseClient.setWebClient(reactiveProxyTokenWebClient);
		jwtBearerReactiveOAuth2AuthorizedClientProvider.setJwtAssertionResolver(oAuth2AuthorizationContext -> resolveJwtAssertion(oAuth2AuthorizationContext, maskinportenClientJwkSource));
		jwtBearerReactiveOAuth2AuthorizedClientProvider.setAccessTokenResponseClient(webClientReactiveJwtBearerTokenResponseClient);
		return jwtBearerReactiveOAuth2AuthorizedClientProvider;
	}

	private static Mono<Jwt> resolveJwtAssertion(OAuth2AuthorizationContext context, JWKSource<SecurityContext> maskinportenClientJwkSource) {
		ClientRegistration clientRegistration = context.getClientRegistration();
		if (MASKINPORTEN_CLIENT_REGISTRATION.equals(clientRegistration.getRegistrationId())) {
			JwsHeader.Builder headersBuilder = JwsHeader.with(RS256);

			Instant issuedAt = Instant.now();
			Instant expiresAt = issuedAt.plus(Duration.ofSeconds(60));

			JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
					.issuer(clientRegistration.getClientId())
					.audience(singletonList(clientRegistration.getProviderDetails().getIssuerUri()))
					.id(UUID.randomUUID().toString())
					.issuedAt(issuedAt)
					.expiresAt(expiresAt)
					.claim("scope", clientRegistration.getScopes().stream().reduce((a, b) -> a + " " + b).orElse(""));

			NimbusJwtEncoder nimbusJwtEncoder = new NimbusJwtEncoder(maskinportenClientJwkSource);
			Jwt jws = nimbusJwtEncoder.encode(JwtEncoderParameters.from(headersBuilder.build(), claimsBuilder.build()));
			return Mono.just(jws);
		}
		return Mono.empty();
	}

	/**
	 * @return WebClient med webproxy støtte
	 */
	private static WebClient createReactiveProxyTokenWebClient() {
		var nettyHttpClient = HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);
		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Bean
	ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(List<ClientRegistration> clientRegistration) {
		return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
	}

	@Bean
	List<ClientRegistration> clientRegistration(MaskinportenProperties maskinportenProperties) {
		return List.of(
				ClientRegistration.withRegistrationId(MASKINPORTEN_CLIENT_REGISTRATION)
						.tokenUri(maskinportenProperties.getTokenEndpoint())
						.clientId(maskinportenProperties.getClientId())
						.issuerUri(maskinportenProperties.getIssuer())
						// client authentication behøves ikke mot maskinporten https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_token
						.clientAuthenticationMethod(NONE)
						.authorizationGrantType(JWT_BEARER)
						.scope(maskinportenProperties.getScopes())
						.build()
		);
	}
}
