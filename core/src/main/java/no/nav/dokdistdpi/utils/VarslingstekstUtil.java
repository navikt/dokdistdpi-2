package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.domain.LeverandoerAdresse;

import static java.lang.String.format;
import static no.nav.dokdistdpi.domain.LeverandoerAdresse.findByOrganisasjonsnummer;

public class VarslingstekstUtil {

	//Pass på encoding her; Disse må inneholde "å".
	public static final String VEDTAK_TEKST = "Du har fått et vedtak fra Nav. Les det i din digitale postkasse (%s).";
	public static final String VIKTIG_TEKST = "Du har fått et viktig brev fra Nav. Les det i din digitale postkasse (%s).";
	public static final String DEFAULT_TEKST = "Du har fått et brev fra Nav. Les det i din digitale postkasse (%s).";

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
		LeverandoerAdresse leverandoerAdresse = findByOrganisasjonsnummer(leverandoer);
		return leverandoerAdresse.getNavn();
	}

	private static String varslingstekst(String tekst, String leverandoerNavn) {
		return format(tekst, leverandoerNavn);
	}
}
