package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Service {

	private ServiceIdentifier identifier;
	private String endpointUrl;
	private String serviceCode;
	private String serviceEditionCode;
	private Integer securityLevel;

	@JsonCreator
	public Service(@JsonProperty("identifier") ServiceIdentifier identifier,
				   @JsonProperty("endpointUrl") String endpointUrl,
				   @JsonProperty("serviceCode") String serviceCode,
				   @JsonProperty("serviceEditionCode") String serviceEditionCode,
				   @JsonProperty("securityLevel") Integer securityLevel) {
		this.identifier = identifier;
		this.endpointUrl = endpointUrl;
		this.serviceCode = serviceCode;
		this.serviceEditionCode = serviceEditionCode;
		this.securityLevel = securityLevel;
	}
}
