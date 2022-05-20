package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.dokdistdpi.consumer.rdist001.kodeverk.DistribusjonstidspunktKode;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HentForsendelseResponse {

	public static final String ARKIV_SYSTEM_JOARK = "JOARK";
	private String bestillingsId;
	private String konversasjonId;
	private String bestillendeFagsystem;
	private String modus;
	private String forsendelseStatus;
	private String tema;
	private String forsendelseTittel;
	private String batchId;
	private String dokumentProdApp;
	private MottakerTo mottaker;
	private ArkivInformasjonTo arkivInformasjon;
	private PostadresseTo postadresse;
	private List<DokumentTo> dokumenter;
	private DistribusjonstidspunktKode distribusjonstidspunkt;
	private DistribusjonsTypeKode distribusjonstype;

	public boolean isIkkeArkivertIJoark() {
		return getArkivInformasjon() == null || !ARKIV_SYSTEM_JOARK.equals(getArkivInformasjon().getArkivSystem());
	}

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MottakerTo {
		private String mottakerId;
		private String mottakerNavn;
		private String mottakerType;

	}

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ArkivInformasjonTo {
		private String arkivSystem;
		private String arkivId;
	}

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PostadresseTo {
		private String adresselinje1;
		private String adresselinje2;
		private String adresselinje3;
		private String postnummer;
		private String poststed;
		private String landkode;
	}

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DokumentTo {
		private String tilknyttetSom;
		private String dokumentObjektReferanse;
		private String arkivDokumentInfoId;
		private String dokumenttypeId;
	}
}
