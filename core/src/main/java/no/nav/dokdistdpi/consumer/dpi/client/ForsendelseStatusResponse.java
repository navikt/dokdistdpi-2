package no.nav.dokdistdpi.consumer.dpi.client;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class ForsendelseStatusResponse {
	StatusType status;
	String beskrivelse;
	OffsetDateTime timestamp;
}
