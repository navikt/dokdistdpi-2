package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

public class OidcErrorHandler extends DefaultResponseErrorHandler {
	@Override
	protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode) throws IOException {
		if (statusCode.is4xxClientError()) {
			throw new HttpClientErrorException(statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		if (statusCode.is5xxServerError()) {
			throw new HttpServerErrorException(statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		} else {
			throw new RestClientException("Unknown status code [" + statusCode + "]");

		}
	}
}
