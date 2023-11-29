package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class SmsVarsel extends CommonVarsel {
	private String mobiltelefonnummer;
}
