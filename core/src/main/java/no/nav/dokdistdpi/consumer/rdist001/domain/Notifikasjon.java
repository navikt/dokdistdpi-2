package no.nav.dokdistdpi.consumer.rdist001.domain;


import no.nav.dokdistdpi.consumer.rdist001.kodeverk.VarslingKanalCode;

import java.time.LocalDateTime;

public record Notifikasjon(
		VarslingKanalCode kanal,
		String tittel,
		String tekst,
		String kontaktInfo,
		LocalDateTime varslingstidspunkt) {
}
