package cz.muriel.core.monitoring.bff.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration @EnableCaching @Slf4j
public class MonitoringBffConfig {

  @Bean @ConfigurationProperties(prefix = "monitoring.grafana")
  public GrafanaProperties grafanaProperties() {
    return new GrafanaProperties();
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom().failureRateThreshold(50) // Open
                                                                                         // circuit
                                                                                         // if 50%
                                                                                         // of
                                                                                         // requests
                                                                                         // fail
        .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
        .slidingWindowSize(10) // Track last 10 requests
        .minimumNumberOfCalls(5) // Min 5 calls before calculating failure rate
        .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open
        .automaticTransitionFromOpenToHalfOpenEnabled(true).build();

    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  public CircuitBreaker grafanaCircuitBreaker(CircuitBreakerRegistry registry) {
    CircuitBreaker circuitBreaker = registry.circuitBreaker("grafana");

    // Log circuit breaker state changes
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> log.warn("Circuit breaker state changed: {} -> {}",
            event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
        .onError(
            event -> log.error("Circuit breaker error: {}", event.getThrowable().getMessage()));

    return circuitBreaker;
  }

  @Bean
  public WebClient grafanaWebClient(GrafanaProperties props, CircuitBreaker circuitBreaker) {
    // Connection pool configuration (production-ready)
    ConnectionProvider provider = ConnectionProvider.builder("grafana-pool").maxConnections(100) // Max
                                                                                                 // 100
                                                                                                 // concurrent
                                                                                                 // connections
        .maxIdleTime(Duration.ofSeconds(20)) // Close idle connections after 20s
        .maxLifeTime(Duration.ofMinutes(5)) // Max connection lifetime: 5 minutes
        .pendingAcquireTimeout(Duration.ofSeconds(5)) // Wait max 5s for available connection
        .evictInBackground(Duration.ofSeconds(30)) // Background eviction every 30s
        .build();

    // HttpClient with timeouts and connection pool
    HttpClient httpClient = HttpClient.create(provider)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000).responseTimeout(Duration.ofSeconds(30))
        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

    // Increase buffer size for large dashboard responses
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .build();

    // Add circuit breaker filter
    ExchangeFilterFunction circuitBreakerFilter = (request, next) -> next.exchange(request)
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)).onErrorResume(throwable -> {
          log.error("Grafana request failed (circuit breaker): {}", throwable.getMessage());
          return Mono.error(new RuntimeException("Grafana service unavailable", throwable));
        });

    return WebClient.builder().baseUrl(props.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient)).exchangeStrategies(strategies)
        .filter(circuitBreakerFilter).build();
  }

  /**
   * 🔹 Shared Caffeine CacheManager (fallback when Redis unavailable)
   * 
   * This is the PRIMARY CacheManager when Redis is not available. It dynamically
   * creates caches for: - Monitoring: grafana-queries, grafana-dashboards -
   * Reporting: reportQueryCache - Any other @Cacheable annotated methods
   * 
   * TTL: 30 seconds (short for real-time monitoring data) Max size: 1000 entries
   * per cache Stats enabled for monitoring
   */
  @Bean @ConditionalOnMissingBean(CacheManager.class) @Primary
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("grafana-queries",
        "grafana-dashboards", "reportQueryCache");
    cacheManager.setCaffeine(caffeineConfig());
    return cacheManager;
  }

  @Bean
  public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS) // Short TTL for real-time
                                                                        // data
        .maximumSize(1000) // Max 1000 entries
        .recordStats(); // Enable stats for monitoring
  }

  @Data
  public static class GrafanaProperties {
    private String baseUrl = "http://grafana:3000";
    private int maxBodySize = 10485760; // 10MB
    private int connectTimeout = 5000;
    private int readTimeout = 30000;
    private int writeTimeout = 30000;
  }
}
