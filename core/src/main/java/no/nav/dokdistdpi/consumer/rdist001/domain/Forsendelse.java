package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Value;

@Value
public class Forsendelse {

	String forsendelseId;

	public Forsendelse(Long forsendelseId) {
		if (forsendelseId == null) {
			throw new IllegalArgumentException("forsendelseId kan ikke være null");
		}
		this.forsendelseId = forsendelseId.toString();
	}
}
