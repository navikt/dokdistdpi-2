package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ForsendelseStatusResponse {
	StatusType status;
	String beskrivelse;
	LocalDateTime timestamp;

	public enum StatusType {
		OPPRETTET, SENDT, FEILET
	}
}

