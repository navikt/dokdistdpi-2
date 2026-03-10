package no.nav.dokdistdpi.consumer.rdist001;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dpi.client.OppdaterForsendelseAndVarselRequest;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterForsendelseRequest;
import no.nav.dokdistdpi.consumer.rdist001.map.OppdaterVarselInfoMapper;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_OVERSENDT;

@Slf4j
@Component
public class DokdistAdministrerForsendelseUpdater {

	private static final String FORSENDELSE_TIL_DPI_COUNTER = "forsendelse_til_dpi_counter";
	private final MeterRegistry meterRegistry;
	private final OppdaterVarselInfoMapper oppdaterVarselInfoMapper;
	private final DokdistadminConsumer dokdistadminConsumer;

	public DokdistAdministrerForsendelseUpdater(MeterRegistry meterRegistry,
												DokdistadminConsumer dokdistadminConsumer) {
		this.meterRegistry = meterRegistry;
		this.oppdaterVarselInfoMapper = new OppdaterVarselInfoMapper();
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	@SuppressWarnings("unused")
	@Handler
	public void updateStatusDigitalpostkasseInfoAndVarselInfo(OppdaterForsendelseAndVarselRequest oppdaterDigitalAdresseRequest) {
		updateStatusDigitalLeverandoerAndPostkasseadresse(oppdaterDigitalAdresseRequest);
		log.info("qdist011 har oppdatert digital postkasse info og forsendelseStatus=OVERSENDT med forsendelseId={}", oppdaterDigitalAdresseRequest.getForsendelseId());

		oppdaterVarselInfo(oppdaterDigitalAdresseRequest);
		log.info("qdist011 har oppdatert varselInfo i dokdistDb med forsendelseId={}", oppdaterDigitalAdresseRequest.getForsendelseId());
	}

	private void updateStatusDigitalLeverandoerAndPostkasseadresse(OppdaterForsendelseAndVarselRequest oppdaterDigitalAdresseRequest) {
		dokdistadminConsumer.oppdaterForsendelse(mapDigitalAdresse(oppdaterDigitalAdresseRequest));
		meldingTilDpiCounter();
	}

	private OppdaterForsendelseRequest mapDigitalAdresse(OppdaterForsendelseAndVarselRequest oppdaterDigitalAdresseRequest) {
		return oppdaterDigitalAdresseRequest == null ? null : OppdaterForsendelseRequest.builder()
				.forsendelseId(oppdaterDigitalAdresseRequest.getForsendelseId())
				.forsendelseStatus(FORSENDELSE_STATUS_OVERSENDT)
				.digitalLeverandoeradresse(oppdaterDigitalAdresseRequest.getDigitalLeverandoeradresse())
				.digitalPostkasseadresse(oppdaterDigitalAdresseRequest.getDigitalPostkasseadresse())
				.build();
	}

	private void oppdaterVarselInfo(OppdaterForsendelseAndVarselRequest oppdaterDigitalAdresseRequest) {
		if (nonNull(oppdaterDigitalAdresseRequest.getVarsler())) {
			dokdistadminConsumer.oppdaterVarselInfo(oppdaterVarselInfoMapper.mapVarselInfo(oppdaterDigitalAdresseRequest));
		}
	}

	private void meldingTilDpiCounter() {
		meterRegistry.counter(FORSENDELSE_TIL_DPI_COUNTER,
				"forsendelseStatus", FORSENDELSE_STATUS_OVERSENDT).increment();
	}
}
