package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfoRecord {

	private String identifier;
	private String organizationName;

	@JsonCreator
	public InfoRecord(@JsonProperty("identifier") String identifier, @JsonProperty("organizationName") String organizationName) {
		this.identifier = identifier;
		this.organizationName = organizationName;
	}
}
