package no.nav.dokdistdpi.consumer.rdist001.domain;

public enum Oppslagsnoekkel {
	KONVERSASJONSID("konversasjonsId"),
	BESTILLINGSID("bestillingsId"),
	JOURNALPOSTID("journalpostId");

	public final String noekkel;

	Oppslagsnoekkel(String noekkel) {
		this.noekkel = noekkel;
	}
}