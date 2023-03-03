package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.Notifikasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class OppdaterVarselInfoMapper {

	public static final String VARSELTITTEL_VEDTAK = "Varsel om post";
	public static final String VARSELTITTEL_ANNET = "Du har en ny melding";
	private static final String EPOST = "EPOST";
	private static final String MOBILTELEFON = "MOBILTELEFON";

	public OppdaterVarselInfoRequest mapVarselInfo(String forsendelseId, Varsler varsler, String distribusjonsType) {
		if (isNull(varsler)) {
			return null;
		}

		return new OppdaterVarselInfoRequest(forsendelseId, mapNotifikasjon(varsler, distribusjonsType));
	}

	private Set<Notifikasjon> mapNotifikasjon(Varsler varsler, String distribusjonsType) {
		Set<Notifikasjon> notifikasjons = new HashSet<>();

		EpostVarsel epostvarsel = varsler.getEpostvarsel();
		SmsVarsel smsvarsel = varsler.getSmsvarsel();
		if (nonNull(epostvarsel)) {
			notifikasjons.add(new Notifikasjon(EPOST, getTittel(distribusjonsType),
					epostvarsel.getVarslingstekst(), epostvarsel.getEpostadresse(), LocalDateTime.now()));
		}

		if (nonNull(smsvarsel)) {
			notifikasjons.add(new Notifikasjon(MOBILTELEFON, null,
					smsvarsel.getVarslingstekst(), smsvarsel.getMobiltelefonnummer(), LocalDateTime.now()));
		}

		return notifikasjons;
	}

	private String getTittel(String distribusjonstype) {
		if (isBlank(distribusjonstype)) {
			return null;
		}

		DistribusjonsTypeKode distribusjonsTypeKode = valueOf(distribusjonstype);
		return switch (distribusjonsTypeKode) {
					case VEDTAK, VIKTIG -> VARSELTITTEL_VEDTAK;
					case ANNET -> VARSELTITTEL_ANNET;
				};
	}

}
