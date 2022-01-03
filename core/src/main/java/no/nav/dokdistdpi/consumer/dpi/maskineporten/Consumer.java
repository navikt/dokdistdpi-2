package no.nav.dokdistdpi.consumer.dpi.maskineporten;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Consumer {
	@JsonProperty("Authority")
	private String authority;
	@JsonProperty("ID")
	private String id;
}
