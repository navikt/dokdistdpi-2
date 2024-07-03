package no.nav.dokdistdpi.qdist011.Utils;

import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;

import static java.lang.String.format;
import static no.nav.dokdistdpi.qdist011.Utils.LeverandoerAdresse.getByValue;

public class VarslingstekstUtil {

	//Pass på encoding her; Disse må inneholde "å".
	public static String VEDTAK_TEKST = "Du har fått et vedtak fra NAV. Les det i din digitale postkasse %s.";
	public static String VIKTIG_TEKST = "Du har fått et viktig brev fra NAV. Les det i din digitale postkasse %s.";
	public static String DEFAULT_TEKST = "Du har fått et brev fra NAV. Les det i din digitale postkasse %s.";
	public static final String LEVERANDOER_DIGIPOST = "(Digipost)";
	public static final String LEVERANDOER_E_BOKS = "(eBoks)";

	public static String determineVarslingstekst(DistribusjonsTypeKode distribusjonstype, String leverandoerAdresse) {

		String leverandoerNavn = getLeverandoerNavn(leverandoerAdresse);
		//Pattern matching er ikke lov i switcher enda virker det som.
		//Ønsker å ha denne inn i switchen: "case VIKTIG, null -> VIKTIG_TEKST" når det er mulig
		if (distribusjonstype == null) {
			return varslingstekst(VIKTIG_TEKST, leverandoerNavn);
		}

		return switch (distribusjonstype) {
			case VEDTAK -> varslingstekst(VEDTAK_TEKST, leverandoerNavn);
			case VIKTIG -> varslingstekst(VIKTIG_TEKST, leverandoerNavn);
			default -> varslingstekst(DEFAULT_TEKST, leverandoerNavn);
		};
	}

	private static String getLeverandoerNavn(String leverandoer) {
		LeverandoerAdresse leverandoerAdresse = getByValue(leverandoer);
		return switch (leverandoerAdresse) {
			case DIGIPOST -> LEVERANDOER_DIGIPOST;
			case EBOKS -> LEVERANDOER_E_BOKS;
		};
	}

	private static String varslingstekst(String tekst, String leverandoerNavn) {
		return format(tekst, leverandoerNavn).strip();
	}
}
