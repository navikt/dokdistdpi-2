package no.nav.dokdistdpi.consumer.rdist001;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiConstant.FORSENDELSE_STATUS_OVERSENDT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_CONVERSATION_ID;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.PROPERTY_FORSENDELSE_ID;

@Component
public class DokdistAdministrerForsendelseUpdater {
	private static final String FORSENDELSE_TIL_DPI_COUNTER = "forsendelse_til_dpi_counter";
	private final AdministrerForsendelseConsumer administrerForsendelse;
	private final MeterRegistry meterRegistry;

	@Autowired
	public DokdistAdministrerForsendelseUpdater(AdministrerForsendelseConsumer administrerForsendelse,
												MeterRegistry meterRegistry) {
		this.administrerForsendelse = administrerForsendelse;
		this.meterRegistry = meterRegistry;
	}

	public void updateStatus(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);
		administrerForsendelse.oppdaterForsendelseStatus(forsendelseId, FORSENDELSE_STATUS_OVERSENDT);
	}

	public void updateStatusAndConversationId(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);
		final String conversationId = exchange.getProperty(PROPERTY_CONVERSATION_ID, String.class);
		administrerForsendelse.oppdaterForsendelseStatusOgKonversasjonsId(forsendelseId, FORSENDELSE_STATUS_OVERSENDT, conversationId);
		meldingTilDpiCounter();
	}

	private void meldingTilDpiCounter() {
		meterRegistry.counter(FORSENDELSE_TIL_DPI_COUNTER,
				FORSENDELSE_STATUS_OVERSENDT).increment();
	}
}
