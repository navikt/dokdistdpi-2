package no.nav.dokdistdpi.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

	public static final String TKAT020_CACHE = "tkat020Cache";
	public static final String TKAT021_CACHE = "tkat021Cache";
	public static final String STS_CACHE = "stsCache";
	public static final String MASKINPORTEN_CACHE = "maskinportenCache";
	public static final String LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE = "LightweightSafJournalpostQdist011Cache";
	public static final String SAF_JOURNALPOST_QDIST011_CACHE = "SafJournalpostQueryServiceImplQdist011Cache";

	@Bean
	@Primary
	@Profile({"nais", "local"})
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.DAYS)
						.build()),
				new CaffeineCache(TKAT021_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(1, TimeUnit.DAYS).build()),
				new CaffeineCache(STS_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(55, TimeUnit.MINUTES).build()),
				new CaffeineCache(LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(30, TimeUnit.SECONDS).build()),
				new CaffeineCache(SAF_JOURNALPOST_QDIST011_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(30, TimeUnit.SECONDS).build()),
				new CaffeineCache(MASKINPORTEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(120, TimeUnit.SECONDS).build())
		));
		return manager;
	}
}
