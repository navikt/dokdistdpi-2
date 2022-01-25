package no.nav.dokdistdpi.qdist011;

import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.Dokumentkatalog;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.DokumenttypeInfoTo;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfoTo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import no.nav.dokdistdpi.exception.functional.Tkat020FunctionalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.EPOST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.SMS;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;

/**
 * @author Tsigab A. Gebremedhin, NAV
 */

@Component
public class DigitalPostService {

	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final VarselInfo varselInfo;
	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator;
	private final Dokumentkatalog dokumentkatalog;

	@Autowired
	public DigitalPostService(MaskinportenTokenConsumer maskinportenTokenConsumer, DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator,
							  DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer, VarselInfo varselInfo,
							  Dokumentkatalog dokumentkatalog) {
		this.maskinportenTokenConsumer = maskinportenTokenConsumer;
		this.digitalKontaktInformasjonValidator = digitalKontaktInformasjonValidator;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.varselInfo = varselInfo;
		this.dokumentkatalog = dokumentkatalog;
	}

	public SikkerDigitalKontaktInfo hentDigitalKontaktInfo(HentForsendelseResponse hentForsendelseResponse, VarselInfoTo varselInfoTo) {
		String mottakerId = getMottakerId(hentForsendelseResponse);
		assertNotBlank("mottakerId", mottakerId);
		SikkerDigitalKontaktInfo digitalKontaktInfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(mottakerId);
		digitalKontaktInformasjonValidator.validateKontaktinfo(digitalKontaktInfo, varselInfoTo);
		return digitalKontaktInfo;
	}

	public String getMaskinportenToken() {
		return Optional.of(maskinportenTokenConsumer.fetchToken())
				.map(OidcTokenResponse::getAccessToken)
				.orElseThrow(() -> new MaskinportenFunctionalException("MaskinportenToken kan ikke være null"));
	}

	public VarselInfoTo getVarselInfo(DokumenttypeInfoTo dokumenttypeInfo) {
		return isNull(dokumenttypeInfo) ? null : varselInfo.getVarselInfo(dokumenttypeInfo.getVarselTypeId());
	}

	public DokumenttypeInfoTo getDokumenttypeInfo(HentForsendelseResponse forsendelseResponse) {
		return forsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.map(dokument -> dokumentkatalog.getDokumenttypeInfo(dokument.getDokumenttypeId())).findAny()
				.orElseThrow(() -> new Tkat020FunctionalException("DokumenttypeInfo kan ikke være null"));
	}

	public Varsler mapVarsler(VarselInfoTo varselInfoTo, SikkerDigitalKontaktInfo digitalKontaktInfo) {
		return Varsler.builder()
				.epostvarsel(mapEpostVarsler(varselInfoTo, digitalKontaktInfo))
				.smsvarsel(mapSMSVarsler(varselInfoTo, digitalKontaktInfo))
				.build();
	}

	private SmsVarsel mapSMSVarsler(VarselInfoTo varselInfoTo, SikkerDigitalKontaktInfo digitalKontaktInfo) {
		return SmsVarsel.builder()
				.mobiltelefonnummer(digitalKontaktInfo.getMobiltelefonnummer())
				.varslingstekst(varselInfoTo.getVarslingsTekst()
						.get(SMS))
				.repetisjoner(varselInfoTo.getAntallDagerListe())
				.build();
	}

	private EpostVarsel mapEpostVarsler(VarselInfoTo varselInfoTo, SikkerDigitalKontaktInfo digitalKontaktInfo) {
		return EpostVarsel.builder()
				.epostadresse(digitalKontaktInfo.getEpostadresse())
				.varslingstekst(varselInfoTo.getVarslingsTekst()
						.get(EPOST))
				.repetisjoner(varselInfoTo.getAntallDagerListe())
				.build();
	}

	private String getMottakerId(HentForsendelseResponse hentMottakerResponse) {
		if (isNull(hentMottakerResponse) && isNull(hentMottakerResponse.getMottaker())) {
			throw new AdminstrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}
		HentForsendelseResponse.MottakerTo mottakerTo = hentMottakerResponse.getMottaker();
		return requireNonNull(mottakerTo.getMottakerId(), "MottakerId kan ikke være null");
	}
}
