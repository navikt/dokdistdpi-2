package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentifierResource {
	private InfoRecord infoRecord;
	private List<ServiceRecord> serviceRecords;

	Optional<ServiceRecord> findServiceRecord(final String process, final ServiceIdentifier serviceIdentifier) {
		return serviceRecords.stream()
				.filter(serviceRecord -> process.equals(serviceRecord.getProcess()))
				.filter(serviceRecord -> serviceIdentifier == serviceRecord.getService().getIdentifier())
				.findAny();
	}

	static IdentifierResource empty() {
		return IdentifierResource.builder()
				.infoRecord(new InfoRecord(null, null))
				.serviceRecords(Collections.emptyList())
				.build();
	}
}
