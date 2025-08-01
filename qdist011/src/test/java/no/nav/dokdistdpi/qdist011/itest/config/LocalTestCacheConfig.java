package no.nav.dokdistdpi.qdist011.itest.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistdpi.config.cache.CacheConfig.AZURE_CLIENT_CREDENTIAL_TOKEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.MASKINPORTEN_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.SAF_JOURNALPOST_QDIST011_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT020_CACHE;
import static no.nav.dokdistdpi.config.cache.CacheConfig.TKAT021_CACHE;

@Configuration
public class LocalTestCacheConfig {

	@Bean
	CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(
				new CaffeineCache(TKAT020_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, DAYS).build()),
				new CaffeineCache(TKAT021_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, DAYS).build()),
				new CaffeineCache(LIGHTWEIGHT_SAF_JOURNALPOST_QDIST011_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, SECONDS).build()),
				new CaffeineCache(SAF_JOURNALPOST_QDIST011_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, SECONDS).build()),
				new CaffeineCache(MASKINPORTEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, SECONDS).build()),
				new CaffeineCache(AZURE_CLIENT_CREDENTIAL_TOKEN_CACHE, Caffeine.newBuilder()
						.expireAfterWrite(0, MINUTES)
						.maximumSize(2).build())
		));
		return manager;
	}
}
