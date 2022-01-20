package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DpiKvittering {
	private DpiFeilKvittering feil;
	private VarslingFeiletKvittering varslingfeiletkvittering;
	private LeveringsKvittering leveringskvittering;
}
