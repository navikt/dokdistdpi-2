package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OidcTokenResponse {
	private String accessToken;
	private Integer expiresIn;
	private String scope;
}
