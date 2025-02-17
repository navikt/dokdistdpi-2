package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sikkerhetsnivaa {
	NIVAA_3(3),
	NIVAA_4(4);

	private final int value;
}
