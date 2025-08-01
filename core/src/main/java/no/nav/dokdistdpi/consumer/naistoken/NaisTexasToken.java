package no.nav.dokdistdpi.consumer.naistoken;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaisTexasToken(
		@JsonProperty("access_token") String accessToken
) {

}
