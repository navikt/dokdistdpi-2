package no.nav.dokdistdpi.consumer.juridisklogg;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoggMeldingRequest {
	String meldingsId;
	String avsender;
	String mottaker;
	String joarkRef;
	byte[] meldingsInnhold;
	Integer antallAarLagres;
}
