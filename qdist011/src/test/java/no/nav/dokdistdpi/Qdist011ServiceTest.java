package no.nav.dokdistdpi;

import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktInformasjonValidator;
import no.nav.dokdistdpi.consumer.dkif.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistdpi.consumer.dokkat.tkat20.Dokumentkatalog;
import no.nav.dokdistdpi.consumer.dokkat.tkat21.VarselInfo;
import no.nav.dokdistdpi.consumer.dpi.maskineporten.MaskinportenTokenConsumer;
import no.nav.dokdistdpi.consumer.rdist001.AdministrerForsendelseConsumer;
import no.nav.dokdistdpi.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistdpi.s3storage.AmazonS3Storage;
import no.nav.dokdistdpi.s3storage.Storage;
import no.nav.dokdistdpi.saf.JournalpostQdist011;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.dokdistdpi.TestUtil.buildHentForsendelseResponseWithDokumentAndArkivSystemAsJoark;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Qdist011ServiceTest {

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

	@BeforeEach
	void setup() {
		s3Storage = mock(AmazonS3Storage.class);
		administrerForsendelse = mock(AdministrerForsendelseConsumer.class);
		safJournalpostQueryService = mock(SafJournalpostQueryService.class);
		maskinportenTokenConsumer = mock(MaskinportenTokenConsumer.class);
		digitalKontaktinformasjonConsumer = mock(DigitalKontaktinformasjonConsumer.class);
		varselInfo = mock(VarselInfo.class);
		dokumentkatalog = mock(Dokumentkatalog.class);

		digitalKontaktInformasjonValidator = new DigitalKontaktInformasjonValidator();
		digitalPostService = new DigitalPostService(maskinportenTokenConsumer,digitalKontaktInformasjonValidator,
				digitalKontaktinformasjonConsumer, varselInfo, dokumentkatalog );

		qdist011Service = new Qdist011Service(s3Storage, administrerForsendelse, digitalPostService, safJournalpostQueryService);

	}

	@Test
	void ShouldTestSomething(){
		when(administrerForsendelse.hentForsendelse(anyString())).thenReturn(buildHentForsendelseResponseWithDokumentAndArkivSystemAsJoark());
		when(safJournalpostQueryService.hentJournalpost(anyString())).thenReturn(TestUtil.)


	}

}