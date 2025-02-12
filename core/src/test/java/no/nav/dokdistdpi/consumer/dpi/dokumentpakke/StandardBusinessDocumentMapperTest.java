package no.nav.dokdistdpi.consumer.dpi.dokumentpakke;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Dokumentpakkefingeravtrykk;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.Partner;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.PartnerIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.Scope;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.sbdh.StandardBusinessDocument;
import no.nav.dokdistdpi.utils.ForsendelseData;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DIGITALPOST_FORRETNINGSMELDING;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DOCUMENT_IDENTIFICATOR_STANDARD;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.HEADER_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.IDENTIFIER_AUTHORITY;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.SCOPE_CONVERSATION_ID_IDENTIFIER;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.TYPE_VERSION;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.utils.ForsendelseData.CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.ForsendelseData.forsendelse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardBusinessDocumentMapperTest {

	private final StandardBusinessDocumentMapper mapper = new StandardBusinessDocumentMapper();

	@Test
	void shouldMapDigitalPostKonvolutt() {
		final StandardBusinessDocument sbd = mapper.mapDigitalPostEnvelope(forsendelse(Dokumentpakke.builder().build()), Dokumentpakkefingeravtrykk
				.builder()
						.digestMethod("")
						.digestValue("fsdfsdfsdfsdfsd")
				.build());
		assertThat(sbd.getStandardBusinessDocumentHeader().getHeaderVersion()).isEqualTo(HEADER_VERSION);
		assertEquals(HEADER_VERSION, sbd.getStandardBusinessDocumentHeader().getHeaderVersion());


		assertThat(sbd.getStandardBusinessDocumentHeader().getSender())
				.extracting(Partner::getIdentifier)
				.extracting(PartnerIdentification::getAuthority).contains(IDENTIFIER_AUTHORITY);
		assertThat(sbd.getStandardBusinessDocumentHeader().getSender())
				.extracting(Partner::getIdentifier)
				.extracting(PartnerIdentification::getValue).contains(asIso6523(NAV_ORGNUMMER));
		assertThat(sbd.getStandardBusinessDocumentHeader().getReceiver())
				.extracting(Partner::getIdentifier)
				.extracting(PartnerIdentification::getAuthority).contains(IDENTIFIER_AUTHORITY);
		assertThat(sbd.getStandardBusinessDocumentHeader().getReceiver())
				.extracting(Partner::getIdentifier)
				.extracting(PartnerIdentification::getValue).contains(asIso6523(ForsendelseData.MOTTAKER_ORGNO));
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getStandard()).isEqualTo(DOCUMENT_IDENTIFICATOR_STANDARD);
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getTypeVersion()).isEqualTo(TYPE_VERSION);
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier()).isEqualTo(CONVERSATION_ID);
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getType()).isEqualTo(DIGITALPOST_FORRETNINGSMELDING);
		assertThat(sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope()).hasSize(1);
		assertThat(sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope())
				.anyMatch(scope -> SCOPE_CONVERSATION_ID.equals(scope.getType()))
				.flatExtracting(Scope::getInstanceIdentifier, Scope::getIdentifier)
				.contains(CONVERSATION_ID, SCOPE_CONVERSATION_ID_IDENTIFIER);
		assertThat(sbd.getAny()).isInstanceOf(DigitalPost.class);
	}

}