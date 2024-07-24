package no.nav.dokdistdpi.consumer.dokmet.tkat20;

import no.nav.dokdistdpi.consumer.dokmet.DokmetFunctionalException;
import no.nav.dokmet.api.tkat020.DistribusjonVarselTo;
import no.nav.dokmet.api.tkat020.DokumenttypeInfoTo;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.DISTRIBUSJONS_SDP_KANAL;

public class DokumenttypeInfoMapper {

	public static DokumenttypeInfo mapDokumenttypeInfoTo(DokumenttypeInfoTo response) {
		if (isNull(response.getDokumentProduksjonsInfo()) &&
				isNull(response.getDokumentProduksjonsInfo().getDistribusjonInfo())) {
			throw new DokmetFunctionalException(format("DokumentProduksjonsInfo eller DokumentProduksjonsInfo.DistribusjonInfo er null for dokumenttypeId=%s. Ikke et utgående dokument? dokumentType=%s",
					response.getDokumenttypeId(), response.getDokumentType()));
		}

		DistribusjonVarselTo distribusjonVarsel = response.getDokumentProduksjonsInfo()
				.getDistribusjonInfo().getDistribusjonVarsels().stream()
				.filter(distribusjonVarselTo -> DISTRIBUSJONS_SDP_KANAL.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))
				.findAny()
				.orElseThrow(() -> new DokmetFunctionalException(format("Fant ingen distribusjonVarsel med varselForDistribusjonKanal=%s for dokumenttypeId=%s",
						DISTRIBUSJONS_SDP_KANAL, response.getDokumenttypeId())));

		return DokumenttypeInfo.builder()
				.varselTypeId(distribusjonVarsel.getVarseltypeId())
				.sikkerhetsnivaa(response.getDokumentProduksjonsInfo().getDistribusjonInfo().getSikkerhetsnivaa())
				.build();
	}
}
