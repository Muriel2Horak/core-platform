package cz.muriel.core.reporting.service;

import cz.muriel.core.test.config.TestQueryConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for CubeQueryService with Circuit Breaker behavior. Tests
 * resilience patterns: open/closed state transitions, fallback handling.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestQueryConfig.class)
class CubeQueryServiceIT {

  @Mock
  private WebClient cubeWebClient;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  @Mock
  private QueryDeduplicator queryDeduplicator;

  private CircuitBreakerRegistry circuitBreakerRegistry;
  private CubeQueryService cubeQueryService;
  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Create real CircuitBreakerRegistry with test config
    var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
        .slidingWindowSize(10).failureRateThreshold(50.0f)
        .slowCallDurationThreshold(java.time.Duration.ofSeconds(5)).slowCallRateThreshold(50.0f)
        .waitDurationInOpenState(java.time.Duration.ofSeconds(30))
        .permittedNumberOfCallsInHalfOpenState(5).build();

    circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
    cubeQueryService = new CubeQueryService(cubeWebClient, circuitBreakerRegistry, queryDeduplicator);
    circuitBreaker = circuitBreakerRegistry.circuitBreaker("cubeQueryCircuitBreaker-tenant-1");
    circuitBreaker.reset();
    
    // Setup QueryDeduplicator mock to pass through
    when(queryDeduplicator.executeWithDeduplication(any(), anyString(), any()))
        .thenAnswer(invocation -> {
          java.util.function.Supplier<Map<String, Object>> supplier = invocation.getArgument(2);
          return supplier.get();
        });
  }

  @Test
  void shouldExecuteQueryInClosedState() {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    Map<String, Object> expectedResponse = Map.of("data", List.of());

    when(cubeWebClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(expectedResponse));

    // Act
    Map<String, Object> result = cubeQueryService.executeQuery(query, "tenant-1");

    // Assert
    assertNotNull(result);
    assertEquals(expectedResponse, result);
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  void shouldTransitionToOpenStateAfterFailureThreshold() {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    when(cubeWebClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(Map.class))
        .thenReturn(Mono.error(new RuntimeException("Cube.js timeout")));

    // Act & Assert: Fail 6 times to exceed 50% failure threshold
    for (int i = 0; i < 6; i++) {
      try {
        cubeQueryService.executeQuery(query, "tenant-1");
        fail("Should have thrown exception");
      } catch (Exception e) {
        // Expected
      }
    }

    // Circuit Breaker should now be OPEN
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
  }

  @Test
  void shouldTransitionToHalfOpenAndClose() {
    // Arrange
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));

    when(cubeWebClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenAnswer(inv -> requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("data", List.of())));

    // Force HALF_OPEN state
    circuitBreaker.transitionToHalfOpenState();

    // Act: Make 5 successful test calls
    for (int i = 0; i < 5; i++) {
      cubeQueryService.executeQuery(query, "tenant-1");
    }

    // Assert: Circuit should close
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  void shouldIsolateCircuitBreakerPerTenant() {
    // Arrange - tenant-1 CB
    CircuitBreaker tenant1CB = circuitBreakerRegistry
        .circuitBreaker("cubeQueryCircuitBreaker-tenant-1");
    assertEquals(CircuitBreaker.State.CLOSED, tenant1CB.getState());

    // tenant-2 CB
    CircuitBreaker tenant2CB = circuitBreakerRegistry
        .circuitBreaker("cubeQueryCircuitBreaker-tenant-2");
    assertEquals(CircuitBreaker.State.CLOSED, tenant2CB.getState());

    // Manually open tenant-1 CB
    tenant1CB.transitionToOpenState();

    // Assert: tenant-1 OPEN, tenant-2 still CLOSED
    assertEquals(CircuitBreaker.State.OPEN, tenant1CB.getState());
    assertEquals(CircuitBreaker.State.CLOSED, tenant2CB.getState());
  }
}
