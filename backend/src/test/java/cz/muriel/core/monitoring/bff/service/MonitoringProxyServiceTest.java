package cz.muriel.core.monitoring.bff.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MonitoringProxyService.
 * 
 * TODO: Add integration tests with WireMock or MockWebServer to verify HTTP
 * proxying, header injection, and error handling.
 * 
 * For now, these tests are disabled pending proper mock setup.
 */
class MonitoringProxyServiceTest {

  @Test @Disabled("Integration test - requires WireMock setup")
  void forwardQuery_shouldAddAuthorizationAndOrgIdHeaders() {
    // TODO: Setup WireMock server
    // TODO: Verify Authorization: Bearer <SAT> header
    // TODO: Verify X-Grafana-Org-Id header
  }

  @Test @Disabled("Integration test - requires WireMock setup")
  void forwardGet_shouldProxyDatasourcesRequest() {
    // TODO: Mock GET /api/datasources
    // TODO: Verify headers and response
  }

  @Test @Disabled("Integration test - requires WireMock setup")
  void forwardQuery_shouldHandleGrafanaError() {
    // TODO: Mock 500 error from Grafana
    // TODO: Verify exception thrown
  }
}
