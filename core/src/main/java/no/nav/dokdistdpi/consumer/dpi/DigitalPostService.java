package no.nav.dokdistdpi.consumer.dpi;

import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.map.ForsendelseMapper;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;

@Component
public class DigitalPostService {

	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final AdministrerForsendelseConsumer administrerForsendelseConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final ForsendelseMapper digitalPostMapper;

	@Autowired
	public DigitalPostService(MaskinportenTokenConsumer maskinportenTokenConsumer,
							  AdministrerForsendelseConsumer administrerForsendelseConsumer,
							  DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.administrerForsendelseConsumer = administrerForsendelseConsumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.digitalPostMapper = new ForsendelseMapper();
	}

	//Sender digital post melding til hjørne-2
	public void send(Forsendelse forsendelse) {

	}

	public DigitalPost digitalPostInfo(String forsendelseId) {
		assertNotBlank("forsendelseId", forsendelseId);
		HentForsendelseResponse hentForsendelseResponse = administrerForsendelseConsumer.hentForsendelse(forsendelseId);
		String mottakerId = getMottakerId(hentForsendelseResponse);
		assertNotBlank("mottakerId", mottakerId);
		SikkerDigitalKontaktInfo digitalKontaktInfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(mottakerId);
		String maskinportenToken = maskinportenTokenConsumer.fetchToken().getAccessToken();

		return digitalPostMapper.map(digitalKontaktInfo, maskinportenToken, hentForsendelseResponse);
	}

	private String getMottakerId(HentForsendelseResponse hentMottakerResponse) {
		if (isNull(hentMottakerResponse) || isNull(hentMottakerResponse.getMottaker())) {
			throw new AdminstrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}
		HentForsendelseResponse.MottakerTo mottakerTo = hentMottakerResponse.getMottaker();
		return requireNonNull(mottakerTo.getMottakerId(), "MottakerId kan ikke være null");
	}
}
