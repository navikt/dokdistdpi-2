package no.nav.dokdistdpi.qdist014;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private final ObjectMapper dpiObjectMapper;

	public BehandleForretningskvitteringService(ForretningsKvitteringMapper forretningsKvitteringMapper,
												LagreJuridiskLoggService lagreJuridiskLoggService,
												ObjectMapper dpiObjectMapper) {
		this.forretningsKvitteringMapper = forretningsKvitteringMapper;
		this.lagreJuridiskLoggService = lagreJuridiskLoggService;
		this.dpiObjectMapper = dpiObjectMapper;
	}

	@Handler
	public DpiMelding behandleForretningskvittering(String sbdJsonString, Exchange exchange) {
		try {
			SimpleStandardBusinessDocument simpleSbd = dpiObjectMapper.readValue(sbdJsonString, SimpleStandardBusinessDocument.class);
			lagreJuridiskLoggService.lagreJuridiskLogg(new JuridiskLoggMetadata(simpleSbd.getDokumentKonversasjonId(), simpleSbd.getSender(), simpleSbd.getReceiver()), sbdJsonString);
			exchange.setProperty(PROPERTY_CONVERSATION_ID, simpleSbd.getConversationId());
			return forretningsKvitteringMapper.mapForretningsKvittering(simpleSbd, sbdJsonString);
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}
}
