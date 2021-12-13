package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
public class Partner {
	private PartnerIdentification identifier;

	private Set<ContactInformation> contactInformation;

	public Partner setIdentifier(PartnerIdentification identifier) {
		this.identifier = identifier;
		identifier.setPartner(this);
		return this;
	}

	public Set<ContactInformation> getContactInformation() {
		if (contactInformation == null) {
			contactInformation = new HashSet<>();
		}
		return this.contactInformation;
	}
}
