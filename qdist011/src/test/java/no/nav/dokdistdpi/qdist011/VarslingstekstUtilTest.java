package no.nav.dokdistdpi.qdist011;

import org.junit.jupiter.api.Test;

import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdpi.qdist011.Utils.LeverandoerAdresse.DIGIPOST;
import static no.nav.dokdistdpi.qdist011.Utils.LeverandoerAdresse.EBOKS;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.determineVarslingstekst;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarslingstekstUtilTest {

	private static final String VEDTAK_DIGIPOST = "Du har fått et vedtak fra NAV. Les det i din digitale postkasse (Digipost).";
	private static final String VIKTIG_DIGIPOST = "Du har fått et viktig brev fra NAV. Les det i din digitale postkasse (Digipost).";
	private static final String DEFAULT_EBOKS = "Du har fått et brev fra NAV. Les det i din digitale postkasse (eBoks).";

	@Test
	public void shouldTestNullInput() {
		String varslingTekst = determineVarslingstekst(null, DIGIPOST.getOrganisasjonsnummer());
		assertEquals(VIKTIG_DIGIPOST, varslingTekst);
	}

	@Test
	public void shouldTestViktigInput() {
		String varslingTekst = determineVarslingstekst(VIKTIG, DIGIPOST.getOrganisasjonsnummer());
		assertEquals(VIKTIG_DIGIPOST, varslingTekst);
	}

	@Test
	public void shouldTestAnnetInput() {
		String varslingTekst = determineVarslingstekst(ANNET, EBOKS.getOrganisasjonsnummer());
		assertEquals(DEFAULT_EBOKS, varslingTekst);
	}

	@Test
	public void shouldTestVedtakInput() {
		String varslingTekst = determineVarslingstekst(VEDTAK, DIGIPOST.getOrganisasjonsnummer());
		assertEquals(VEDTAK_DIGIPOST, varslingTekst);
	}
}
