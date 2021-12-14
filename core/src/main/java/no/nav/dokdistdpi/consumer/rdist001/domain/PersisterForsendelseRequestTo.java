package no.nav.dokdistdpi.consumer.rdist001.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PersisterForsendelseRequestTo {
	private String bestillingsId;
	private String distribusjonsKanal;
	private String bestillendeFagsystem;
	private String tema;
	private String forsendelseTittel;
	private String batchId;
	private String dokumentProdApp;
	private String originalDistribusjonId;
	private MottakerTo mottaker;
	private ArkivInformasjonTo arkivInformasjon;
	private PostadresseTo postadresse;
	private List<DokumentTo> dokumenter;

	@Data
	@Builder
	public static class MottakerTo {
		private String mottakerId;
		private String mottakerNavn;
		private String mottakerType;
	}

	@Data
	@Builder
	public static class ArkivInformasjonTo {
		private String arkivSystem;
		private String arkivId;
	}

	@Data
	@Builder
	public static class PostadresseTo {
		private String adresselinje1;
		private String adresselinje2;
		private String adresselinje3;
		private String postnummer;
		private String poststed;
		private String landkode;
	}

	@Data
	@Builder
	public static class DokumentTo {
		private String tilknyttetSom;
		private String dokumentObjektReferanse;
		private Integer rekkefolge;
		private String arkivDokumentInfoId;
		private String dokumenttypeId;
	}
}
