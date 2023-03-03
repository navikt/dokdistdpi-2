package no.nav.dokdistdpi.consumer.rdist001.domain;


import java.time.LocalDateTime;

public record Notifikasjon(
		String kanal,
		String tittel,
		String tekst,
		String kontaktInfo,
		LocalDateTime varslingstidspunkt
) {
}
