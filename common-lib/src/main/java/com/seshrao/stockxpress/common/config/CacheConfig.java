package com.seshrao.stockxpress.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration supporting both local (Caffeine) and distributed (Redis) caching.
 * 
 * Profiles:
 * - "local" or "dev": Uses Caffeine in-memory cache
 * - "docker" or "prod": Uses Redis distributed cache
 * 
 * Cache Names:
 * - inventory: Stores inventory availability data
 * - products: Stores product catalog data
 * - orders: Stores order details
 * 
 * @author Seshrao
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.default.ttl:300}")
    private long defaultTtlSeconds;

    @Value("${cache.inventory.ttl:60}")
    private long inventoryTtlSeconds;

    @Value("${cache.products.ttl:600}")
    private long productsTtlSeconds;

    @Value("${cache.orders.ttl:300}")
    private long ordersTtlSeconds;

    @Value("${cache.max.size:1000}")
    private long maxCacheSize;

    /**
     * Caffeine Cache Manager for local development.
     * Active when profiles: local, dev, or test
     */
    @Bean
    @Primary
    @Profile({"local", "dev", "test", "default"})
    @ConditionalOnProperty(name = "cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        log.info("Initializing Caffeine Cache Manager with default TTL: {} seconds", defaultTtlSeconds);
        
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "inventory", "products", "orders"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(defaultTtlSeconds, TimeUnit.SECONDS)
            .recordStats()
            .build());
        
        log.info("Caffeine Cache Manager initialized successfully");
        return cacheManager;
    }

    /**
     * Custom Caffeine configurations for specific caches with different TTLs.
     */
    @Bean
    @Profile({"local", "dev", "test", "default"})
    @ConditionalOnProperty(name = "cache.type", havingValue = "caffeine", matchIfMissing = true)
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(defaultTtlSeconds, TimeUnit.SECONDS)
            .recordStats();
    }

    /**
     * Redis Cache Manager for distributed caching in production.
     * Active when profiles: docker, prod, or staging
     */
    @Bean
    @Primary
    @Profile({"docker", "prod", "staging"})
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Initializing Redis Cache Manager with default TTL: {} seconds", defaultTtlSeconds);
        
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(defaultTtlSeconds))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();

        // Custom cache configurations with specific TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        cacheConfigurations.put("inventory", defaultCacheConfig
            .entryTtl(Duration.ofSeconds(inventoryTtlSeconds)));
        
        cacheConfigurations.put("products", defaultCacheConfig
            .entryTtl(Duration.ofSeconds(productsTtlSeconds)));
        
        cacheConfigurations.put("orders", defaultCacheConfig
            .entryTtl(Duration.ofSeconds(ordersTtlSeconds)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
        
        log.info("Redis Cache Manager initialized successfully with custom TTLs:");
        log.info("  - Inventory Cache TTL: {} seconds", inventoryTtlSeconds);
        log.info("  - Products Cache TTL: {} seconds", productsTtlSeconds);
        log.info("  - Orders Cache TTL: {} seconds", ordersTtlSeconds);
        
        return cacheManager;
    }
}