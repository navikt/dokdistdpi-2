package no.nav.dokdistdpi.qdist014.map;


import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.Feiltype.KLIENT;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ForretningsKvitteringMapperTest {

	private static final String BESTILLINGS_ID = "ff88849c-e281-4809-8555-7cd54952b916";
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
		assertEquals(BESTILLINGS_ID, feilKvittering.getBestillingsId());
		assertEquals(KLIENT, feilKvittering.getFeiltype());
		assertEquals(KvitteringType.FEILET, feilKvittering.getKvitteringType());
	}

}