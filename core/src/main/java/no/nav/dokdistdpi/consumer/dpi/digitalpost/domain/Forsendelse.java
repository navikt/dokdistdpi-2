package no.nav.dokdistdpi.consumer.dpi.digitalpost.domain;

import lombok.Builder;
import lombok.Data;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;

@Data
@Builder
public class Forsendelse {
	private String forsendelseId;
	private String personidentifikator;
	private String mottakerSertifikat;
	private String digitalPostLeverandoerAdresse;
	private final String konversasjonId;
	private final String bestillingsId;
	private final DistribusjonsTypeKode distribusjonsTypeKode;
	private DigitalPost digital;
	private Dokumentpakke dokumentpakke;
}
