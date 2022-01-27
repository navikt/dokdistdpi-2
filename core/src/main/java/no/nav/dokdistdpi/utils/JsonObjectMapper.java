package no.nav.dokdistdpi.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokdistdpi.consumer.dpi.JacksonConfig;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.getByValue;

public class JsonObjectMapper {

	private static final String SBD = "standardBusinessDocument";

	private JsonObjectMapper() {
	}

	public static SimpleStandardBusinessDocument mapSimpleSbd(String jwtPayload) {
		try {
			return new JacksonConfig().dpiObjectMapper().readValue(jwtPayload, SimpleStandardBusinessDocument.class);
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	public static DpiKvittering mapKvittering(String jsonPayload) {
		try {
			ObjectMapper mapper = new JacksonConfig().dpiObjectMapper();
			JsonNode jsonNode = mapper.readTree(jsonPayload);
			JsonNode feilJsnode = jsonNode.path(SBD).path(FEILET.getValue());
			JsonNode varslingJsnode = jsonNode.path(SBD).path(VARSLINGFEILET.getValue());
			JsonNode leveringsKvittering = jsonNode.path(SBD).path(LEVERING.getValue());
			SimpleStandardBusinessDocument simpleSbd = mapSimpleSbd(jsonPayload);
			DpiKvittering dpiKvittering = new DpiKvittering();

			switch (getByValue(simpleSbd.getType())) {
				case VARSLINGFEILET -> {
					VarslingFeiletKvittering varslingfeiletKvittering = mapper.convertValue(varslingJsnode, VarslingFeiletKvittering.class);
					dpiKvittering.setVarslingfeiletkvittering(varslingfeiletKvittering);
				}
				case LEVERING -> {
					LeveringsKvittering leveringKvittering = mapper.convertValue(leveringsKvittering, LeveringsKvittering.class);
					dpiKvittering.setLeveringskvittering(leveringKvittering);
				}
				case FEILET -> {
					DpiFeilKvittering feilKvittering = mapper.convertValue(feilJsnode, DpiFeilKvittering.class);
					dpiKvittering.setFeil(feilKvittering);
				}
			}
			return dpiKvittering;
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}
}
