package no.nav.dokdistdpi.consumer.dkif;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;

@Slf4j
@Component
public class SikkerDigitlPostInfoService {

	private final AdministrerForsendelseConsumer administrerForsendelseConsumer;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;

	@Autowired
	public SikkerDigitlPostInfoService(AdministrerForsendelseConsumer administrerForsendelseConsumer, PdlGraphQLConsumer pdlGraphQLConsumer,
									   DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer) {
		this.administrerForsendelseConsumer = administrerForsendelseConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
	}

	public SikkerDigitalKontaktInfo hentSikkerDigitalPostLeverandoer(String forsendelseId) {
		assertNotBlank("forsendelseId", forsendelseId);
		String mottakerId = administrerForsendelseConsumer.hentMottaker(forsendelseId);
		assertNotBlank("mottakerId", mottakerId);
		String ident = pdlGraphQLConsumer.hentIdent(mottakerId);
		assertNotBlank("ident", ident);
		return digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(ident);
	}

}
