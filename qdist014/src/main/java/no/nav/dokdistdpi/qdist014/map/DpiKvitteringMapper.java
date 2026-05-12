package no.nav.dokdistdpi.qdist014.map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;

@Component
class DpiKvitteringMapper {

	private static final String SBD = "standardBusinessDocument";
	static final String AAPNINGSKVITTERING_ERROR_MESSAGE = "Åpningskvitteringer blir bare sendt dersom dette er bestilt av Avsender i digital meldingen ved å spesifisere dette med propertien aapningskvittering. " +
														   "Nav setter ikke aapningskvittering og kan ikke behandle denne";
	static final String MOTTAKSKVITTERING_ERROR_MESSAGE = "Denne kvitteringen leveres tilbake så fort utskrift og forsendelsestjenesten har mottatt forsendelsen og validert at den kan skrives ut. " +
														  "Nav benytter seg ikke utskriftstjenesten til DPI og kan ikke behandle denne";
	private final JsonMapper jsonMapper;

	public DpiKvitteringMapper(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	DpiKvittering mapKvittering(KvitteringType kvitteringType, String jsonPayload) {
		try {
			return switch (kvitteringType) {
				// https://docs.digdir.no/dpi_aapningskvittering.html
				case AAPNING -> throw new UnsupportedOperationException(AAPNINGSKVITTERING_ERROR_MESSAGE);
				// https://docs.digdir.no/dpi_varslingfeiletkvittering.html
				case VARSLINGFEILET -> mapVarslingfeilet(jsonPayload);
				// https://docs.digdir.no/dpi_leveringskvittering.html
				case LEVERING -> mapLevering(jsonPayload);
				// https://docs.digdir.no/dpi_mottakskvittering.html
				case MOTTAK -> throw new UnsupportedOperationException(MOTTAKSKVITTERING_ERROR_MESSAGE);
				// https://docs.digdir.no/dpi_feil.html
				case FEILET -> mapFeilet(jsonPayload);
			};
		} catch (JacksonException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	private DpiKvittering mapVarslingfeilet(String jsonPayload) {
		JsonNode jsonNode = jsonMapper.readTree(jsonPayload);
		JsonNode varslingfeiletJsonNode = jsonNode.path(SBD).path(VARSLINGFEILET.getValue());
		VarslingFeiletKvittering varslingfeiletKvittering = jsonMapper.convertValue(varslingfeiletJsonNode, VarslingFeiletKvittering.class);
		return new DpiKvittering(null, varslingfeiletKvittering, null);
	}

	private DpiKvittering mapLevering(String jsonPayload) {
		JsonNode jsonNode = jsonMapper.readTree(jsonPayload);
		JsonNode leveringJsonNode = jsonNode.path(SBD).path(LEVERING.getValue());
		LeveringsKvittering leveringKvittering = jsonMapper.convertValue(leveringJsonNode, LeveringsKvittering.class);
		return new DpiKvittering(null, null, leveringKvittering);
	}

	private DpiKvittering mapFeilet(String jsonPayload) {
		JsonNode jsonNode = jsonMapper.readTree(jsonPayload);
		JsonNode feilJsonNode = jsonNode.path(SBD).path(FEILET.getValue());
		DpiFeilKvittering feilKvittering = jsonMapper.convertValue(feilJsonNode, DpiFeilKvittering.class);
		return new DpiKvittering(feilKvittering, null, null);
	}
}
