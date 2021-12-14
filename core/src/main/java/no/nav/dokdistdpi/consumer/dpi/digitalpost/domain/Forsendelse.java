package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;

@Data
@Builder
public class Forsendelse {
	private String personidentifikator;
	private String mottakerSertifikat;
	private String digitalPostLeverandoerAdresse;
	private final String konversasjonId;
	private final String bestillingsId;
	private DigitalPost digital;
	private Dokumentpakke dokumentpakke;
}
