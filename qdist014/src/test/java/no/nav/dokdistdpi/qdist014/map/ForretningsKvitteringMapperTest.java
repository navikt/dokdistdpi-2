package no.nav.dokdistdpi.qdist014.map;


import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.Feiltype.KLIENT;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ForretningsKvitteringMapperTest {

	private static final String CONVERSATION_ID = "37efbd4c-413d-4e2c-bbc5-257ef4a65a45";

	private Exchange exchange;
	private ForretningsKvitteringMapper mapper;

	@BeforeEach
	void setUp() {
		CamelContext camelContext = new DefaultCamelContext();
		exchange = new DefaultExchange(camelContext);
		mapper = new ForretningsKvitteringMapper();
	}

	@Test
	void shouldMapLeveringskvittering() {
		String sbd = Testutil.classpathToString("__files/kvitteringer/leveringskvittering.json");

		LeveringsKvittering leveringsKvittering = (LeveringsKvittering) mapper.mapForretningsKvittering(sbd, exchange);

		assertEquals(CONVERSATION_ID, leveringsKvittering.getKonversasjonsId());
		assertEquals(LEVERING, leveringsKvittering.getKvitteringType());
	}

	@Test
	void shouldMapFeilKvittering() {
		String sbd = Testutil.classpathToString("__files/kvitteringer/feilkvittering.json");

		DpiFeilKvittering feilKvittering = (DpiFeilKvittering) mapper.mapForretningsKvittering(sbd, exchange);
		assertEquals(CONVERSATION_ID, feilKvittering.getKonversasjonsId());
		assertEquals(KLIENT, feilKvittering.getFeiltype());
		assertEquals(KvitteringType.FEILET, feilKvittering.getKvitteringType());
	}

	@ParameterizedTest
	@ValueSource(strings = {"88888888", "+4788888888"})
	void shouldMaskMobilnummer(String mobilnummer) {
		VarslingFeiletKvittering varslingFeiletKvittering = VarslingFeiletKvittering.builder()
				.varslingskanal("sms")
				.beskrivelse("Sms til " + mobilnummer + " feilet")
				.build();
		String maskertBeskrivelse = mapper.maskerBeskrivelse(varslingFeiletKvittering);
		assertThat(maskertBeskrivelse).isEqualTo("Sms til ******** feilet");
	}

	@ParameterizedTest
	@ValueSource(strings = {"benjamin@chang.community", "benjamin+senorchang@chang.community"})
	void shouldMaskEpostadresse(String epostadresse) {
		VarslingFeiletKvittering varslingFeiletKvittering = VarslingFeiletKvittering.builder()
				.varslingskanal("epost")
				.beskrivelse("Epost til " + epostadresse + " feilet")
				.build();
		String maskertBeskrivelse = mapper.maskerBeskrivelse(varslingFeiletKvittering);
		assertThat(maskertBeskrivelse).isEqualTo("Epost til ********@****.*** feilet");
	}
}