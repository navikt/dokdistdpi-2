package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.dpi.client.OppdaterDigitalAdresseRequest;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.VEDTAK;
import static no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper.EPOST_VARSELTITTEL_ANNET;
import static no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper.EPOST_VARSELTITTEL_VEDTAK;
import static no.nav.dokdistdpi.utils.ForsendelseData.EPOSTADRESSE;
import static no.nav.dokdistdpi.utils.ForsendelseData.FORSENDELSE_ID;
import static no.nav.dokdistdpi.utils.ForsendelseData.MOBILNUMMER;
import static no.nav.dokdistdpi.utils.ForsendelseData.VEDTAK_TEKST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OppdaterVarselInfoMapperTest {

	public OppdaterVarselInfoMapper mapper = new OppdaterVarselInfoMapper();


	@Test
	public void shouldMapEpostVarsel() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(createOppdaterDigitalAdresseRequest(Varsler.builder()
				.epostvarsel(createEpostVarsel()).build(), VEDTAK));

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());
		oppdaterVarselInfo.notifikasjoner().forEach(notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(EPOST_VARSELTITTEL_VEDTAK, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});

	}

	@Test
	public void shouldMapBothEpostAndSMSVarsel() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(createOppdaterDigitalAdresseRequest(Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.smsvarsel(createSmsVarsel())
				.build(), VEDTAK));

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		assertThat(oppdaterVarselInfo.notifikasjoner()).extracting("kontaktInfo", "tekst", "kanal", "tittel")
				.contains(tuple(EPOSTADRESSE, VEDTAK_TEKST, "EPOST", EPOST_VARSELTITTEL_VEDTAK),
						tuple(MOBILNUMMER, VEDTAK_TEKST, "MOBILTELEFON", null));

	}

	@ParameterizedTest
	@ValueSource(strings = {"VIKTIG", "VEDTAK"})
	public void shouldMapEpostVarselTittelWhenDistribusjonsTypeIsVedtakOrViktig(DistribusjonsTypeKode distribusjonsType) {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(createOppdaterDigitalAdresseRequest(Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), distribusjonsType));

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach(notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(EPOST_VARSELTITTEL_VEDTAK, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}

	@Test
	public void shouldMapEpostVarselTittelWhenDistribusjonsTypeIsAnnet() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(createOppdaterDigitalAdresseRequest(Varsler.builder()
				.epostvarsel(createEpostVarsel())
				.build(), ANNET));

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach(notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertEquals(EPOST_VARSELTITTEL_ANNET, notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}

	@Test
	public void epostVarselTittelMapsToNullWhenDistribusjonsTypeKodeIsNull() {
		OppdaterVarselInfoRequest oppdaterVarselInfo = mapper.mapVarselInfo(
				createOppdaterDigitalAdresseRequest(Varsler.builder()
						.epostvarsel(createEpostVarsel())
						.build(), null));

		assertEquals(FORSENDELSE_ID, oppdaterVarselInfo.forsendelseId());

		oppdaterVarselInfo.notifikasjoner().stream().forEach(notifikasjon -> {
			assertEquals(EPOSTADRESSE, notifikasjon.kontaktInfo());
			assertEquals(VEDTAK_TEKST, notifikasjon.tekst());
			assertEquals("EPOST", notifikasjon.kanal());
			assertNull(notifikasjon.tittel());
			assertNotNull(notifikasjon.varslingstidspunkt());
		});
	}


	private static OppdaterDigitalAdresseRequest createOppdaterDigitalAdresseRequest(Varsler varsler, DistribusjonsTypeKode distribusjonsType) {
		return OppdaterDigitalAdresseRequest.builder().forsendelseId(FORSENDELSE_ID)
				.varsler(varsler)
				.distribusjonsTypeKode(distribusjonsType)
				.build();
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