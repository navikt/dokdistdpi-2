package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Avsender;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Virksomhetmottaker;

import java.time.LocalDateTime;

@Data
@SuperBuilder
public class Kvittering {
	private Avsender avsender;
	private Virksomhetmottaker virksomhetmottaker;
	private LocalDateTime tidspunkt;
}
