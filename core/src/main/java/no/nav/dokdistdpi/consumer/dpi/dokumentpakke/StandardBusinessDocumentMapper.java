package no.nav.dokdistdpi.consumer.dpi.dokumentpakke;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.BusinessScope;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.DocumentIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.Partner;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.PartnerIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.Scope;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocumentHeader;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DIGITALPOST_FORRETNINGSMELDING;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DOCUMENT_IDENTIFICATOR_STANDARD;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.HEADER_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.IDENTIFIER_AUTHORITY;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID_IDENTIFIER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.TYPE_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DEFAULT_ZONE_ID;

@Component
public class StandardBusinessDocumentMapper {

	public StandardBusinessDocument mapDigitalPostEnvelope(Forsendelse forsendelse) {
		StandardBusinessDocumentHeader standardBusinessDocumentHeader = new StandardBusinessDocumentHeader();
		standardBusinessDocumentHeader.setHeaderVersion(HEADER_VERSION);
		standardBusinessDocumentHeader.addSender(createSender());
		standardBusinessDocumentHeader.addReceiver(createReceiver(forsendelse.getDigitalPostLeverandoerAdresse()));
		standardBusinessDocumentHeader.setDocumentIdentification(createDocumentIdentification(forsendelse.getBestillingsId()));
		BusinessScope businessScope = new BusinessScope();
		businessScope.addScope(createConversationIdScope(forsendelse.getKonversasjonId()));
		standardBusinessDocumentHeader.setBusinessScope(businessScope);
		return StandardBusinessDocument.builder()
				.standardBusinessDocumentHeader(standardBusinessDocumentHeader)
				.any(forsendelse.getDigital())
				.build();
	}

	private DocumentIdentification createDocumentIdentification(final String instanceIdentifier) {
		return DocumentIdentification.builder()
				.standard(DOCUMENT_IDENTIFICATOR_STANDARD)
				.typeVersion(TYPE_VERSION)
				.instanceIdentifier(instanceIdentifier)
				.type(DIGITALPOST_FORRETNINGSMELDING)
				.creationDateAndTime(OffsetDateTime.now(DEFAULT_ZONE_ID))
				.build();
	}

	private Partner createSender() {
		return Partner.builder()
				.identifier(PartnerIdentification.builder()
						.authority(IDENTIFIER_AUTHORITY)
						.value(asIso6523(NAV_ORGNUMMER))
						.build())
				.build();
	}

	private Partner createReceiver(final String mottakerOrgNo) {
		return Partner.builder()
				.identifier(PartnerIdentification.builder()
						.authority(IDENTIFIER_AUTHORITY)
						.value(asIso6523(mottakerOrgNo))
						.build())
				.build();
	}

	private Scope createConversationIdScope(final String instanceIdentifier) {
		return Scope.builder()
				.type(SCOPE_CONVERSATION_ID)
				.instanceIdentifier(instanceIdentifier)
				.identifier(SCOPE_CONVERSATION_ID_IDENTIFIER)
				.build();
	}
}
