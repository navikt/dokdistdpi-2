package no.nav.dokdistdpi.qdist014.map;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.SimpleStandardBusinessDocument;
import no.nav.dokdistdpi.exception.technical.SikkerDigitalPostException;
import no.nav.dokdistdpi.utils.JsonObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.getByValue;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;

@Slf4j
@Component
public class ForretningsKvitteringMapper {

	private static final Pattern MOBILNUMMER_REGEX = Pattern.compile("(0047|\\+47|47)?\\d{8}");
	private static final Pattern EPOST_REGEX = Pattern.compile("[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*");

	@Handler
	public DpiMelding mapForretningsKvittering(String sbdJsonString, Exchange exchange) {
		SimpleStandardBusinessDocument simpleSbd = JsonObjectMapper.mapSimpleSbd(sbdJsonString);
		DpiKvittering dpiKvittering = JsonObjectMapper.mapKvittering(sbdJsonString);
		exchange.setProperty(PROPERTY_CONVERSATION_ID, simpleSbd.getKonversasjonId());
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, simpleSbd.getDokumentKonversasjonId());

		switch (getByValue(simpleSbd.getType())) {
			case VARSLINGFEILET -> {
				VarslingFeiletKvittering varslingFeilet = dpiKvittering.getVarslingfeiletkvittering();
				log.warn("Kvittering varslingfeilet: {}", maskerBeskrivelse(varslingFeilet));
				return VarslingFeiletKvittering.builder()
						.konversasjonsId(simpleSbd.getKonversasjonId())
						.documentIdentification(simpleSbd.getDokumentKonversasjonId())
						.kvitteringType(VARSLINGFEILET)
						.tidspunkt(varslingFeilet.getTidspunkt())
						.varslingskanal(varslingFeilet.getVarslingskanal())
						.beskrivelse(varslingFeilet.getBeskrivelse())
						.build();
			}
			case LEVERING -> {
				return LeveringsKvittering.builder()
						.konversasjonsId(simpleSbd.getKonversasjonId())
						.documentIdentification(simpleSbd.getDokumentKonversasjonId())
						.kvitteringType(LEVERING)
						.tidspunkt(dpiKvittering.getLeveringskvittering().getTidspunkt())
						.build();
			}
			case FEILET -> {
				DpiFeilKvittering dpiFeilKvittering = dpiKvittering.getFeil();
				log.warn("Kvittering feilet: {}", dpiFeilKvittering.getDetaljer());
				return DpiFeilKvittering.builder()
						.konversasjonsId(simpleSbd.getKonversasjonId())
						.documentIdentification(simpleSbd.getDokumentKonversasjonId())
						.kvitteringType(FEILET)
						.tidspunkt(dpiFeilKvittering.getTidspunkt())
						.feiltype(dpiFeilKvittering.getFeiltype())
						.detaljer(dpiFeilKvittering.getDetaljer())
						.build();
			}
		}
		throw new SikkerDigitalPostException("Kvittering tilbake fra meldingsformidler var verken kvittering eller feil");
	}

	String maskerBeskrivelse(VarslingFeiletKvittering varslingFeiletKvittering) {
		final String beskrivelse = varslingFeiletKvittering.getBeskrivelse();
		switch (varslingFeiletKvittering.getVarslingskanal()) {
			case "sms":
				Matcher mobilmatcher = MOBILNUMMER_REGEX.matcher(beskrivelse);
				if (mobilmatcher.find()) {
					return mobilmatcher.replaceAll("********");
				}
				return beskrivelse;
			case "epost":
				Matcher epostmatcher = EPOST_REGEX.matcher(beskrivelse);
				if (epostmatcher.find()) {
					return epostmatcher.replaceAll("********@****.***");
				}
				return beskrivelse;
			default:
				return beskrivelse;
		}
	}
}
