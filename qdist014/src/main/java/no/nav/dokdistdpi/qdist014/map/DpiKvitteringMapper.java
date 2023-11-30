package no.nav.dokdistdpi.qdist014.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;

@Component
class DpiKvitteringMapper {

	private static final String SBD = "standardBusinessDocument";
	static final String AAPNINGSKVITTERING_ERROR_MESSAGE = "Åpningskvitteringer blir bare sendt dersom dette er bestilt av Avsender i digital meldingen ved å spesifisere dette med propertien aapningskvittering. " +
														   "NAV setter ikke aapningskvittering og kan ikke behandle denne";
	static final String MOTTAKSKVITTERING_ERROR_MESSAGE = "Denne kvitteringen leveres tilbake så fort utskrift og forsendelsestjenesten har mottatt forsendelsen og validert at den kan skrives ut. " +
														  "NAV benytter seg ikke utskriftstjenesten til DPI og kan ikke behandle denne";
	private final ObjectMapper dpiObjectMapper;

	public DpiKvitteringMapper(@Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper) {
		this.dpiObjectMapper = dpiObjectMapper;
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
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	private DpiKvittering mapVarslingfeilet(String jsonPayload) throws JsonProcessingException {
		JsonNode jsonNode = dpiObjectMapper.readTree(jsonPayload);
		JsonNode varslingfeiletJsonNode = jsonNode.path(SBD).path(VARSLINGFEILET.getValue());
		VarslingFeiletKvittering varslingfeiletKvittering = dpiObjectMapper.convertValue(varslingfeiletJsonNode, VarslingFeiletKvittering.class);
		return new DpiKvittering(null, varslingfeiletKvittering, null);
	}

	private DpiKvittering mapLevering(String jsonPayload) throws JsonProcessingException {
		JsonNode jsonNode = dpiObjectMapper.readTree(jsonPayload);
		JsonNode leveringJsonNode = jsonNode.path(SBD).path(LEVERING.getValue());
		LeveringsKvittering leveringKvittering = dpiObjectMapper.convertValue(leveringJsonNode, LeveringsKvittering.class);
		return new DpiKvittering(null, null, leveringKvittering);
	}

	private DpiKvittering mapFeilet(String jsonPayload) throws JsonProcessingException {
		JsonNode jsonNode = dpiObjectMapper.readTree(jsonPayload);
		JsonNode feilJsonNode = jsonNode.path(SBD).path(FEILET.getValue());
		DpiFeilKvittering feilKvittering = dpiObjectMapper.convertValue(feilJsonNode, DpiFeilKvittering.class);
		return new DpiKvittering(feilKvittering, null, null);
	}
}
