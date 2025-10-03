package no.nav.dokdistdpi.map;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.utils.VarslingstekstUtil;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class VarselMapper {
	public static Varsler mapVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, DistribusjonsTypeKode distribusjonsType) {
	if (Objects.isNull(varselInfo)) {
		return null;
	}

	String varslingstekst = VarslingstekstUtil.determineVarslingstekst(distribusjonsType, digitalKontaktInfo.getLeverandoerAdresse());
	return Varsler.builder()
			.epostvarsel(mapEpostVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
			.smsvarsel(mapSMSVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
			.build();
}

	private static SmsVarsel mapSMSVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, String varslingstekst) {
		if (isBlank(digitalKontaktInfo.getMobiltelefonnummer())) {
			return null;
		}

		return SmsVarsel.builder()
				.mobiltelefonnummer(digitalKontaktInfo.getMobiltelefonnummer())
				.varslingstekst(varslingstekst)
				.repetisjoner(varselInfo.getAntallDagerListe())
				.build();
	}

	private static EpostVarsel mapEpostVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, String varslingstekst) {
		if (isBlank(digitalKontaktInfo.getEpostadresse())) {
			return null;
		}

		return EpostVarsel.builder()
				.epostadresse(digitalKontaktInfo.getEpostadresse())
				.varslingstekst(varslingstekst)
				.repetisjoner(varselInfo.getAntallDagerListe())
				.build();
	}
}
