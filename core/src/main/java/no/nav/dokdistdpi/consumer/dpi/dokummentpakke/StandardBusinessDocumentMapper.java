package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.BusinessScope;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.DocumentIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.PartnerIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.Receiver;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.Scope;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.Sender;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.StandardBusinessDocumentHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DIGITALPOST_FORRETNINGSMELDING;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DOCUMENT_IDENTIFICATOR_STANDARD;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.HEADER_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.IDENTIFIER_AUTHORITY;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID_IDENTIFIER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.TYPE_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;

@Component
public class StandardBusinessDocumentMapper {

	private final Clock clock;

	@Autowired
	public StandardBusinessDocumentMapper(Clock clock) {
		this.clock = clock;
	}

	public StandardBusinessDocument mapDigitalPostEnvelope(Forsendelse forsendelse) {
		StandardBusinessDocumentHeader standardBusinessDocumentHeader = new StandardBusinessDocumentHeader();
		standardBusinessDocumentHeader.setHeaderVersion(HEADER_VERSION);
		standardBusinessDocumentHeader.addSender(createSender());
		standardBusinessDocumentHeader.addReceiver(createReceiver(forsendelse.getMottakerOrgNo()));
		standardBusinessDocumentHeader.setDocumentIdentification(createDocumentIdentification(forsendelse.getBestillingsId()));
		BusinessScope businessScope = new BusinessScope();
		businessScope.addScope(createConversationIdScope(forsendelse.getConversationId()));
		standardBusinessDocumentHeader.setBusinessScope(businessScope);
		return StandardBusinessDocument.builder()
				.standardBusinessDocumentHeader(standardBusinessDocumentHeader)
				.any(forsendelse.getDigital())
				.build();
	}

	private DocumentIdentification createDocumentIdentification(final String instanceIdentifier) {
		return DocumentIdentification.builder()
				.standard(DOCUMENT_IDENTIFICATOR_STANDARD)
				.typeVersion(TYPE_VERSION).instanceIdentifier(instanceIdentifier)
				.type(DIGITALPOST_FORRETNINGSMELDING).multipleType(true)
				.creationDateAndTime(OffsetDateTime.now(clock).minus(10, ChronoUnit.SECONDS)).build();
	}

	private Sender createSender() {
		return Sender.builder()
				.identifier(PartnerIdentification.builder()
						.authority(IDENTIFIER_AUTHORITY)
						.value(asIso6523(NAV_ORGNUMMER))
						.build())
				.build();
	}

	private Receiver createReceiver(final String mottakerOrgNo) {
		return Receiver.builder()
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
