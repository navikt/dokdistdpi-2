package no.nav.dokdistdpi.config;

import no.nav.dokdistdpi.azure.AzureTokenConsumer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public record WebClientAzureAuthentication(AzureTokenConsumer azureTokenConsumer, String scope)
		implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return next.exchange(ClientRequest.from(request).headers((headers) ->
				headers.setBearerAuth(azureTokenConsumer.getClientCredentialToken(scope))).build());
	}

}
