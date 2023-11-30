package no.nav.dokdistdpi.qdist014.map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.JsonParserTechnicalException;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.getByValue;
import static no.nav.dokdistdpi.qdist014.map.DpiKvitteringMapper.AAPNINGSKVITTERING_ERROR_MESSAGE;
import static no.nav.dokdistdpi.qdist014.map.DpiKvitteringMapper.MOTTAKSKVITTERING_ERROR_MESSAGE;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;

@Slf4j
@Component
public class ForretningsKvitteringMapper {

	private static final Pattern MOBILNUMMER_REGEX = Pattern.compile("(0047|\\+47|47)?\\d{8}");
	private static final Pattern EPOST_REGEX = Pattern.compile("[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*");

	private final DpiKvitteringMapper dpiKvitteringMapper;
	private final ObjectMapper dpiObjectMapper;

	public ForretningsKvitteringMapper(DpiKvitteringMapper dpiKvitteringMapper,
									   @Qualifier("dpiObjectMapper") ObjectMapper dpiObjectMapper) {
		this.dpiObjectMapper = dpiObjectMapper;
		this.dpiKvitteringMapper = dpiKvitteringMapper;
	}

	@Handler
	public DpiMelding mapForretningsKvittering(String sbdJsonString, Exchange exchange) {
		try {
			SimpleStandardBusinessDocument simpleSbd = dpiObjectMapper.readValue(sbdJsonString, SimpleStandardBusinessDocument.class);

			KvitteringType kvitteringType = getByValue(simpleSbd.getType());
			DpiKvittering dpiKvittering = dpiKvitteringMapper.mapKvittering(kvitteringType, sbdJsonString);
			exchange.setProperty(PROPERTY_CONVERSATION_ID, simpleSbd.getConversationId());

			return switch (kvitteringType) {
				case AAPNING -> throw new UnsupportedOperationException(AAPNINGSKVITTERING_ERROR_MESSAGE);
				case VARSLINGFEILET -> {
					VarslingFeiletKvittering varslingFeilet = dpiKvittering.getVarslingfeiletkvittering();
					log.info("Kvittering varslingfeilet(kanal={}): {}", varslingFeilet.getVarslingskanal(), maskerBeskrivelse(varslingFeilet));
					yield VarslingFeiletKvittering.builder()
							.konversasjonsId(simpleSbd.getConversationId())
							.documentIdentification(simpleSbd.getDokumentKonversasjonId())
							.kvitteringType(VARSLINGFEILET)
							.tidspunkt(varslingFeilet.getTidspunkt())
							.varslingskanal(varslingFeilet.getVarslingskanal())
							.beskrivelse(varslingFeilet.getBeskrivelse())
							.build();
				}
				case LEVERING -> LeveringsKvittering.builder()
						.konversasjonsId(simpleSbd.getConversationId())
						.documentIdentification(simpleSbd.getDokumentKonversasjonId())
						.kvitteringType(LEVERING)
						.tidspunkt(dpiKvittering.getLeveringskvittering().getTidspunkt())
						.build();
				case MOTTAK -> throw new UnsupportedOperationException(MOTTAKSKVITTERING_ERROR_MESSAGE);
				case FEILET -> {
					DpiFeilKvittering dpiFeilKvittering = dpiKvittering.getFeil();
					log.warn("Kvittering feilet: {}", dpiFeilKvittering.getDetaljer());
					yield DpiFeilKvittering.builder()
							.konversasjonsId(simpleSbd.getConversationId())
							.documentIdentification(simpleSbd.getDokumentKonversasjonId())
							.kvitteringType(FEILET)
							.tidspunkt(dpiFeilKvittering.getTidspunkt())
							.feiltype(dpiFeilKvittering.getFeiltype())
							.detaljer(dpiFeilKvittering.getDetaljer())
							.build();
				}
			};
		} catch (JsonProcessingException e) {
			throw new JsonParserTechnicalException("Feilet å mappe StandardBusinessDocument", e);
		}
	}

	String maskerBeskrivelse(VarslingFeiletKvittering varslingFeiletKvittering) {
		final String beskrivelse = varslingFeiletKvittering.getBeskrivelse();
		return switch (varslingFeiletKvittering.getVarslingskanal()) {
			case "sms":
				Matcher mobilmatcher = MOBILNUMMER_REGEX.matcher(beskrivelse);
				if (mobilmatcher.find()) {
					yield mobilmatcher.replaceAll("********");
				}
				yield beskrivelse;
			case "epost":
				Matcher epostmatcher = EPOST_REGEX.matcher(beskrivelse);
				if (epostmatcher.find()) {
					yield epostmatcher.replaceAll("********@****.***");
				}
				yield beskrivelse;
			default:
				yield beskrivelse;
		};
	}
}
