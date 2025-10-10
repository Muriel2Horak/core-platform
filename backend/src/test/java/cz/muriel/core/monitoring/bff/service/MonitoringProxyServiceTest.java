package cz.muriel.core.monitoring.bff.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import cz.muriel.core.test.wiremock.WireMockExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for MonitoringProxyService with WireMock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(WireMockExtension.class)
class MonitoringProxyServiceTest {

  @Autowired
  private MonitoringProxyService proxyService;
  
  @Autowired
  private TenantOrgService tenantOrgService;

  private Jwt createMockJwt(String tenantId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("tenant", tenantId);
    claims.put("preferred_username", "test-user");
    claims.put("realm_access", Map.of("roles", List.of("ROLE_USER")));
    
    Map<String, Object> headers = new HashMap<>();
    headers.put("alg", "RS256");
    
    return new Jwt("mock-token", 
        Instant.now(), 
        Instant.now().plusSeconds(3600),
        headers, 
        claims);
  }

  @Test
  void forwardQuery_shouldAddAuthorizationAndOrgIdHeaders(WireMockServer wireMock) {
    // Setup mock Grafana
    wireMock.stubFor(WireMock.post("/api/ds/query")
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"results\":{}}")));

    Map<String, Object> requestBody = Map.of("queries", List.of(Map.of("refId", "A")));
    Jwt jwt = createMockJwt("TENANT_A");

    // Execute
    ResponseEntity<String> response = proxyService.forwardQuery(jwt, requestBody);

    // Verify response
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("results");

    // Verify headers were sent to Grafana
    var binding = tenantOrgService.resolve(jwt);
    
    wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/ds/query"))
        .withHeader("Authorization", WireMock.equalTo("Bearer " + binding.serviceAccountToken()))
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(binding.orgId())))
        .withHeader("Content-Type", WireMock.containing("application/json")));
  }

  @Test
  void forwardGet_shouldProxyDatasourcesRequest(WireMockServer wireMock) {
    // Setup mock
    wireMock.stubFor(WireMock.get("/api/datasources")
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("[{\"id\":1,\"name\":\"Prometheus\"}]")));

    Jwt jwt = createMockJwt("TENANT_A");

    // Execute
    ResponseEntity<String> response = proxyService.forwardGet(jwt, "/api/datasources");

    // Verify response
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("Prometheus");

    // Verify headers
    var binding = tenantOrgService.resolve(jwt);
    
    wireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/datasources"))
        .withHeader("Authorization", WireMock.equalTo("Bearer " + binding.serviceAccountToken()))
        .withHeader("X-Grafana-Org-Id", WireMock.equalTo(String.valueOf(binding.orgId()))));
  }

  @Test
  void forwardQuery_shouldHandleGrafanaError(WireMockServer wireMock) {
    // Setup error response
    wireMock.stubFor(WireMock.post("/api/ds/query")
        .willReturn(WireMock.aResponse()
            .withStatus(500)
            .withBody("{\"error\":\"Internal Server Error\"}")));

    Map<String, Object> requestBody = Map.of("queries", List.of(Map.of("refId", "A")));
    Jwt jwt = createMockJwt("TENANT_A");

    // Execute and verify exception
    assertThatThrownBy(() -> proxyService.forwardQuery(jwt, requestBody))
        .hasMessageContaining("500");
  }
}


