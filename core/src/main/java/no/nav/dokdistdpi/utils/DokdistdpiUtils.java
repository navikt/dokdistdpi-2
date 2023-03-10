package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.exception.functional.SafJournalpostValidationException;
import org.apache.camel.Exchange;
import org.slf4j.MDC;

import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CALLID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.NAV_CONSUMER_ID;
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

	public static void setOrGenerateCallIdToMdc(Exchange exchange) {
		final String callId = exchange.getIn().getHeader(NAV_CALLID, String.class);
		if (isNull(callId) || isBlank(callId)) {
			String newCallId = UUID.randomUUID().toString();
			exchange.getIn().setHeader(NAV_CALLID, newCallId);
			MDC.put(NAV_CALLID, newCallId);
		} else {
			MDC.put(NAV_CALLID, callId);
		}
	}

	public static void setConsumerIdToMdc(Exchange exchange) {
		String consumerId = exchange.getIn().getHeader(NAV_CONSUMER_ID, String.class);
		if (!isBlank(consumerId)) {
			MDC.put(NAV_CONSUMER_ID, consumerId);
		}
	}
}
