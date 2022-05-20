package no.nav.dokdistdpi.qdist011;

import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.DEFAULT_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.VEDTAK_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.VIKTIG_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.determineVarslingstekst;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarslingstekstUtilTest {

	@Test
	public void shouldTestNullInput(){
		String varslingTekst = determineVarslingstekst(null);
		assertEquals(VIKTIG_TEKST, varslingTekst);
	}

	@Test
	public void shouldTestViktigInput(){
		String varslingTekst = determineVarslingstekst(VIKTIG);
		assertEquals(VIKTIG_TEKST, varslingTekst);
	}

	@Test
	public void shouldTestAnnetInput(){
		String varslingTekst = determineVarslingstekst(ANNET);
		assertEquals(DEFAULT_TEKST, varslingTekst);
	}

	@Test
	public void shouldTestVedtakInput(){
		String varslingTekst = determineVarslingstekst(VEDTAK);
		assertEquals(VEDTAK_TEKST, varslingTekst);
	}
}
