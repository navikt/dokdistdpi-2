package no.nav.dokdistdpi.consumer.dpi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Organisasjonsnummer {
	static final Pattern ISO6523_PATTERN = Pattern.compile("^([0-9]{4}:)([0-9]{9})$");
	public static final String ISO6523_AUTHORITY = "iso6523-actorid-upis";
	public static final String ISO6523_PREFIX = "0192:";

	private Organisasjonsnummer() {
	}

	public static String asIso6523(final String orgNummer) {
		if(isIso6523(orgNummer)) {
			return orgNummer;
		}
		return ISO6523_PREFIX + orgNummer;
	}

	public static String fromIso6523(final String iso6523Orgnr) {
		Matcher matcher = ISO6523_PATTERN.matcher(iso6523Orgnr);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid organization number. " +
					"Expected format is ISO 6523, got following organization number: " + iso6523Orgnr);
		}
		return matcher.group(2);
	}

	public static boolean isIso6523(final String iso6523Orgnr) {
		return ISO6523_PATTERN.matcher(iso6523Orgnr).matches();
	}

}
