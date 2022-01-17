package no.nav.dokdistdpi.sdist003.itest.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static no.nav.dokdistdpi.config.cache.CacheConfig.LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE;

public class LocalTestCacheConfig {

	public static final String TKAT020_CACHE = "tkat020Cache";
	public static final String TKAT021_CACHE = "tkat021Cache";
	public static final String STS_CACHE = "stsCache";

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(TKAT021_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.maximumSize(0)
						.build()),
				new CaffeineCache(STS_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.build()),
				new CaffeineCache(LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, TimeUnit.MINUTES)
						.build())
		));
		return manager;
	}
}
