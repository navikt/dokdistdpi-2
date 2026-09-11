package no.nav.dokdistdpi.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.config.prop.MaskinportenProperties;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInfoResponse;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DistribusjonInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.naistoken.NaisTexasConsumer;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.dokdistdpi.exception.functional.AdministrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.functional.NaisTexasTechnicalException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinfoMapper.mapDigitalKontaktinfo;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;

@Slf4j
@Service
public class DigitalPostService {

	private final NaisTexasConsumer naisTexasConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final DokmetConsumer dokmetConsumer;
	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator;
	private final MaskinportenProperties maskinportenProperties;

	public DigitalPostService(NaisTexasConsumer naisTexasConsumer,
							  DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator,
							  DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer,
							  DokmetConsumer dokmetConsumer,
							  MaskinportenProperties maskinportenProperties) {
		this.naisTexasConsumer = naisTexasConsumer;
		this.digitalKontaktInformasjonValidator = digitalKontaktInformasjonValidator;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.dokmetConsumer = dokmetConsumer;
		this.maskinportenProperties = maskinportenProperties;
	}

	public SikkerDigitalKontaktInfo hentDigitalKontaktInfo(HentForsendelseResponse hentForsendelseResponse) {
		String mottakerId = getMottakerId(hentForsendelseResponse);
		assertNotBlank("mottakerId", mottakerId);
		DigitalKontaktInfoResponse.DigitalKontaktinfo digitalKontaktInfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(mottakerId);
		SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo = mapDigitalKontaktinfo(digitalKontaktInfo);
		digitalKontaktInformasjonValidator.validateKontaktinfo(sikkerDigitalKontaktInfo);
		return sikkerDigitalKontaktInfo;
	}

	public String getMaskinportenToken() {
		return Optional.of(naisTexasConsumer.getMaskinportenToken(maskinportenProperties.scopes()))
				.orElseThrow(() -> new NaisTexasTechnicalException("Maskinporten token kan ikke være null"));
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
			throw new AdministrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}

		Mottaker mottaker = hentMottakerResponse.getMottaker();
		return requireNonNull(mottaker.getMottakerId(), "MottakerId kan ikke være null");
	}
}
