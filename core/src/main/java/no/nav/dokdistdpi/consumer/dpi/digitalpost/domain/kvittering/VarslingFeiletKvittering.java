package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class VarslingFeiletKvittering extends DpiMelding {
	private String beskrivelse;
	private Varslingskanal varslingskanal;
}
