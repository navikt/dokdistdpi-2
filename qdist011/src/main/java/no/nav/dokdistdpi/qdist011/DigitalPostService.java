package no.nav.dokdistdpi.qdist011;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistdpi.consumer.dokmet.tkat20.DokumenttypeInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.OidcTokenResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse.Mottaker;
import no.nav.dokdistdpi.exception.functional.AdminstrerForsendelseFunctionalException;
import no.nav.dokdistdpi.exception.functional.MaskinportenFunctionalException;
import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistdpi.qdist011.Utils.VarslingstekstUtil.determineVarslingstekst;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.HOVEDDOKUMENT;
import static no.nav.dokdistdpi.utils.DokdistdpiUtils.assertNotBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@Slf4j
public class DigitalPostService {

	private final MaskinportenTokenConsumer maskinportenTokenConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final DokmetConsumer dokmetConsumer;
	private final DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator;

	public DigitalPostService(MaskinportenTokenConsumer maskinportenTokenConsumer, DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator,
							  DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer, DokmetConsumer dokmetConsumer) {
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

	public VarselInfo getVarselInfo(DokumenttypeInfo dokumenttypeInfo) {
		return isNull(dokumenttypeInfo) ? null : dokmetConsumer.getVarselInfo(dokumenttypeInfo.getVarselTypeId());
	}

	public DokumenttypeInfo hentDokumenttypeInfo(HentForsendelseResponse forsendelseResponse) {
		return forsendelseResponse.getDokumenter().stream()
				.filter(dokument -> HOVEDDOKUMENT.equals(dokument.getTilknyttetSom()))
				.map(dokument -> dokmetConsumer.hentDokumenttypeInfo(dokument.getDokumenttypeId())).findAny()
				.orElseThrow(() -> new DokmetFunctionalException("DokumenttypeInfo kan ikke være null"));
	}

	public Varsler mapVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, DistribusjonsTypeKode distribusjonsType) {
		if (Objects.isNull(varselInfo)) {
			return null;
		}

		String varslingstekst = determineVarslingstekst(distribusjonsType, digitalKontaktInfo.getLeverandoerAdresse());
		return Varsler.builder()
				.epostvarsel(mapEpostVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
				.smsvarsel(mapSMSVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
				.build();
	}

	private SmsVarsel mapSMSVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, String varslingstekst) {
		if (isBlank(digitalKontaktInfo.getMobiltelefonnummer())) {
			return null;
		}

		return SmsVarsel.builder()
				.mobiltelefonnummer(digitalKontaktInfo.getMobiltelefonnummer())
				.varslingstekst(varslingstekst)
				.repetisjoner(varselInfo.getAntallDagerListe())
				.build();
	}

	private EpostVarsel mapEpostVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, String varslingstekst) {
		if (isBlank(digitalKontaktInfo.getEpostadresse())) {
			return null;
		}

		return EpostVarsel.builder()
				.epostadresse(digitalKontaktInfo.getEpostadresse())
				.varslingstekst(varslingstekst)
				.repetisjoner(varselInfo.getAntallDagerListe())
				.build();
	}

	private String getMottakerId(HentForsendelseResponse hentMottakerResponse) {
		if (hentMottakerResponse == null) {
			throw new AdminstrerForsendelseFunctionalException("Mottaker kan ikke være null");
		}

		Mottaker mottaker = hentMottakerResponse.getMottaker();
		return requireNonNull(mottaker.getMottakerId(), "MottakerId kan ikke være null");
	}
}
