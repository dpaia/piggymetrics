package com.piggymetrics.statistics.config;

import com.piggymetrics.statistics.domain.timeseries.DataPoint;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import java.util.List;

@Configuration
public class CacheConfig extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @Override
    public CacheManager cacheManager() {
        try {
            EhcacheCachingProvider provider = (EhcacheCachingProvider) Caching.getCachingProvider();
            javax.cache.CacheManager jsr107CacheManager = provider.getCacheManager();
            
            // Create cache configuration for statisticsData
            @SuppressWarnings("unchecked")
            Class<List<DataPoint>> listClass = (Class<List<DataPoint>>) (Class<?>) List.class;
            CacheConfigurationBuilder<String, List<DataPoint>> statisticsDataConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                    String.class, listClass,
                    ResourcePoolsBuilder.heap(150))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(20, java.util.concurrent.TimeUnit.MINUTES)));
            
            // Create cache configuration for exchangeRates  
            CacheConfigurationBuilder<String, Object> exchangeRatesConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(
                    String.class, Object.class,
                    ResourcePoolsBuilder.heap(10))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(30, java.util.concurrent.TimeUnit.MINUTES)));
            
            // Create JSR-107 configurations with statistics enabled
            javax.cache.configuration.Configuration<String, List<DataPoint>> statisticsJsr107Config = 
                Eh107Configuration.fromEhcacheCacheConfiguration(statisticsDataConfig);
            javax.cache.configuration.Configuration<String, Object> exchangeRatesJsr107Config = 
                Eh107Configuration.fromEhcacheCacheConfiguration(exchangeRatesConfig);
            
            // Create the caches if they don't already exist
            if (jsr107CacheManager.getCache("statisticsData") == null) {
                jsr107CacheManager.createCache("statisticsData", statisticsJsr107Config);
            }
            if (jsr107CacheManager.getCache("exchangeRates") == null) {
                jsr107CacheManager.createCache("exchangeRates", exchangeRatesJsr107Config);
            }

            jsr107CacheManager.enableStatistics("statisticsData", true);
            jsr107CacheManager.enableStatistics("exchangeRates", true);

            logger.info("Ehcache initialized with programmatic configuration");
            logger.info("Available cache names: {}", jsr107CacheManager.getCacheNames());
            return new JCacheCacheManager(jsr107CacheManager);
            
        } catch (Exception e) {
            logger.error("Error initializing Ehcache", e);
            throw new RuntimeException("Failed to initialize cache", e);
        }
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logger.error("Cache get error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                logger.error("Cache put error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logger.error("Cache evict error for cache '{}' and key '{}': {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                logger.error("Cache clear error for cache '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }

    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(".");
            key.append(method.getName()).append(":");
            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(",");
                }
            }
            if (key.charAt(key.length() - 1) == ',') {
                key.deleteCharAt(key.length() - 1);
            }
            return key.toString();
        };
    }
}