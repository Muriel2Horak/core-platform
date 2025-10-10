package cz.muriel.core.reporting.app;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Reporting module.
 */
@Slf4j
@Configuration
@EnableCaching
@EnableConfigurationProperties(ReportingProperties.class)
@ConditionalOnProperty(name = "reporting.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReportingConfiguration {

    private final ReportingProperties properties;

    /**
     * Redis-based cache manager (primary if Redis is available).
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "reporting.cache.provider", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis cache manager for reporting with TTL: {}s", 
            properties.getDefaultTtlSeconds());

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(properties.getDefaultTtlSeconds()))
            .prefixCacheNameWith(properties.getCache().getKeyPrefix())
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }

    /**
     * Caffeine-based cache manager (fallback).
     */
    @Bean
    @ConditionalOnProperty(name = "reporting.cache.provider", havingValue = "caffeine")
    public CacheManager caffeineCacheManager() {
        log.info("Configuring Caffeine cache manager for reporting with TTL: {}s", 
            properties.getDefaultTtlSeconds());

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(properties.getDefaultTtlSeconds(), TimeUnit.SECONDS)
            .recordStats());
        return cacheManager;
    }

    /**
     * RestClient for Cube.js API.
     */
    @Bean("cubeRestClient")
    public RestClient cubeRestClient() {
        log.info("Configuring Cube.js REST client: {}", properties.getCube().getBaseUrl());

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(properties.getCube().getBaseUrl());

        // Add authorization if token is present
        if (properties.getCube().getApiToken() != null 
            && !properties.getCube().getApiToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getCube().getApiToken());
        }

        return builder.build();
    }

    /**
     * ProxyManager for Bucket4j rate limiting with Redis backend.
     */
    @Bean
    @ConditionalOnProperty(name = "reporting.cache.provider", havingValue = "redis", matchIfMissing = true)
    public ProxyManager<String> redisProxyManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis-based ProxyManager for Bucket4j");

        if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
            RedisClient redisClient = RedisClient.create(lettuceFactory.getStandaloneConfiguration().getHostName());
            StatefulRedisConnection<String, byte[]> connection = redisClient.connect(io.lettuce.core.codec.RedisCodec.of(
                io.lettuce.core.codec.StringCodec.UTF8, 
                io.lettuce.core.codec.ByteArrayCodec.INSTANCE
            ));
            
            return LettuceBasedProxyManager.builderFor(connection)
                .build();
        }

        throw new IllegalStateException("LettuceConnectionFactory required for Redis-based rate limiting");
    }
}

