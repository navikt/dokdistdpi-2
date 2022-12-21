package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.Set;

import static java.util.Objects.nonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(using = SimpleSBDSerializer.class)
public class SimpleStandardBusinessDocument {

	@NonNull
	@JsonProperty
	private StandardBusinessDocument standardBusinessDocument;

	@JsonIgnore
	public final String getDokumentKonversasjonId() {
		return getStandardBusinessDocumentHeader().getDocumentIdentification()
				.getInstanceIdentifier();
	}

	@JsonIgnore
	public String getKonversasjonId() {
		return getScopes().stream()
				.map(Scope::getInstanceIdentifier)
				.findFirst().orElse(null);
	}

	@JsonIgnore
	public String getType() {
		return getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
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
		return nonNull(standardBusinessDocument.getStandardBusinessDocumentHeader()) ? standardBusinessDocument.getStandardBusinessDocumentHeader() : null;
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
