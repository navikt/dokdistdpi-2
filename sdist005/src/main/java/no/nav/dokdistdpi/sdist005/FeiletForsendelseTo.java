package no.nav.dokdistdpi.sdist005;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Holder referanse til en forsendelse og status fra DPI
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Value
@Builder
public class FeiletForsendelseTo {
	String forsendelseId;
	String bestillingsId;
	String feilbeskrivelse;
	LocalDateTime feiltidspunkt;
}

