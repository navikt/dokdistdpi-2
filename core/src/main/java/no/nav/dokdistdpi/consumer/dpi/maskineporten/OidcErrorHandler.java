package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;

public class OidcErrorHandler extends DefaultResponseErrorHandler {

	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		var statusCode = response.getStatusCode();

		if (statusCode.is4xxClientError()) {
			throw new HttpClientErrorException(statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		} else if (statusCode.is5xxServerError()) {
			throw new HttpServerErrorException(statusCode, response.getStatusText(),
					response.getHeaders(), getResponseBody(response), getCharset(response));
		}

		throw new RestClientException("Unknown status code [" + statusCode + "]");
	}

}