package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.exception.functional.SafJournalpostValidationException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

public class DokdistdpiUtils {

	private DokdistdpiUtils() {
	}

	public static void assertNotNull(String feltnavn, Object obj) {
		if (isEmpty(obj)) {
			throw new IllegalArgumentException(format("%s kan ikke være null", feltnavn));
		}
	}

	public static void assertNotBlank(String feltnavn, String str) {
		if (isBlank(str)) {
			throw new IllegalArgumentException(format("%s kan ikke være null", feltnavn));
		}
	}

	public static void assertFieldOnSafDokumenterNotNullOrEmpty(String field, String value, String journalpostId, String dokumentInfoId) {
		if (isBlank(value)) {
			throw new SafJournalpostValidationException(format("Feltet %s kan ikke være null eller tomt i journalpost-respons fra SAF. journalpostId=%s, dokumentInfoId=%s", field, journalpostId, dokumentInfoId));
		}
	}
}
