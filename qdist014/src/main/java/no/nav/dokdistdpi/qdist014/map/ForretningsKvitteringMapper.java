package no.nav.dokdistdpi.qdist014.map;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.DigitalPostTechnicalException;
import no.nav.dokdistdpi.utils.JsonObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.getByValue;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;

@Slf4j
@Component
public class ForretningsKvitteringMapper {

	@Handler
	public DpiMelding mapForretningsKvittering(String sbdJsonString, Exchange exchange) {
		SimpleStandardBusinessDocument simpleSbd = JsonObjectMapper.mapSimpleSbd(sbdJsonString);
		DpiKvittering dpiKvittering = JsonObjectMapper.mapKvittering(sbdJsonString);
		exchange.setProperty(PROPERTY_CONVERSATION_ID, simpleSbd.getConversationId());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, simpleSbd.getBestillingsId());

		switch (getByValue(simpleSbd.getType())) {
			case VARSLINGFEILET -> {
				VarslingFeiletKvittering varslingFeilet = dpiKvittering.getVarslingfeiletkvittering();
				return VarslingFeiletKvittering.builder()
						.konversasjonsId(simpleSbd.getConversationId())
						.bestillingsId(simpleSbd.getBestillingsId())
						.kvitteringType(VARSLINGFEILET)
						.tidspunkt(varslingFeilet.getTidspunkt())
						.varslingskanal(varslingFeilet.getVarslingskanal())
						.beskrivelse(varslingFeilet.getBeskrivelse())
						.build();
			}
			case LEVERING -> {
				return LeveringsKvittering.builder()
						.konversasjonsId(simpleSbd.getConversationId())
						.bestillingsId(simpleSbd.getBestillingsId())
						.kvitteringType(LEVERING)
						.tidspunkt(dpiKvittering.getLeveringskvittering().getTidspunkt())
						.build();
			}
			case FEILET -> {
				DpiFeilKvittering dpiFeilKvittering = dpiKvittering.getFeil();
				return DpiFeilKvittering.builder()
						.konversasjonsId(simpleSbd.getConversationId())
						.bestillingsId(simpleSbd.getBestillingsId())
						.kvitteringType(FEILET)
						.tidspunkt(dpiFeilKvittering.getTidspunkt())
						.feiltype(dpiFeilKvittering.getFeiltype())
						.detaljer(dpiFeilKvittering.getDetaljer())
						.build();
			}
		}
		throw new DigitalPostTechnicalException("Kvittering tilbake fra meldingsformidler var verken kvittering eller feil");
	}

	public boolean erKvitteringBehandlet(DpiKvittering dpiKvittering) {
		return isNull(dpiKvittering) && (isNull(dpiKvittering.getFeil()) || isNull(dpiKvittering.getLeveringskvittering()) ||
				isNull(dpiKvittering.getVarslingfeiletkvittering()));
	}
}
