package cz.muriel.core.reporting.app;

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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for Reporting module.
 */
@Slf4j @Configuration @EnableCaching @EnableScheduling @EnableConfigurationProperties(ReportingProperties.class) @ConditionalOnProperty(name = "reporting.enabled", havingValue = "true", matchIfMissing = true) @RequiredArgsConstructor
public class ReportingConfiguration {

  private final ReportingProperties properties;

  /**
   * 🔴 Shared Redis CacheManager (PRIMARY when Redis available)
   * 
   * This is the PRIMARY CacheManager for the entire application when Redis is
   * enabled. It creates caches dynamically for: - Reporting: reportQueryCache -
   * Monitoring: grafana-queries, grafana-dashboards - Any other @Cacheable
   * annotated methods
   * 
   * Falls back to Caffeine CacheManager (from MonitoringBffConfig) when Redis
   * unavailable.
   */
  @Bean @Primary @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    log.info("🔴 Configuring SHARED Redis cache manager (TTL: {}s, prefix: '{}')",
        properties.getDefaultTtlSeconds(), properties.getCache().getKeyPrefix());

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(properties.getDefaultTtlSeconds()))
        .prefixCacheNameWith(properties.getCache().getKeyPrefix())
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).transactionAware()
        .build();
  }

  // ℹ️ Caffeine CacheManager removed - using shared one from MonitoringBffConfig
  // It creates caches dynamically including "reportQueryCache"

  /**
   * RestClient for Cube.js API.
   */
  @Bean("cubeRestClient")
  public RestClient cubeRestClient() {
    log.info("Configuring Cube.js REST client: {}", properties.getCube().getBaseUrl());

    RestClient.Builder builder = RestClient.builder().baseUrl(properties.getCube().getBaseUrl());

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
  @Bean @ConditionalOnProperty(name = "reporting.cache.provider", havingValue = "redis", matchIfMissing = true)
  public ProxyManager<String> redisProxyManager(RedisConnectionFactory connectionFactory) {
    log.info("Configuring Redis-based ProxyManager for Bucket4j");

    if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
      // Get Redis configuration
      var config = lettuceFactory.getStandaloneConfiguration();
      String host = config.getHostName();
      int port = config.getPort();

      log.info("Creating Redis client for Bucket4j: {}:{}", host, port);

      // Create Redis URI properly
      String redisUri = String.format("redis://%s:%d", host, port);
      RedisClient redisClient = RedisClient.create(redisUri);

      StatefulRedisConnection<String, byte[]> connection = redisClient
          .connect(io.lettuce.core.codec.RedisCodec.of(io.lettuce.core.codec.StringCodec.UTF8,
              io.lettuce.core.codec.ByteArrayCodec.INSTANCE));

      return LettuceBasedProxyManager.builderFor(connection).build();
    }

    throw new IllegalStateException(
        "LettuceConnectionFactory required for Redis-based rate limiting");
  }
}
