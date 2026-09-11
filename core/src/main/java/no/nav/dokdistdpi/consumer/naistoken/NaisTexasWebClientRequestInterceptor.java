package no.nav.dokdistdpi.consumer.naistoken;

import no.nav.dokdistdpi.exception.functional.NaisTexasTechnicalException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static no.nav.dokdistdpi.consumer.naistoken.NaisTexasConsumer.TARGET_PATTERN;
import static no.nav.dokdistdpi.consumer.naistoken.NaisTexasRequestInterceptor.TARGET_SCOPE;

public class NaisTexasWebClientRequestInterceptor implements ExchangeFilterFunction {

	private final NaisTexasConsumer naisTexasConsumer;

	public NaisTexasWebClientRequestInterceptor(NaisTexasConsumer naisTexasConsumer) {
		this.naisTexasConsumer = naisTexasConsumer;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return Mono.fromCallable(() -> addHeaders(request))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(next::exchange)
				.onErrorMap(error ->
						new NaisTexasTechnicalException(String.format("Kunne ikke legge token i headers. feilmelding=%s", error.getMessage()), error)
				);
	}

	private ClientRequest addHeaders(ClientRequest request) {
		ClientRequest.Builder requestBuilder = ClientRequest.from(request);
		request.attribute(TARGET_SCOPE)
				.map(String.class::cast)
				.ifPresent(targetScope -> {
					if (TARGET_PATTERN.matcher(targetScope).matches()) {
						requestBuilder.headers(headers -> headers.setBearerAuth(naisTexasConsumer.getSystemToken(targetScope)));
					} else {
						requestBuilder.headers(headers -> headers.setBearerAuth(naisTexasConsumer.getMaskinportenToken(targetScope)));
					}
				});
		return requestBuilder.build();
	}
}