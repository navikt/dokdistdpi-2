package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Avsender;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.CommonVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPostInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Virksomhetmottaker;
import no.nav.dokdistdpi.consumer.dpi.dokummentpakke.Dokumentpakke;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPostInfo.Sikkerhetsnivaa.NIVAA_3;

public class ForsendelseData {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String MOTTAKER_FNR = "04036125433";
	private static final String  POSTKASSEADRESSE="ove.jonsen#6K5A";
	private static final String EPOSTADRESSE = "example@email.org";
	private static final String MOBILTELEFONNUMMER ="4799999999";
	private static final String VARSLINGSTEKST = "Du har mottatt brev i din digitale postkasse";
	private static final String  VIRKSOMHETMOTTAKER = "984661185";
	private static final String TITTLE = "Ikke-sensitiv tittel for forsendelsen";


	public static Forsendelse forsendelse(Dokumentpakke dokumentpakke) {
		return Forsendelse.builder()
				.digital(digitalPost())
				.dokumentpakke(dokumentpakke)
				.build();
	}


	public static DigitalPostInfo digitalPost() {


		EpostVarsel epostVarsel = EpostVarsel.builder()
				.epostadresse(EPOSTADRESSE)
				.varslingstekst(VARSLINGSTEKST)
				.repetisjoner(CommonVarsel.Repetisjoner.builder()
						.dagerEtters(asList(0, 7))
						.build())
				.build();

		SmsVarsel smsVarsel = SmsVarsel.builder()
				.mobiltelefonnummer(MOBILTELEFONNUMMER)
				.varslingstekst(VARSLINGSTEKST)
				.repetisjoner(CommonVarsel.Repetisjoner.builder()
						.dagerEtters(asList(0, 7))
						.build())
				.build();

		DigitalPostInfo.Personmottaker personmottaker = DigitalPostInfo.Personmottaker.builder()
						.postkasseadresse(POSTKASSEADRESSE)
						.build();


		return DigitalPostInfo.builder()
				.avsender(Avsender.builder()
						.avsenderindentifikator(NAV_ORGNUMMER)
						.virksomhetsidentifikator(asIso6523(NAV_ORGNUMMER))
						.build())
				.virksomhetmottaker(Virksomhetmottaker
						.builder().virksomhetsidentifikator(asIso6523(VIRKSOMHETMOTTAKER))
						.motakeridentifikator(POSTKASSEADRESSE)
						.build())
				.personmottaker(personmottaker)
				.virkningsdato(LocalDate.now())
				.aapningskvittering(false)
				.sikkerhetsnivaa(NIVAA_3)
				.varsler(Varsler.builder()
						.smsvarsel(smsVarsel)
						.epostvarsel(epostVarsel)
						.build())
				.build();
	}

}
