package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

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
	private Set<Sender> sender;
	private Set<Receiver> receiver;
	private DocumentIdentification documentIdentification;
	private Manifest manifest;
	private BusinessScope businessScope;

	public void setSender(Set<Sender> sender) {
		this.sender = sender;
	}

	public Set<Sender> getSender() {
		if (sender == null) {
			sender = new HashSet<>();
		}
		return this.sender;
	}

	public StandardBusinessDocumentHeader addSender(Sender partner) {
		getSender().add(partner);
		return this;
	}

	public Set<Receiver> getReceiver() {
		if (receiver == null) {
			receiver = new HashSet<>();
		}
		return this.receiver;
	}

	public StandardBusinessDocumentHeader addReceiver(Receiver partner) {
		getReceiver().add(partner);
		return this;
	}

	@JsonIgnore
	Optional<Sender> getFirstSender() {
		if (sender == null) {
			return Optional.empty();
		}
		return sender.stream().findFirst();
	}

	@JsonIgnore
	Optional<Receiver> getFirstReceiver() {
		if (receiver == null) {
			return Optional.empty();
		}
		return receiver.stream().findFirst();
	}

}
