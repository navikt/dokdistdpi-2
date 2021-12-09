package no.nav.dokdistdpi.consumer.dpi.digitalpost;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class SmsVarsel extends CommonVarsel {
	private String mobiltelefonnummer;
}
