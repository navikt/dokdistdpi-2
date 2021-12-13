package no.nav.dokdistdpi.consumer.dpi.digitalpost.map;

import no.nav.dokdistdpi.consumer.dkif.SikkerDigitalKontaktInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Identifikator;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;
import no.nav.dokdistdpi.consumer.rdist001.HentForsendelseResponse;

import java.util.Objects;
import java.util.Optional;

import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Identifikator.Authority.ISO_6523_ACTORID_UPIS;


public class ForsendelseMapper {

	public Forsendelse mapForsendelse(SikkerDigitalKontaktInfo digitalKontaktInfo, String maskinportenToken,
									  HentForsendelseResponse hentForsendelseResponse, Dokumentpakke dokumentpakke) {
		return Forsendelse.builder()
				.personidentifikator(mapMottakerId(hentForsendelseResponse))
				.bestillingsId(hentForsendelseResponse.getBestillingsId())
				.conversationId(hentForsendelseResponse.getKonversasjonId())
				.digital(map(digitalKontaktInfo, maskinportenToken, hentForsendelseResponse))
				.dokumentpakke(dokumentpakke)
				.build();
	}

	public DigitalPost map(SikkerDigitalKontaktInfo digitalKontaktInfo, String maskinportenToken,
						   HentForsendelseResponse hentForsendelseResponse) {
		return DigitalPost.builder()
				.avsender(DigitalPost.Avsender.builder()
						.virksomhetsidentifikator(Identifikator.builder()
								.authority(ISO_6523_ACTORID_UPIS)
								.value(asIso6523(NAV_ORGNUMMER))
								.build())
						.build())
				.mottaker(DigitalPost.Personmottaker.builder()
						.postkasseadresse(digitalKontaktInfo.getBrukerAdresse())
						.build())
				.maskinportentoken(maskinportenToken)
				.build();
	}

	private String mapMottakerId(HentForsendelseResponse hentForsendelseResponse) {
		HentForsendelseResponse.MottakerTo mottaker = hentForsendelseResponse.getMottaker();
		return Optional.ofNullable(mottaker)
				.map(HentForsendelseResponse.MottakerTo::getMottakerId)
				.orElseThrow(() -> new IllegalArgumentException("MottakerId kan ikke være null"));
	}
}
