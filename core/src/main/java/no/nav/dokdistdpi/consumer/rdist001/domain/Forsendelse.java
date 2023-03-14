package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Value;

import static java.lang.String.valueOf;

@Value
public class Forsendelse {

	String forsendelseId;

	public Forsendelse(Long forsendelseId) {
		if (forsendelseId == null) {
			throw new IllegalArgumentException("forsendelseId kan ikke være null");
		}
		this.forsendelseId = valueOf(forsendelseId);
	}
}
