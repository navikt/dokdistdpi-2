package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ServiceRecord {

	private String organisationNumber;
	private String pemCertificate;
	private String process;
	private List<String> documentTypes;
	private Service service;
}
