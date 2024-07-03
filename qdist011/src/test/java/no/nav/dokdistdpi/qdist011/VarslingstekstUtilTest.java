package no.nav.dokdistdpi.qdist011;

import no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdpi.qdist011.Utils.LeverandoerAdresse.DIGIPOST;
import static no.nav.dokdistdpi.qdist011.Utils.LeverandoerAdresse.EBOKS;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.DEFAULT_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.LEVERANDOER_E_BOKS;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.VEDTAK_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.VIKTIG_TEKST;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.determineVarslingstekst;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarslingstekstUtilTest {

	@Test
	public void shouldTestNullInput() {
		String varslingTekst = determineVarslingstekst(null, DIGIPOST.getValue());
		assertEquals(format(VIKTIG_TEKST, VarslingstekstUtil.LEVERANDOER_DIGIPOST).strip(), varslingTekst);
	}

	@Test
	public void shouldTestViktigInput() {
		String varslingTekst = determineVarslingstekst(VIKTIG, DIGIPOST.getValue());
		assertEquals(format(VIKTIG_TEKST, VarslingstekstUtil.LEVERANDOER_DIGIPOST), varslingTekst);
	}

	@Test
	public void shouldTestAnnetInput() {
		String varslingTekst = determineVarslingstekst(ANNET, EBOKS.getValue());
		assertEquals(format(DEFAULT_TEKST, LEVERANDOER_E_BOKS), varslingTekst);
	}

	@Test
	public void shouldTestVedtakInput() {
		String varslingTekst = determineVarslingstekst(VEDTAK, DIGIPOST.getValue());
		assertEquals(format(VEDTAK_TEKST, VarslingstekstUtil.LEVERANDOER_DIGIPOST), varslingTekst);
	}
}
