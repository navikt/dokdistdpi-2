package no.nav.dokdistdpi.qdist011.Utils;

import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;

public class DigitalPostUtils {

	//Pass på encoding her; Disse må inneholde "å".
	public static String VEDTAK_TEKST = "Du har fått et vedtak fra NAV. Les det i din digitale postkasse.";
	public static String VIKTIG_TEKST = "Du har fått et viktig brev fra NAV. Les det i din digitale postkasse";
	public static String DEFAULT_TEKST = "Du har fått et brev fra NAV. Les det i din digitale postkasse";


	public static String determineVarslingstekst(DistribusjonsTypeKode distribusjonstype) {

		//Pattern matching er ikke lov i switcher enda virker det som.
		//Ønsker å ha denne inn i switchen: "case VIKTIG, null -> VIKTIG_TEKST" når det er mulig
		if (distribusjonstype == null) {
			return VIKTIG_TEKST;
		}

		return switch (distribusjonstype) {
			case VEDTAK -> VEDTAK_TEKST;
			case VIKTIG -> VIKTIG_TEKST;
			default -> DEFAULT_TEKST;
		};
	}
}
