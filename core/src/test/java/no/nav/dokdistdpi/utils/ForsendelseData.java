package no.nav.dokdistdpi.utils;

import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.CommonVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.DigitalPost;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.EpostVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Identifikator;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.SmsVarsel;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Varsler;
import no.nav.dokdistdpi.consumer.dpi.dokumentpakke.Dokumentpakke;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.nav.dokdistdpi.consumer.dpi.DigitalPostConstants.NAV_ORGNUMMER;
import static no.nav.dokdistdpi.consumer.dpi.Organisasjonsnummer.asIso6523;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Authority.ISO_6523_ACTORID_UPIS;
import static no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Sikkerhetsnivaa.NIVAA_3;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.EPOST;
import static no.nav.dokdistdpi.utils.DokdistdpiConstant.SMS;
import static no.nav.dokdistdpi.utils.TestUtils.classpathToString;


public class ForsendelseData {

	public static final String BESTILLINGS_ID = UUID.randomUUID().toString();
	private static final String MOTTAKER_FNR = "04036125433";
	private static final String POSTKASSEADRESSE = "ove.jonsen#6K5A";
	private static final String EPOSTADRESSE = "example@email.org";
	private static final String MOBILTELEFONNUMMER = "4799999999";
	private static final String VARSLINGSTEKST = "Du har mottatt brev i din digitale postkasse";
	public static final String VIRKSOMHETMOTTAKER = "984661185";
	public static final String MOTTAKER_ORGNO = "988015814";
	public static final String CONVERSATION_ID = UUID.randomUUID().toString();
	private static final String TITTEL = "Ikke-sensitiv tittel for forsendelsen";


	public static Forsendelse forsendelse(Dokumentpakke dokumentpakke) {
		return Forsendelse.builder()
				.konversasjonId(CONVERSATION_ID)
				.bestillingsId(BESTILLINGS_ID)
				.personidentifikator(MOTTAKER_FNR)
				.mottakerSertifikat(classpathToString("secrets/mottakercertificate"))
				.digitalPostLeverandoerAdresse(MOTTAKER_ORGNO)
				.digital(digitalPost())
				.dokumentpakke(dokumentpakke)
				.build();
	}


	public static DigitalPost digitalPost() {


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

		DigitalPost.Personmottaker personmottaker = DigitalPost.Personmottaker.builder()
				.postkasseadresse(POSTKASSEADRESSE)
				.build();


		return DigitalPost.builder()
				.avsender(DigitalPost.Avsender.builder()
						.virksomhetsidentifikator(Identifikator.builder()
								.authority(ISO_6523_ACTORID_UPIS)
								.value(asIso6523(NAV_ORGNUMMER))
								.build())
						.build())
				.mottaker(personmottaker)
				.virkningsdato(LocalDate.now())
				.aapningskvittering(false)
				.ikkesensitivtittel(TITTEL)
				.sikkerhetsnivaa(NIVAA_3.getValue())
				.varsler(Varsler.builder()
						.smsvarsel(smsVarsel)
						.epostvarsel(epostVarsel)
						.build())
				.build();
	}

	public static Set<String> makePreferertKanalSet(String... preferertKanal) {
		return Arrays.stream(preferertKanal).collect(Collectors.toSet());
	}

	public static java.util.Map<String, String> varslingsTekster(String epostVarslingsTekst, String smsVarslingsTekst) {
		Map<String, String> varslingsMap = new HashMap<>();
		varslingsMap.put(EPOST, epostVarslingsTekst);
		varslingsMap.put(SMS, smsVarslingsTekst);
		return varslingsMap;
	}

}
