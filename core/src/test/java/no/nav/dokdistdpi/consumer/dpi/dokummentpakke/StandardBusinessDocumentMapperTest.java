package no.nav.dokdistdpi.consumer.dpi.dokummentpakke;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.Partner;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.PartnerIdentification;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.Scope;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.sbdh.StandardBusinessDocument;
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
import static no.nav.dokdistdpi.utils.ForsendelseData.BESTILLINGS_ID;
import static no.nav.dokdistdpi.utils.ForsendelseData.CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.ForsendelseData.forsendelse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardBusinessDocumentMapperTest {
	private static final String KONVERSASJON_ID = "konversasjonId1";
	private static final String FIXED_TIME = "2020-01-01T12:00:00Z";
	private static final String TEN_SECONDS_BEFORE = "2020-01-01T12:59:50+01:00";
	private static final String TWENTY_FOUR_HOURS_LATER = "2020-01-06T13:00:00+01:00";
	private final StandardBusinessDocumentMapper mapper = new StandardBusinessDocumentMapper();

	@Test
	void shouldMapDigitalPostKonvolutt() {
		final StandardBusinessDocument sbd = mapper.mapDigitalPostEnvelope(forsendelse(Dokumentpakke.builder().build()));
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
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier()).isEqualTo(BESTILLINGS_ID);
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getType()).isEqualTo(DIGITALPOST_FORRETNINGSMELDING);
		assertThat(sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getMultipleType()).isTrue();
		assertThat(sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope()).hasSize(1);
		assertThat(sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope())
				.anyMatch(scope -> SCOPE_CONVERSATION_ID.equals(scope.getType()))
				.flatExtracting(Scope::getInstanceIdentifier, Scope::getIdentifier)
				.contains(CONVERSATION_ID, SCOPE_CONVERSATION_ID_IDENTIFIER);
		assertThat(sbd.getAny()).isInstanceOf(DigitalPost.class);

	}
}