package no.nav.dokdistdpi.consumer.dpi.serviceregistry;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitlPostInfoService;
import no.nav.dokdistdpi.exception.functional.DigitaPostProviderInfoIkkeFunnetException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.DPI_PROCESS;
import static no.nav.dokdistdpi.consumer.dpi.serviceregistry.ServiceIdentifier.DPI;

@Component
public class DigitalPostProviderInfoService {

	private final ServiceRegistryConsumer serviceRegistryConsumer;
	private final SikkerDigitlPostInfoService sikkerDigitlPostInfoService;

	@Autowired
	public DigitalPostProviderInfoService(ServiceRegistryConsumer serviceRegistryConsumer,
										  SikkerDigitlPostInfoService sikkerDigitlPostInfoService) {
		this.serviceRegistryConsumer = serviceRegistryConsumer;
		this.sikkerDigitlPostInfoService = sikkerDigitlPostInfoService;
	}

	public DigitaPostProviderInfo hentDigitaPostProviderInfo(final String forsendelseId) {
		SikkerDigitalKontaktInfo digitalKontaktInfo = sikkerDigitlPostInfoService.hentSikkerDigitalPostLeverandoer(forsendelseId);
		final IdentifierResource identifierResource = serviceRegistryConsumer.getIdentifierResource(digitalKontaktInfo.getLeverandoerAdresse(), DPI_PROCESS);
		final ServiceRecord serviceRecord = identifierResource.findServiceRecord(DPI_PROCESS, DPI)
				.orElseThrow(() -> new DigitaPostProviderInfoIkkeFunnetException("Fant ikke mottakerinfo for organisasjon=" + digitalKontaktInfo.getLeverandoerAdresse() + " og prosess=" + DPI_PROCESS));
		final Service service = serviceRecord.getService();

		return new DigitaPostProviderInfo(serviceRecord.getOrganisationNumber(),
				serviceRecord.getPemCertificate(),
				service.getServiceCode(),
				service.getServiceEditionCode());
	}
}
