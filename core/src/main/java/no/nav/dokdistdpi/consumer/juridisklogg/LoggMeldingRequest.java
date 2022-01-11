package no.nav.dokdistdpi.consumer.juridisklogg;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoggMeldingRequest {
	private String meldingsId;
	private String avsender;
	private String mottaker;
	private String joarkRef;
	private byte[] meldingsInnhold;
	private Integer antallAarLagres;
}
