package no.nav.dokdistdpi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.dokdistdpi.consumer.dpi.JacksonConfig;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.ForretningsmeldingParseException;

public class JsonObjectMapper {

	private JsonObjectMapper() {
	}

	public static SimpleStandardBusinessDocument mapSimpleSbd(String jwtPayload) {
		try {
			return new JacksonConfig().dpiObjectMapper().readValue(jwtPayload, SimpleStandardBusinessDocument.class);
		} catch (JsonProcessingException e) {
			throw new ForretningsmeldingParseException("Feilet å mappe StandardBusinessDocument", e);
		}
	}
}
