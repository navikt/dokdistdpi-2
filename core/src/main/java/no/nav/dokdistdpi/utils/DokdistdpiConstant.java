package no.nav.dokdistdpi.utils;

import java.time.ZoneId;
import java.util.TimeZone;

public class DokdistdpiConstant {
	public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
	public static final ZoneId DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();

	public static final String CALL_ID = "callId";
	public static final String MDC_REQUEST_ID = "requestId";
	public static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
	public static final String NAV_CALL_ID = "Nav-Call-Id";
	public static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";
	public static final String APP_NAME = "dokdistdpi";
	public static final String BEARER_PREFIX = "Bearer ";
	public static final String NAV_PERSONIDENT = "Nav-Personident";
	public static final String NAV_PERSONIDENTER = "Nav-Personidenter";

	public static final String STS_CACHE = "stsCache";
	public static final String PROCESS = "process";
	public static final String DOK_REQUEST = "dok_request";
	public static final String DISTRIBUSJONS_SDP_KANAL = "SDP";
	public static final String EPOST = "EPOST";
	public static final String SMS = "SMS";

	public static final String FORSENDELSE_STATUS_KLAR_FOR_DIST = "KLAR_FOR_DIST";
	public static final String FORSENDELSE_STATUS_OVERSENDT = "OVERSENDT";
	public static final String FORSENDELSE_STATUS_EKSPEDERT = "EKSPEDERT";

	public static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	public static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";

	public static final int BACKOFF_DELAY = 500;
	public static final int BACKOFF_MULTIPLIER = 3;

	public static final String HOVEDDOKUMENT = "HOVEDDOKUMENT";
	public static final String VEDLEGG = "VEDLEGG";
	public static final String VEDLEGG_TITTEL_PREFIX = "Vedlegg ";

	private DokdistdpiConstant() {
		//no-op
	}
}
