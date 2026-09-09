package no.nav.dokdistdpi.consumer.naistoken;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

import static no.nav.dokdistdpi.consumer.naistoken.NaisTexasConsumer.TARGET_PATTERN;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.CALL_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALL_ID;

public class NaisTexasRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String TARGET_SCOPE = "targetScope";

	private final NaisTexasConsumer naisTexasConsumer;

	public NaisTexasRequestInterceptor(NaisTexasConsumer naisTexasConsumer) {
		this.naisTexasConsumer = naisTexasConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		Map<String, Object> attributes = request.getAttributes();
		if (attributes.containsKey(TARGET_SCOPE)) {
			String targetScope = (String) attributes.get(TARGET_SCOPE);
			if (TARGET_PATTERN.matcher(targetScope).matches()) {
				request.getHeaders().setBearerAuth(naisTexasConsumer.getSystemToken(targetScope));
			} else {
				request.getHeaders().setBearerAuth(naisTexasConsumer.getMaskinportenToken(targetScope));
			}
		}
		request.getHeaders().set(NAV_CALL_ID, MDC.get(CALL_ID));
		return execution.execute(request, body);
	}
}
