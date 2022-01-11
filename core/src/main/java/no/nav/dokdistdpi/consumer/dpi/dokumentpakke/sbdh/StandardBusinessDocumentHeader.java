package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardBusinessDocumentHeader {
	private String headerVersion;
	private Set<Partner> sender;
	private Set<Partner> receiver;
	private DocumentIdentification documentIdentification;
	private BusinessScope businessScope;

	public void setSender(Set<Partner> sender) {
		this.sender = sender;
	}

	public Set<Partner> getSender() {
		if (sender == null) {
			sender = new HashSet<>();
		}
		return this.sender;
	}

	public StandardBusinessDocumentHeader addSender(Partner partner) {
		getSender().add(partner);
		return this;
	}

	public Set<Partner> getReceiver() {
		if (receiver == null) {
			receiver = new HashSet<>();
		}
		return this.receiver;
	}

	public StandardBusinessDocumentHeader addReceiver(Partner partner) {
		getReceiver().add(partner);
		return this;
	}

	@JsonIgnore
	Optional<Partner> getFirstSender() {
		if (sender == null) {
			return Optional.empty();
		}
		return sender.stream().findFirst();
	}

	@JsonIgnore
	Optional<Partner> getFirstReceiver() {
		if (receiver == null) {
			return Optional.empty();
		}
		return receiver.stream().findFirst();
	}

}
