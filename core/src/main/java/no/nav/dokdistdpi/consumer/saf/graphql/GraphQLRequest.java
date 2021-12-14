package no.nav.dokdistdpi.consumer.saf.graphql;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class GraphQLRequest {
	private final String query;
	private final String operationName;
	private final Map<String, Object> variables;

	@JsonCreator
	public GraphQLRequest(@JsonProperty("query") String query,
						  @JsonProperty("operationName") String operationName,
						  @JsonProperty("variables") Map<String, Object> variables) {
		this.query = query;
		this.operationName = operationName;
		this.variables = variables;
	}
}
