package no.nav.dokdistdpi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokdistdpi.consumer.dpi.JacksonConfig;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;

public class JsonObjectMapper {

	private static final String SBD = "standardBusinessDocument";

	private JsonObjectMapper() {
	}

	public static SimpleStandardBusinessDocument mapSimpleSbd(String jwtPayload) {
		try {
			return new JacksonConfig().dpiObjectMapper().readValue(jwtPayload, SimpleStandardBusinessDocument.class);
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe JWT Forretningsmelding", e);
		}
	}

	public static DpiKvittering mapKvittering(String jsonPayload) {
		try {
			ObjectMapper mapper = new JacksonConfig().dpiObjectMapper();
			JsonNode jsonNode = mapper.readTree(jsonPayload);
			JsonNode feilJsnode = jsonNode.path(SBD).path(KvitteringType.FEILET.getValue());
			JsonNode leveringJsnode = jsonNode.path(SBD).path(KvitteringType.LEVERING.getValue());
			JsonNode varslingfeiletJsnode = jsonNode.path(SBD).path(KvitteringType.VARSLINGFEILET.getValue());
			VarslingFeiletKvittering varslingfeiletKvittering = mapper.convertValue(varslingfeiletJsnode, VarslingFeiletKvittering.class);
			DpiFeilKvittering kvittering = mapper.convertValue(feilJsnode, DpiFeilKvittering.class);
			LeveringsKvittering leveringKvittering = mapper.convertValue(leveringJsnode, LeveringsKvittering.class);
			DpiKvittering dpiKvittering = new DpiKvittering();
			dpiKvittering.setFeil(kvittering);
			dpiKvittering.setLeveringskvittering(leveringKvittering);
			dpiKvittering.setVarslingfeiletkvittering(varslingfeiletKvittering);
			return dpiKvittering;
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe JWT Forretningsmelding", e);
		}
	}


}
