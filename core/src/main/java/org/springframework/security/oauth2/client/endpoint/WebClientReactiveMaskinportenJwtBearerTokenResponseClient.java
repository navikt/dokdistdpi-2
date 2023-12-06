package org.springframework.security.oauth2.client.endpoint;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Set;

/**
 * Overrider package private metoder
 * <p>
 * https://github.com/spring-projects/spring-security/issues/11298
 * <p>
 * Maskinporten støtter ikke client_id, scope parameters
 */
public class WebClientReactiveMaskinportenJwtBearerTokenResponseClient extends AbstractWebClientReactiveOAuth2AccessTokenResponseClient<JwtBearerGrantRequest> {

	@Override
	ClientRegistration clientRegistration(JwtBearerGrantRequest grantRequest) {
		return grantRequest.getClientRegistration();
	}

	@Override
	Set<String> scopes(JwtBearerGrantRequest grantRequest) {
		return grantRequest.getClientRegistration().getScopes();
	}

	@Override
	BodyInserters.FormInserter<String> populateTokenRequestBody(JwtBearerGrantRequest grantRequest,
																BodyInserters.FormInserter<String> body) {
		return body.with(OAuth2ParameterNames.ASSERTION,
				grantRequest.getJwt().getTokenValue());
	}
}
