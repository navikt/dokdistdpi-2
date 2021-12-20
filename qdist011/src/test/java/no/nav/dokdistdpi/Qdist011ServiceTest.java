package no.nav.dokdistdpi;

import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.Dokumentkatalog;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.DokumentkatalogConsumer;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.digitalpost.domain.Forsendelse;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.s3storage.Storage;
import no.nav.dokdistdpi.saf.JournalpostQdist011;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.dokdistdpi.TestUtil.MASKINPORTEN_TOKEN;
import static no.nav.dokdistdpi.TestUtil.buildHentForsendelseResponseWithDokumentAndWithoutArkivInformasjon;
import static no.nav.dokdistdpi.TestUtil.createDistribuerTilKanal;
import static no.nav.dokdistdpi.TestUtil.createDokumenttypeInfoTo;
import static no.nav.dokdistdpi.TestUtil.createOidcTokenResponse;
import static no.nav.dokdistdpi.TestUtil.createSikkerDigitalKontaktInfo;
import static no.nav.dokdistdpi.TestUtil.createVarselInfoTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Qdist011ServiceTest {

	//@MockBean
	private Storage s3Storage;
	private AdministrerForsendelseConsumer administrerForsendelse;
	private SafJournalpostQueryService<JournalpostQdist011> safJournalpostQueryService;
	private DigitalPostService digitalPostService;
	private Qdist011Service qdist011Service;

	private MaskinportenTokenConsumer maskinportenTokenConsumer;
	private DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private VarselInfo varselInfo;
	private DigitalKontaktInformasjonValidator digitalKontaktInformasjonValidator;
	private Dokumentkatalog dokumentkatalog;
	private Exchange exchange;

	@BeforeEach
	void setup() {
		administrerForsendelse = mock(AdministrerForsendelseConsumer.class);
		safJournalpostQueryService = mock(SafJournalpostQueryService.class);
		maskinportenTokenConsumer = mock(MaskinportenTokenConsumer.class);
		digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
		varselInfo = mock(VarselInfo.class);
		dokumentkatalog = mock(DokumentkatalogConsumer.class);
		exchange = mock(Exchange.class);
		s3Storage = mock(Storage.class);

		digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();
		digitalPostService = new DigitalPostService(maskinportenTokenConsumer, digitalKontaktInformasjonValidator,
				digitalKontaktinformasjonConsumer, varselInfo, dokumentkatalog);

		qdist011Service = new Qdist011Service(s3Storage, administrerForsendelse, digitalPostService, safJournalpostQueryService);

		when(s3Storage.get(anyString()))
				.thenReturn("""
						{
							"pdf":"SE9WRURET0tfVEVTVF9DT05URU5U",
							"dokumentObjektReferanse":"ref-1",
							"dokumentInfoId":"123"
						}
						""");
//				.thenReturn("{'pdf':'SE9WRURET0tfVEVTVF9DT05URU5U','dokumentObjektReferanse':'ref-1','dokumentInfoId':'123'}");


	}

	@Test
	void ShouldTestSomething() {
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokumentAndWithoutArkivInformasjon());
		when(safJournalpostQueryService.hentJournalpost(anyString())).thenReturn(TestUtil.createJournalpostQdist011());
		when(maskinportenTokenConsumer.fetchToken()).thenReturn(createOidcTokenResponse());
		when(digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(anyString())).thenReturn(createSikkerDigitalKontaktInfo());
		when(varselInfo.getVarselInfo(anyString())).thenReturn(createVarselInfoTo());
		when(dokumentkatalog.getDokumenttypeInfo(anyString())).thenReturn(createDokumenttypeInfoTo());

		Forsendelse forsendelse = qdist011Service.createForsendelse(createDistribuerTilKanal(), exchange);
		Assertions.assertEquals(MASKINPORTEN_TOKEN, forsendelse.getDigital().getMaskinportentoken());
	}

}