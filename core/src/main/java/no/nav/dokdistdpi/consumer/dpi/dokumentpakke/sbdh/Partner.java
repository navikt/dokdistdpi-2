package no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class Partner {
	private PartnerIdentification identifier;
	private Set<ContactInformation> contactInformation;
}
