package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class VarslingFeiletKvittering extends DpiMelding {
	private String beskrivelse;
	private String varslingskanal;
}
