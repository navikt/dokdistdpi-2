package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistdpi.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;

import java.util.List;

@Value
@Builder
public class HentForsendelseResponse {

	public static final String ARKIV_SYSTEM_JOARK = "JOARK";

	String bestillingsId;
	String konversasjonId;
	String bestillendeFagsystem;
	String modus;
	String forsendelseStatus;
	String tema;
	String forsendelseTittel;
	String batchId;
	String dokumentProdApp;
	Mottaker mottaker;
	ArkivInformasjon arkivInformasjon;
	Postadresse postadresse;
	List<Dokument> dokumenter;
	DistribusjonstidspunktKode distribusjonstidspunkt;
	DistribusjonsTypeKode distribusjonstype;

	public boolean isIkkeArkivertIJoark() {
		return getArkivInformasjon() == null || !ARKIV_SYSTEM_JOARK.equals(getArkivInformasjon().getArkivSystem());
	}

	@Value
	@Builder
	public static class Mottaker {
		String mottakerId;
		String mottakerNavn;
		String mottakerType;

	}

	@Value
	@Builder
	public static class ArkivInformasjon {
		String arkivSystem;
		String arkivId;
	}

	@Value
	@Builder
	public static class Postadresse {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}

	@Value
	@Builder
	public static class Dokument {
		String tilknyttetSom;
		String dokumentObjektReferanse;
		String arkivDokumentInfoId;
		String dokumenttypeId;
	}
}
