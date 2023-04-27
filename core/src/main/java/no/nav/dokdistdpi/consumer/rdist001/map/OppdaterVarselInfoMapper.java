package no.nav.dokdistdpi.consumer.rdist001.map;

import no.nav.dokdistdpi.consumer.dpi.client.OppdaterForsendelseAndVarselRequest;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.rdist001.domain.DistribusjonsTypeKode;
import no.nav.dokdistdpi.consumer.rdist001.domain.Notifikasjon;
import no.nav.dokdistdpi.consumer.rdist001.domain.OppdaterVarselInfoRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistdpi.consumer.rdist001.kodeverk.VarslingKanalCode.EPOST;
import static no.nav.dokdistdpi.consumer.rdist001.kodeverk.VarslingKanalCode.MOBILTELEFON;

public class OppdaterVarselInfoMapper {

	public static final String EPOST_VARSELTITTEL_VEDTAK = "Varsel om post";
	public static final String EPOST_VARSELTITTEL_ANNET = "Du har en ny melding";

	public OppdaterVarselInfoRequest mapVarselInfo(OppdaterForsendelseAndVarselRequest digitalAdresseRequest) {
		if (isNull(digitalAdresseRequest)) {
			return null;
		}

		return new OppdaterVarselInfoRequest(digitalAdresseRequest.getForsendelseId(), mapNotifikasjon(digitalAdresseRequest.getVarsler(), digitalAdresseRequest.getDistribusjonsTypeKode()));
	}

	private List<Notifikasjon> mapNotifikasjon(Varsler varsler, DistribusjonsTypeKode distribusjonsType) {
		List<Notifikasjon> notifikasjons = new ArrayList<>();

		if (Objects.isNull(varsler)) {
			return null;
		}

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

	private String getTittel(DistribusjonsTypeKode distribusjonstype) {
		if (isNull(distribusjonstype)) {
			return null;
		}

		return switch (distribusjonstype) {
			case VEDTAK, VIKTIG -> EPOST_VARSELTITTEL_VEDTAK;
			case ANNET -> EPOST_VARSELTITTEL_ANNET;
		};
	}

}
