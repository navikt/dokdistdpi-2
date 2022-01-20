package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LeveringsKvittering extends DpiMelding {
}
