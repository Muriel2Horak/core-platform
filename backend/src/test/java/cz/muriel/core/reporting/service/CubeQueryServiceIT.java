package cz.muriel.core.reporting.service;

import cz.muriel.core.test.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CubeQueryService with Circuit Breaker behavior. Tests
 * resilience patterns: open/closed state transitions, fallback handling.
 * 
 * Uses WireMock for Cube.js API stubbing and per-test CircuitBreakerRegistry
 * for deterministic state management.
 * 
 * ⚠️ @DirtiesContext AFTER_EACH_TEST_METHOD is required here because:
 * - CircuitBreaker state is global and affects other tests
 * - WireMock server needs clean state per test
 * - Each test needs fresh CircuitBreakerRegistry
 */
@SpringBootTest @ActiveProfiles("test") @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CubeQueryServiceIT extends AbstractIntegrationTest {

  private WireMockServer wireMockServer;
  private WebClient cubeWebClient;
  private CircuitBreakerRegistry circuitBreakerRegistry;
  private CubeQueryService cubeQueryService;
  private QueryDeduplicator queryDeduplicator;

  @BeforeEach
  void setUp() {
    // Start WireMock server
    wireMockServer = new WireMockServer(0); // Random port
    wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());

    // Create WebClient pointing to WireMock with error handling
    cubeWebClient = WebClient.builder().baseUrl("http://localhost:" + wireMockServer.port())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultStatusHandler(status -> status.is5xxServerError(),
            clientResponse -> clientResponse.createException()
                .map(ex -> new RuntimeException("Cube.js service error: " + ex.getMessage(), ex)))
        .build();

    // Create CircuitBreakerRegistry with fast test config
    CircuitBreakerConfig config = CircuitBreakerConfig.custom().slidingWindowSize(4)
        .minimumNumberOfCalls(3) // Allow CB to open after just 3 calls
        .failureRateThreshold(50.0f).waitDurationInOpenState(Duration.ofMillis(250))
        .permittedNumberOfCallsInHalfOpenState(2).recordExceptions(Exception.class).build();

    circuitBreakerRegistry = CircuitBreakerRegistry.of(config);

    // Create QueryDeduplicator (pass-through for these tests)
    queryDeduplicator = new QueryDeduplicator() {
      @Override
      public Map<String, Object> executeWithDeduplication(Map<String, Object> query,
          String tenantId, java.util.function.Supplier<Map<String, Object>> executor) {
        return executor.get();
      }
    };

    // Create CubeQueryService with test dependencies
    cubeQueryService = new CubeQueryService(cubeWebClient, circuitBreakerRegistry,
        queryDeduplicator);
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @Test
  void shouldExecuteQueryInClosedState() {
    // Arrange - stub successful Cube.js response
    stubFor(post(urlPathEqualTo("/cubejs-api/v1/load")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
            {
              "data": [
                {"User.id": "1", "User.name": "John"}
              ]
            }
            """)));

    // Act
    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    Map<String, Object> result = cubeQueryService.executeQuery(query, "tenant-1");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).containsKey("data");

    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cubeQueryCircuitBreaker-tenant-1");
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void shouldOpenCircuitBreakerOnFailures() {
    // Arrange - stub 5xx errors to trigger circuit breaker
    stubFor(post(urlPathEqualTo("/cubejs-api/v1/load"))
        .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cubeQueryCircuitBreaker-tenant-1");

    // Act - make 3 failing calls to exceed 50% failure threshold
    // (slidingWindowSize=4, minimumNumberOfCalls=3)
    for (int i = 0; i < 3; i++) {
      try {
        cubeQueryService.executeQuery(query, "tenant-1");
        fail("Should have thrown exception on iteration " + i);
      } catch (Exception e) {
        // Expected - service error
      }
    }

    // Assert - Circuit Breaker should now be OPEN (100% failure rate after 3 calls)
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(cb.getMetrics().getFailureRate()).isEqualTo(100.0f);
  }

  @Test
  void opensOnFailuresThenRecoverToClosedState() throws InterruptedException {
    // Arrange - First stub 503 errors
    stubFor(post(urlPathEqualTo("/cubejs-api/v1/load"))
        .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cubeQueryCircuitBreaker-tenant-1");

    // Act - Step 1: 3 failures -> OPEN
    for (int i = 0; i < 3; i++) {
      try {
        cubeQueryService.executeQuery(query, "tenant-1");
      } catch (Exception e) {
        // Expected
      }
    }
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    // Now remove 503 stub and add 200 stub
    wireMockServer.resetMappings();
    stubFor(post(urlPathEqualTo("/cubejs-api/v1/load")).willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", "application/json").withBody("{\"data\":[]}")));

    // Act - Step 2: Wait for waitDurationInOpenState (250ms) -> HALF_OPEN
    Thread.sleep(300);

    // Act - Step 3: 2 successful calls in HALF_OPEN -> CLOSED
    for (int i = 0; i < 2; i++) {
      cubeQueryService.executeQuery(query, "tenant-1");
    }

    // Assert
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void remainsOpenOnContinuousFailure() throws InterruptedException {
    // Arrange - All calls fail
    stubFor(post(urlPathEqualTo("/cubejs-api/v1/load")).willReturn(aResponse().withStatus(503)));

    Map<String, Object> query = Map.of("dimensions", List.of("User.id"));
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cubeQueryCircuitBreaker-tenant-1");

    // Act - Step 1: 3 failures -> OPEN
    for (int i = 0; i < 3; i++) {
      try {
        cubeQueryService.executeQuery(query, "tenant-1");
      } catch (Exception e) {
        // Expected
      }
    }
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    // Act - Step 2: Wait for HALF_OPEN transition
    Thread.sleep(300);

    // Act - Step 3: Both permitted calls in HALF_OPEN fail -> back to OPEN
    // (permittedNumberOfCallsInHalfOpenState=2)
    for (int i = 0; i < 2; i++) {
      try {
        cubeQueryService.executeQuery(query, "tenant-1");
      } catch (Exception e) {
        // Expected - failures in HALF_OPEN
      }
    }

    // Assert - Should be back to OPEN after 2 failures in HALF_OPEN
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }

  @Test
  void shouldIsolateCircuitBreakerPerTenant() {
    // Arrange - tenant-1 CB
    CircuitBreaker tenant1CB = circuitBreakerRegistry
        .circuitBreaker("cubeQueryCircuitBreaker-tenant-1");
    assertThat(tenant1CB.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // tenant-2 CB
    CircuitBreaker tenant2CB = circuitBreakerRegistry
        .circuitBreaker("cubeQueryCircuitBreaker-tenant-2");
    assertThat(tenant2CB.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

    // Manually open tenant-1 CB
    tenant1CB.transitionToOpenState();

    // Assert: tenant-1 OPEN, tenant-2 still CLOSED
    assertThat(tenant1CB.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(tenant2CB.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }
}
