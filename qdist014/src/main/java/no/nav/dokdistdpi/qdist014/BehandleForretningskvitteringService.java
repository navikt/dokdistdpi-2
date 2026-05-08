package no.nav.dokdistdpi.qdist014;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import no.nav.dokdistdpi.qdist014.map.ForretningsKvitteringMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;

@Component
public class BehandleForretningskvitteringService {

	private final ForretningsKvitteringMapper forretningsKvitteringMapper;
	private final LagreJuridiskLoggService lagreJuridiskLoggService;
	private final JsonMapper jsonMapper;

	public BehandleForretningskvitteringService(ForretningsKvitteringMapper forretningsKvitteringMapper,
												LagreJuridiskLoggService lagreJuridiskLoggService,
												JsonMapper jsonMapper) {
		this.forretningsKvitteringMapper = forretningsKvitteringMapper;
		this.lagreJuridiskLoggService = lagreJuridiskLoggService;
		this.jsonMapper = jsonMapper;
	}

	@Handler
	public DpiMelding behandleForretningskvittering(String sbdJsonString, Exchange exchange) {
		try {
			SimpleStandardBusinessDocument simpleSbd = jsonMapper.readValue(sbdJsonString, SimpleStandardBusinessDocument.class);
			lagreJuridiskLoggService.lagreJuridiskLogg(new JuridiskLoggMetadata(simpleSbd.getDokumentKonversasjonId(), simpleSbd.getSender(), simpleSbd.getReceiver()), sbdJsonString);
			exchange.setProperty(PROPERTY_CONVERSATION_ID, simpleSbd.getConversationId());
			return forretningsKvitteringMapper.mapForretningsKvittering(simpleSbd, sbdJsonString);
		} catch (JacksonException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}
}
