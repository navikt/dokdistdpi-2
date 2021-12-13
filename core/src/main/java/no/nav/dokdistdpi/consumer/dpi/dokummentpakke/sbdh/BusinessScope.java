package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class BusinessScope implements Serializable {
	protected Set<Scope> scope;

	public Set<Scope> getScope() {
		if (scope == null) {
			scope = new HashSet<>();
		}
		return this.scope;
	}

	public BusinessScope addScope(Scope scope) {
		getScope().add(scope);
		return this;
	}
}
