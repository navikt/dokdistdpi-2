package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder=true)
public class DpiVarslingfeilet extends DpiMelding {
	private String beskrivelse;
	private Varslingskanal varslingskanal;
}
