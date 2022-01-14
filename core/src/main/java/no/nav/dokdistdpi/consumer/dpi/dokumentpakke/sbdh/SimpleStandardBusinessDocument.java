package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer;

import java.util.Objects;
import java.util.Set;

import static java.util.Objects.nonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(using = SimpleSBDSerializer.class)
public class SimpleStandardBusinessDocument {
	private StandardBusinessDocument sbd;

	@JsonIgnore
	public final String getBestillingsId() {
		return getStandardBusinessDocumentHeader().getDocumentIdentification()
				.getInstanceIdentifier();
	}

	@JsonIgnore
	public String getConversationId() {
		return getScopes().stream()
				.map(scope -> scope.getInstanceIdentifier())
				.findFirst().orElse(null);
	}

	@JsonIgnore
	public Set<Scope> getScopes() {
		return getStandardBusinessDocumentHeader()
				.getBusinessScope()
				.getScope();
	}

	@JsonIgnore
	private BusinessScope getBusinessScope() {
		return nonNull(getStandardBusinessDocumentHeader()) ? getStandardBusinessDocumentHeader().getBusinessScope() : null;
	}

	@JsonIgnore
	public StandardBusinessDocumentHeader getStandardBusinessDocumentHeader() {
		return nonNull(sbd.getStandardBusinessDocumentHeader()) ? sbd.getStandardBusinessDocumentHeader() : null;
	}

	@JsonIgnore
	public String getSender() {
		return getStandardBusinessDocumentHeader().getSender().stream().filter(Objects::nonNull).map(sender ->
				Organisasjonsnummer.asIso6523(sender.getIdentifier().getValue())
		).findFirst().orElse(null);
	}

	@JsonIgnore
	public String getReceiver() {
		return getStandardBusinessDocumentHeader().getReceiver().stream().filter(Objects::nonNull).map(sender ->
				Organisasjonsnummer.asIso6523(sender.getIdentifier().getValue())
		).findFirst().orElse(null);
	}
}
