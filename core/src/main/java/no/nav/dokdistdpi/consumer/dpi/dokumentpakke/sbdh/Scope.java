package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scope {
	private String type;
	private String instanceIdentifier;
	private String identifier;
	private Set<CorrelationInformation> scopeInformation;

	public Set<CorrelationInformation> getScopeInformation() {
		if (scopeInformation == null) {
			scopeInformation = new HashSet<>();
		}
		return this.scopeInformation;
	}

	public Scope addScopeInformation(CorrelationInformation correlationInformation) {
		getScopeInformation().add(correlationInformation);
		return this;
	}
}
