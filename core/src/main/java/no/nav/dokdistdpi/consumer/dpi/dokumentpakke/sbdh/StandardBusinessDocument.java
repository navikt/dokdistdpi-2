package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonSerialize(using = StandardBusinessDocumentSerializer.class)
public class StandardBusinessDocument {

	@NonNull
	private StandardBusinessDocumentHeader standardBusinessDocumentHeader;

	@JsonAlias({"digital", "digitalPost", "flyttetDigitalPost", "kvittering", "feil"})
	@NonNull
	private Object any;

	@JsonIgnore
	public final String getBestillingsId() {
		return getStandardBusinessDocumentHeader().getDocumentIdentification()
				.getInstanceIdentifier();
	}

	@JsonIgnore
	public String getConversationId() {
		return getOptionalConversationId()
				.orElseThrow(RuntimeException::new);
	}

	@JsonIgnore
	public Optional<String> getOptionalConversationId() {
		return findScope(ScopeType.CONVERSATION_ID)
				.map(Scope::getInstanceIdentifier);
	}

	@JsonIgnore
	public Set<Scope> getScopes() {
		return getStandardBusinessDocumentHeader()
				.getBusinessScope()
				.getScope();
	}

	public Scope getScope(ScopeType scopeType) {
		return findScope(scopeType)
				.orElseThrow(() -> new RuntimeException(String.format("Missing scope %s", scopeType.name())));
	}

	public Optional<Scope> findScope(ScopeType scopeType) {
		return getScopes()
				.stream()
				.filter(scope -> scopeType.toString().equals(scope.getType()) || scopeType.name().equals(scope.getType()))
				.findAny();
	}
}
