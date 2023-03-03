package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.Notifikasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VIKTIG;
import static no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper.VARSELTITTEL_ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper.VARSELTITTEL_VEDTAK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OppdaterVarselInfoMapperTest {

	private static final String VEDTAK_TEKST = "Du har fått et vedtak fra NAV. Les det i din digitale postkasse.";
	private static final String EPOSTADRESSE = "xyz@nav.no";
	private static final String MOBILNUMMER = "11111111";
	private static final String FORSENDELSE_ID = "11";

	public OppdaterVarselInfoMapper mapper = new OppdaterVarselInfoMapper();


	@Test
	public void shouldMapEpostVarsel() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(FORSENDELSE_ID, Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), VEDTAK.name());

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());
		oppdaterVarselInfo.notifikasjoner().stream().forEach(notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(VARSELTITTEL_VEDTAK, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});

	}

	@Test
	public void shouldMapBothEpostAndSMSVarsel() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(FORSENDELSE_ID, Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.smsvarsel(createSmsVarsel())
				.build(), VEDTAK.name());

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		List<Notifikasjon> notifikasjons = oppdaterVarselInfo.notifikasjoner().stream()
				.sorted(Comparator.comparing(Notifikasjon::kanal))
				.collect(Collectors.toList());

		assertEquals(EPOSTADRESSE, notifikasjons.get(0).kontaktInfo());
		assertEquals(VEDTAK_TEKST, notifikasjons.get(0).tekst());
		assertEquals("EPOST", notifikasjons.get(0).kanal());
		assertEquals(VARSELTITTEL_VEDTAK, notifikasjons.get(0).tittel());
		assertNotNull(notifikasjons.get(0).varslingstidspunkt());

		assertEquals(MOBILNUMMER, notifikasjons.get(1).kontaktInfo());
		assertEquals(VEDTAK_TEKST, notifikasjons.get(1).tekst());
		assertEquals("MOBILTELEFON", notifikasjons.get(1).kanal());
		assertNull(notifikasjons.get(1).tittel());
		assertNotNull(notifikasjons.get(1).varslingstidspunkt());

	}

	@Test
	public void shouldMapEpostVarselTittelWhenDistribusjonsTypeIsVedtakOrViktig() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(FORSENDELSE_ID, Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), VIKTIG.name());

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach( notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(VARSELTITTEL_VEDTAK, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}

	@Test
	public void shouldMapEpostVarselTittelWhenDistribusjonsTypeIsAnnet() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(FORSENDELSE_ID, Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), ANNET.name());

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach( notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(VARSELTITTEL_ANNET, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}

	@Test
	public void epostVarselTittelMapsToNullWhenDistribusjonsTypeKodeIsNull() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(FORSENDELSE_ID, Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), null);

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach( notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertNull(notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}


	private static EpostVarsel createEpostVarsel() {
		return EpostVarsel.builder()
				.epostadresse(EPOSTADRESSE)
				.varslingstekst(VEDTAK_TEKST)
				.build();
	}

	private static SmsVarsel createSmsVarsel() {
		return SmsVarsel.builder()
				.mobiltelefonnummer(MOBILNUMMER)
				.varslingstekst(VEDTAK_TEKST)
				.build();
	}

}