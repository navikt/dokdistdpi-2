package no.nav.dokdistdpi.qdist011.itest;

import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig;
import no.nav.dokdistdpi.s3storage.DokDistDokumentFraS3;
import no.nav.dokdistdpi.s3storage.JsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.jms.Queue;
import java.util.Objects;
import java.util.UUID;

import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.CALL_ID;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.HOVEDDOK_TEST_CONTENT;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.VEDLEGG1_TEST_CONTENT;
import static no.nav.dokdistdpi.qdist011.itest.config.ApplicationTestConfig.VEDLEGG2_TEST_CONTENT;
import static no.nav.dokdistdpi.s3storage.S3Configuration.BUCKET_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist011IT {

	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
	private Queue qdist011;
	@Autowired
	private Queue qdist011FunksjonellFeil;
	@Autowired
	private Queue backoutQueue;
	@Autowired
	private AmazonS3 amazonS3;
	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	public void setupBefore() {
		CALL_ID = UUID.randomUUID().toString();
		Objects.requireNonNull(cacheManager.getCache(TKAT020_CACHE)).clear();
		Objects.requireNonNull(cacheManager.getCache(TKAT021_CACHE)).clear();
		reset(amazonS3);
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_HOVEDDOK)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(HOVEDDOK_TEST_CONTENT.getBytes()).build()));
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG1)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(VEDLEGG1_TEST_CONTENT.getBytes()).build()));
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), eq(DOKUMENT_OBJEKT_REFERANSE_VEDLEGG2)))
				.thenReturn(JsonSerializer.serialize(DokDistDokumentFraS3.builder().pdf(VEDLEGG2_TEST_CONTENT.getBytes()).build()));
	}
}
