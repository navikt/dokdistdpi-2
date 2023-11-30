package no.nav.dokdistdpi.qdist014.map;

import no.nav.dokdistdpi.consumer.dpi.JacksonConfig;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiFeilKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.DpiMelding;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.Feiltype;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.LeveringsKvittering;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.VarslingFeiletKvittering;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.ZonedDateTime;

import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.FEILET;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.LEVERING;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.kvittering.KvitteringType.VARSLINGFEILET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DpiKvitteringMapperTest {

	private final DpiKvitteringMapper dpiKvitteringMapper = new DpiKvitteringMapper(new JacksonConfig().dpiObjectMapper());

	@Test
	void shouldMapLeveringskvittering() {
		String kvitteringForretningsmelding = Testutil.classpathToString("__files/kvitteringer/leveringskvittering.json");

		DpiKvittering dpiKvittering = dpiKvitteringMapper.mapKvittering(LEVERING, kvitteringForretningsmelding);

		assertThat(dpiKvittering.getFeil()).isNull();
		assertThat(dpiKvittering.getVarslingfeiletkvittering()).isNull();
		LeveringsKvittering leveringskvittering = dpiKvittering.getLeveringskvittering();
		assertCommonDpiMelding(leveringskvittering);
	}

	@Test
	void shouldMapFeilKvittering() {
		String kvitteringForretningsmelding = Testutil.classpathToString("__files/kvitteringer/feilkvittering.json");

		DpiKvittering dpiKvittering = dpiKvitteringMapper.mapKvittering(FEILET, kvitteringForretningsmelding);

		assertThat(dpiKvittering.getLeveringskvittering()).isNull();
		assertThat(dpiKvittering.getVarslingfeiletkvittering()).isNull();
		DpiFeilKvittering feilKvittering = dpiKvittering.getFeil();
		assertCommonDpiMelding(feilKvittering);
		assertThat(feilKvittering.getFeiltype()).isEqualTo(Feiltype.KLIENT);
		assertThat(feilKvittering.getDetaljer()).isEqualTo("Reason code: SBD:4069. Bad format in document package - System.FormatException: Unable to read ASIC signature at eboks.");
	}

	@Test
	void shouldMapVarslingfeiletKvittering() {
		String kvitteringForretningsmelding = Testutil.classpathToString("__files/kvitteringer/varslingfeiletkvittering.json");

		DpiKvittering dpiKvittering = dpiKvitteringMapper.mapKvittering(VARSLINGFEILET, kvitteringForretningsmelding);

		assertThat(dpiKvittering.getFeil()).isNull();
		assertThat(dpiKvittering.getLeveringskvittering()).isNull();
		VarslingFeiletKvittering varslingFeiletKvittering = dpiKvittering.getVarslingfeiletkvittering();
		assertThat(varslingFeiletKvittering.getVarslingskanal()).isEqualTo("sms");
		assertThat(varslingFeiletKvittering.getBeskrivelse()).isEqualTo("Sms til 88888888 feilet. Feilkode: 7201: Delivery failed at Operator");
	}

	@ParameterizedTest
	@EnumSource(value = KvitteringType.class, mode = EnumSource.Mode.EXCLUDE, names = {"LEVERING", "VARSLINGFEILET", "FEILET"})
	void shouldThrowUnsupportedOperationExceptionWhenUnsupportedKvitteringType(KvitteringType kvitteringType) {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> dpiKvitteringMapper.mapKvittering(kvitteringType, "noop"));
	}

	private static void assertCommonDpiMelding(DpiMelding dpiMelding) {
		assertThat(dpiMelding.getTidspunkt()).isEqualTo(ZonedDateTime.parse("2021-04-11T15:29:58.753+02:00"));
		assertThat(dpiMelding.getAvsender().getVirksomhetsidentifikator().getAuthority()).isEqualTo("iso6523-actorid-upis");
		assertThat(dpiMelding.getAvsender().getVirksomhetsidentifikator().getValue()).isEqualTo("0192:999888999");
		assertThat(dpiMelding.getVirksomhetmottaker().getVirksomhetsidentifikator().getAuthority()).isEqualTo("iso3166-1-alfa2");
		assertThat(dpiMelding.getVirksomhetmottaker().getVirksomhetsidentifikator().getValue()).isEqualTo("0192:889640782");
		assertThat(dpiMelding.getVirksomhetmottaker().getMotakeridentifikator()).isEqualTo("889640782");
	}
}