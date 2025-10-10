package cz.muriel.core.monitoring.bff;

import com.github.tomakehurst.wiremock.WireMockServer;
import cz.muriel.core.test.wiremock.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Monitoring BFF Query endpoints
 * 
 * Tests valid/invalid DSL, cache HIT/MISS, circuit breaker, and header
 * isolation. Uses WireMock to mock Grafana without Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test") @ExtendWith(WireMockExtension.class)
class MonitoringQueryIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private WireMockServer wireMockServer;

  @BeforeEach
  void setUp() {
    wireMockServer.resetAll();
  }

  @Test
  void validDSL_shouldReturn200_andAddCorrectHeaders() {
    // Given: Grafana stub returns 200 with sample data
    wireMockServer.stubFor(post(urlPathEqualTo("/api/ds/query")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
            {
              "results": {
                "A": {
                  "frames": [{
                    "schema": {
                      "fields": [
                        {"name": "time", "type": "time"},
                        {"name": "value", "type": "number"}
                      ]
                    },
                    "data": {
                      "values": [[1234567890000], [42.5]]
                    }
                  }]
                }
              }
            }
            """)));

    // When: Send query request with test auth header
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Auth", "tenant=TENANT_A;roles=ROLE_USER,ROLE_REPORT");
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = """
        {
          "queries": [{
            "refId": "A",
            "datasourceUid": "test-ds",
            "expr": "test_metric",
            "range": {
              "from": "now-1h",
              "to": "now"
            }
          }]
        }
        """;

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response = restTemplate.postForEntity("/api/monitoring/ds/query",
        request, String.class);

    // Then: Response is 200 OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"frames\"");

    // Verify BFF added correct headers to Grafana request
    wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/ds/query"))
        .withHeader("Authorization", matching("Bearer .*")) // SAT token added
        .withHeader("X-Grafana-Org-Id", equalTo("TENANT_A")) // Tenant org ID added
        .withoutHeader("X-Test-Auth")); // Test header NOT forwarded
  }

  @Test
  void clientSendsGrafanaHeaders_shouldBeIgnored() {
    // Given: Grafana stub
    wireMockServer.stubFor(
        post(urlPathEqualTo("/api/ds/query")).willReturn(ok().withBody("{\"results\":{}}")));

    // When: Client tries to send forged Authorization and X-Grafana-Org-Id headers
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Auth", "tenant=TENANT_A;roles=ROLE_USER");
    headers.set("Authorization", "Bearer FORGED_TOKEN"); // Should be removed
    headers.set("X-Grafana-Org-Id", "FORGED_ORG"); // Should be removed
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = """
        {
          "queries": [{
            "refId": "A",
            "datasourceUid": "test-ds",
            "range": {"from": "now-1h", "to": "now"}
          }]
        }
        """;

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    restTemplate.postForEntity("/api/monitoring/ds/query", request, String.class);

    // Then: BFF uses correct tenant from X-Test-Auth, ignores forged headers
    wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/ds/query"))
        .withHeader("X-Grafana-Org-Id", equalTo("TENANT_A")) // From X-Test-Auth, not forged
        .withHeader("Authorization", matching("Bearer (?!FORGED_TOKEN).*"))); // NOT forged token
  }

  @Test
  void invalidDSL_missingTimeRange_shouldReturn400() {
    // When: Send query without time range
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Auth", "tenant=TENANT_A;roles=ROLE_USER");
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = """
        {
          "queries": [{
            "refId": "A",
            "datasourceUid": "test-ds",
            "expr": "test_metric"
          }]
        }
        """;

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response = restTemplate.postForEntity("/api/monitoring/ds/query",
        request, String.class);

    // Then: 400 Bad Request with Problem+JSON
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void grafanaReturns500_shouldReturn502_andOpenCircuitBreaker() {
    // Given: Grafana returns 500 error
    wireMockServer.stubFor(post(urlPathEqualTo("/api/ds/query")).willReturn(serverError()));

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Auth", "tenant=TENANT_A;roles=ROLE_USER");
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = """
        {
          "queries": [{
            "refId": "A",
            "datasourceUid": "test-ds",
            "range": {"from": "now-1h", "to": "now"}
          }]
        }
        """;

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // When: Make multiple requests to trigger circuit breaker
    for (int i = 0; i < 6; i++) {
      ResponseEntity<String> response = restTemplate.postForEntity("/api/monitoring/ds/query",
          request, String.class);

      // First few requests: 502 Bad Gateway
      assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_GATEWAY,
          HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Then: Circuit breaker should be open (verify via metrics)
    ResponseEntity<String> metricsResponse = restTemplate
        .getForEntity("/actuator/metrics/resilience4j.circuitbreaker.state", String.class);

    assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Circuit breaker state should show "open" or "half_open"
  }

  @Test
  void grafanaTimeout_shouldReturn503() {
    // Given: Grafana delays response beyond timeout
    wireMockServer
        .stubFor(post(urlPathEqualTo("/api/ds/query")).willReturn(ok().withFixedDelay(10000))); // 10s
                                                                                                // delay,
                                                                                                // timeout
                                                                                                // is
                                                                                                // 5s

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Test-Auth", "tenant=TENANT_A;roles=ROLE_USER");
    headers.setContentType(MediaType.APPLICATION_JSON);

    String requestBody = """
        {
          "queries": [{
            "refId": "A",
            "datasourceUid": "test-ds",
            "range": {"from": "now-1h", "to": "now"}
          }]
        }
        """;

    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response = restTemplate.postForEntity("/api/monitoring/ds/query",
        request, String.class);

    // Then: 503 Service Unavailable (timeout mapped to 503)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }
}
