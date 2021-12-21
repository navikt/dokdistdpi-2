package no.nav.dokdistdpi.qdist011.itest.config;

import com.amazonaws.services.s3.AmazonS3;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistdpi.certificate.KeyStoreProperties;
import no.nav.dokdistdpi.config.cache.CacheConfig;
import no.nav.dokdistdpi.config.prop.DpiClientProperties;
import no.nav.dokdistdpi.config.prop.MqGatewayProperties;
import no.nav.dokdistdpi.config.prop.ServiceuserProperties;
import no.nav.dokdistdpi.s3storage.AmazonS3Storage;
import no.nav.dokdistdpi.s3storage.DokDistDokumentFraS3;
import no.nav.dokdistdpi.s3storage.JsonSerializer;
import no.nav.dokdistdpi.s3storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import java.io.IOException;
import java.util.UUID;

import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdpi.s3storage.S3Configuration.BUCKET_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({ServiceuserProperties.class,
		KeyStoreProperties.class,
		MqGatewayProperties.class,
		DpiClientProperties.class,
})
@Import({CacheConfig.class,
		JmsItestConfig.class,
})
@ComponentScan(basePackages = "no.nav.dokdistdpi")
public class ApplicationTestConfig {

	public static final String DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK = "dokumentObjektReferanseHoveddok";
	public static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1 = "dokumentObjektReferanseVedlegg1";
	public static final String DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2 = "dokumentObjektReferanseVedlegg2";
	public static final String HOVEDDOK_TEST_CONTENT = "HOVEDDOK_TEST_CONTENT";
	public static final String VEDLEGG1_TEST_CONTENT = "VEDLEGG1_TEST_CONTENT";
	public static final String VEDLEGG2_TEST_CONTENT = "VEDLEGG2_TEST_CONTENT";
	public static String CALL_ID;

	@Autowired
	private AmazonS3 amazonS3;

	@Autowired
	private CacheManager cacheManager;

	@Bean
	public AmazonS3 s3() {
		return mock(AmazonS3.class);
	}

	@Bean
	public Storage storage(AmazonS3 s3) {
		return new AmazonS3Storage(s3);
	}

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		cacheManager.getCache(TKAT020_CACHE).clear();
		cacheManager.getCache(TKAT021_CACHE).clear();
		reset(amazonS3);
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build()));
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build()));
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build()));
	}

}
