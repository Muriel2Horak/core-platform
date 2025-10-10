package cz.muriel.core.monitoring.bff.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MonitoringBffConfig {

  @Bean @ConfigurationProperties(prefix = "monitoring.grafana")
  public GrafanaProperties grafanaProperties() {
    return new GrafanaProperties();
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50) // Open circuit if 50% of requests fail
        .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
        .slidingWindowSize(10) // Track last 10 requests
        .minimumNumberOfCalls(5) // Min 5 calls before calculating failure rate
        .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();
    
    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  public CircuitBreaker grafanaCircuitBreaker(CircuitBreakerRegistry registry) {
    CircuitBreaker circuitBreaker = registry.circuitBreaker("grafana");
    
    // Log circuit breaker state changes
    circuitBreaker.getEventPublisher()
        .onStateTransition(event -> 
            log.warn("Circuit breaker state changed: {} -> {}", 
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState()))
        .onError(event -> 
            log.error("Circuit breaker error: {}", event.getThrowable().getMessage()));
    
    return circuitBreaker;
  }

  @Bean
  public WebClient grafanaWebClient(GrafanaProperties props, CircuitBreaker circuitBreaker) {
    // Connection pool configuration
    ConnectionProvider provider = ConnectionProvider.builder("grafana").maxConnections(100)
        .maxIdleTime(Duration.ofSeconds(20)).maxLifeTime(Duration.ofSeconds(60))
        .pendingAcquireTimeout(Duration.ofSeconds(10)).evictInBackground(Duration.ofSeconds(120))
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
    ExchangeFilterFunction circuitBreakerFilter = (request, next) -> 
        next.exchange(request)
            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(throwable -> {
              log.error("Grafana request failed (circuit breaker): {}", throwable.getMessage());
              return Mono.error(new RuntimeException("Grafana service unavailable", throwable));
            });

    return WebClient.builder()
        .baseUrl(props.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .filter(circuitBreakerFilter)
        .build();
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
