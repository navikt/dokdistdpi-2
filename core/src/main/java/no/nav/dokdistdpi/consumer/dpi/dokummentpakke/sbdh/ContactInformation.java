package no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh;

import lombok.Data;

@Data
public class ContactInformation {
	private String contact;
	private String emailAddress;
	private String faxNumber;
	private String telephoneNumber;
	private String contactTypeIdentifier;
}
