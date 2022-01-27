package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Avsender;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Virksomhetmottaker;

import java.time.ZonedDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class DpiMelding {
	private Avsender avsender;
	private Virksomhetmottaker virksomhetmottaker;
	private ZonedDateTime tidspunkt;
	private String konversasjonsId;
	private KvitteringType kvitteringType;
	private String bestillingsId;
}
