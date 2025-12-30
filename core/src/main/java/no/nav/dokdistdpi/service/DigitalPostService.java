package no.nav.dokdistdpi.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DistribusjonInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;

@Component
@Slf4j
public class DigitalPostService {

	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final DokmetConsumer dokmetConsumer;
	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator;

	public DigitalPostService(MaskinportenTokenConsumer maskinportenTokenConsumer,
							  DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator,
							  DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer,
							  DokmetConsumer dokmetConsumer) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.digitalKontaktInformasjonValidator = digitalKontaktInformasjonValidator;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.dokmetConsumer = dokmetConsumer;
	}

	public SikkerDigitalKontaktInfo hentDigitalKontaktInfo(HentForsendelseResponse hentForsendelseResponse, VarselInfo varselInfo) {
		String mottakerId = getMottakerId(hentForsendelseResponse);
		assertNotBlank("mottakerId", mottakerId);
		SikkerDigitalKontaktInfo digitalKontaktInfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(mottakerId);
		digitalKontaktInformasjonValidator.validateKontaktinfo(digitalKontaktInfo, varselInfo);
		return digitalKontaktInfo;
	}

	public String getMaskinportenToken() {
		return Optional.of(maskinportenTokenConsumer.fetchToken())
				.map(OidcTokenResponse::getAccessToken)
				.orElseThrow(() -> new MaskinportenFunctionalException("MaskinportenToken kan ikke være null"));
	}

	public VarselInfo getVarselInfo(DistribusjonInfo distribusjonInfo) {
		return isNull(distribusjonInfo) ? null : dokmetConsumer.getVarselInfo(distribusjonInfo.getVarselTypeId());
	}

	public DistribusjonInfo hentDokumenttypeInfo(HentForsendelseResponse forsendelseResponse) {
		return forsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.map(dokument -> dokmetConsumer.hentDokumenttypeInfo(dokument.getDokumenttypeId())).findAny()
				.orElseThrow(() -> new DokmetFunctionalException("DokumenttypeInfo kan ikke være null"));
	}



	private String getMottakerId(HentForsendelseResponse hentMottakerResponse) {
		if (hentMottakerResponse == null) {
			throw new AdminstrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}

		Mottaker mottaker = hentMottakerResponse.getMottaker();
		return requireNonNull(mottaker.getMottakerId(), "MottakerId kan ikke være null");
	}
}
