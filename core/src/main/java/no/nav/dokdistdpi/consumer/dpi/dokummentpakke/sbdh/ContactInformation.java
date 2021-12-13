package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactInformation {
	private String contact;
	private String emailAddress;
	private String faxNumber;
	private String telephoneNumber;
	private String contactTypeIdentifier;
}
