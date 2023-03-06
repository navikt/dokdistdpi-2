package no.nav.dokdistdpi.consumer.rdist001;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistdpi.consumer.dpi.client.OppdaterDigitalAdresseRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequestTo;
import no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_OVERSENDT;

@Component
public class DokdistAdministrerForsendelseUpdater {
	private static final String FORSENDELSE_TIL_DPI_COUNTER = "forsendelse_til_dpi_counter";
	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final MeterRegistry meterRegistry;
	private final OppdaterVarselInfoMapper oppdaterVarselInfoMapper;

	@Autowired
	public DokdistAdministrerForsendelseUpdater(AdministrerForsendelseConsumer administrerForsendelse,
												MeterRegistry meterRegistry) {
		this.administrerForsendelse = administrerForsendelse;
		this.meterRegistry = meterRegistry;
		this.oppdaterVarselInfoMapper = new OppdaterVarselInfoMapper();
	}

	public void updateStatusDigitalLeverandoerAndPostkasseadresse(OppdaterDigitalAdresseRequest oppdaterDigitalAdresseRequest) {
		administrerForsendelse.oppdaterForsendelseAndDigitalPostkasseInfo(mapDigitalAdresse(oppdaterDigitalAdresseRequest));
		meldingTilDpiCounter();
	}

	private OppdaterForsendelseRequestTo mapDigitalAdresse(OppdaterDigitalAdresseRequest oppdaterDigitalAdresseRequest) {
		return oppdaterDigitalAdresseRequest == null ? null : OppdaterForsendelseRequestTo.builder()
				.forsendelseId(oppdaterDigitalAdresseRequest.getForsendelseId())
				.forsendelseStatus(FORSENDELSE_STATUS_OVERSENDT)
				.digitalLeverandoeradresse(oppdaterDigitalAdresseRequest.getDigitalLeverandoeradresse())
				.digitalPostkasseadresse(oppdaterDigitalAdresseRequest.getDigitalPostkasseadresse())
				.build();
	}

	public void oppdaterVarselInfo(OppdaterDigitalAdresseRequest oppdaterDigitalAdresseRequest) {
		administrerForsendelse.oppdaterVarselInfo(oppdaterVarselInfoMapper.mapVarselInfo(oppdaterDigitalAdresseRequest));
	}

	private void meldingTilDpiCounter() {
		meterRegistry.counter(FORSENDELSE_TIL_DPI_COUNTER,
				"forsendelseStatus", FORSENDELSE_STATUS_OVERSENDT).increment();
	}
}
