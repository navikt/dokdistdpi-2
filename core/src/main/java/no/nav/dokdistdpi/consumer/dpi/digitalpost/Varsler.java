package no.nav.dokdistdpi.consumer.dpi.digitalpost;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Varsler {
	private EpostVarsel epostvarsel;
	private SmsVarsel smsvarsel;
}

