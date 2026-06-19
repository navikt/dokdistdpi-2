package no.nav.dokdistdpi.map;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dokmet.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.HentForsendelseResponse;
import no.nav.dokdistdpi.utils.VarslingstekstUtil;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class VarselMapper {
	public static Varsler mapVarsler(VarselInfo varselInfo, SikkerDigitalKontaktInfo digitalKontaktInfo, DistribusjonsTypeKode distribusjonsType) {
		if (isNull(varselInfo)) {
			return null;
		}

		String varslingstekst = VarslingstekstUtil.determineVarslingstekst(distribusjonsType, digitalKontaktInfo.getLeverandoerAdresse());
		return Varsler.builder()
				.epostvarsel(mapEpostVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
				.smsvarsel(mapSMSVarsler(varselInfo, digitalKontaktInfo, varslingstekst))
				.build();
	}

	public static Varsler mapVarslerHvisRiktigDistribusjonstype(HentForsendelseResponse hentForsendelseResponse, VarselInfo varselInfo, SikkerDigitalKontaktInfo sikkerDigitalKontaktInfo) {
		if (skalgiAvsenderstyrtVarsel(hentForsendelseResponse.getDistribusjonstype())) {
			return mapVarsler(varselInfo, sikkerDigitalKontaktInfo, hentForsendelseResponse.getDistribusjonstype());
		} else {
			return null;
		}
	}

	private static boolean skalgiAvsenderstyrtVarsel(DistribusjonsTypeKode distribusjonsTypeKode) {
		if (isNull(distribusjonsTypeKode)) {
			return true;
		}
		return switch (distribusjonsTypeKode) {
			case VIKTIG, VEDTAK -> true;
			default -> false;
		};
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
