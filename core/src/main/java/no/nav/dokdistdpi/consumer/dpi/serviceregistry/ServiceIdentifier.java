package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ServiceIdentifier {

	DPI("DPI"),
	UNKNOWN("UNKNOWN");

	private final String fullname;
}
