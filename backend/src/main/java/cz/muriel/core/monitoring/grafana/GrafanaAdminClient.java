package cz.muriel.core.monitoring.grafana;

import cz.muriel.core.monitoring.grafana.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 🔧 GRAFANA ADMIN API CLIENT
 * 
 * REST klient pro Grafana Admin API s podporou:
 * - Organization management (create/delete)
 * - Service Account management (create/delete/list)
 * - Service Account Token generation
 * - Circuit breaker pro resilience
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrafanaAdminClient {

  private final RestTemplate restTemplate;

  @Value("${grafana.admin.url:http://grafana:3000}")
  private String grafanaUrl;

  @Value("${grafana.admin.username:admin}")
  private String adminUsername;

  @Value("${grafana.admin.password:admin}")
  private String adminPassword;

  /**
   * 🏢 CREATE ORGANIZATION
   * Vytvoří novou Grafana organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createOrganizationFallback")
  public CreateOrgResponse createOrganization(String orgName) {
    log.info("🏢 Creating Grafana organization: {}", orgName);

    String url = grafanaUrl + "/api/orgs";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    CreateOrgRequest request = new CreateOrgRequest(orgName);
    HttpEntity<CreateOrgRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateOrgResponse> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, CreateOrgResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        CreateOrgResponse orgResponse = response.getBody();
        log.info("✅ Grafana organization created: {} (orgId: {})", orgName, orgResponse.getOrgId());
        return orgResponse;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to create Grafana organization: {}", orgName, e);
      throw new GrafanaApiException("Failed to create organization: " + e.getMessage(), e);
    }
  }

  /**
   * 🤖 CREATE SERVICE ACCOUNT
   * Vytvoří service account v dané organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createServiceAccountFallback")
  public CreateServiceAccountResponse createServiceAccount(Long orgId, String name, String role) {
    log.info("🤖 Creating Grafana service account: {} in orgId: {} with role: {}", name, orgId,
        role);

    String url = grafanaUrl + "/api/serviceaccounts";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));

    CreateServiceAccountRequest request = new CreateServiceAccountRequest(name, role);
    HttpEntity<CreateServiceAccountRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateServiceAccountResponse> response = restTemplate.exchange(url,
          HttpMethod.POST, entity, CreateServiceAccountResponse.class);

      if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
        CreateServiceAccountResponse saResponse = response.getBody();
        log.info("✅ Grafana service account created: {} (id: {})", name, saResponse.getId());
        return saResponse;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to create Grafana service account: {} in orgId: {}", name, orgId, e);
      throw new GrafanaApiException("Failed to create service account: " + e.getMessage(), e);
    }
  }

  /**
   * 🔑 CREATE SERVICE ACCOUNT TOKEN
   * Vygeneruje token pro service account
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createServiceAccountTokenFallback")
  public CreateServiceAccountTokenResponse createServiceAccountToken(Long orgId, Long serviceAccountId,
      String tokenName) {
    log.info("🔑 Creating Grafana service account token: {} for SA: {} in orgId: {}", tokenName,
        serviceAccountId, orgId);

    String url = grafanaUrl + "/api/serviceaccounts/" + serviceAccountId + "/tokens";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));

    CreateServiceAccountTokenRequest request = new CreateServiceAccountTokenRequest(tokenName);
    HttpEntity<CreateServiceAccountTokenRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateServiceAccountTokenResponse> response = restTemplate.exchange(url,
          HttpMethod.POST, entity, CreateServiceAccountTokenResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        CreateServiceAccountTokenResponse tokenResponse = response.getBody();
        log.info("✅ Grafana service account token created: {} (key: {}***)", tokenName,
            tokenResponse.getKey().substring(0, Math.min(10, tokenResponse.getKey().length())));
        return tokenResponse;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to create Grafana service account token: {} for SA: {} in orgId: {}",
          tokenName, serviceAccountId, orgId, e);
      throw new GrafanaApiException("Failed to create service account token: " + e.getMessage(),
          e);
    }
  }

  /**
   * 🗑️ DELETE ORGANIZATION
   * Smaže Grafana organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "deleteOrganizationFallback")
  public void deleteOrganization(Long orgId) {
    log.info("🗑️ Deleting Grafana organization: {}", orgId);

    String url = grafanaUrl + "/api/orgs/" + orgId;
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(url, HttpMethod.DELETE, entity, 
              new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

      if (response.getStatusCode() == HttpStatus.OK) {
        log.info("✅ Grafana organization deleted: {}", orgId);
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to delete Grafana organization: {}", orgId, e);
      throw new GrafanaApiException("Failed to delete organization: " + e.getMessage(), e);
    }
  }

  /**
   * 🔍 LIST SERVICE ACCOUNTS
   * Vrátí seznam service accounts v organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "listServiceAccountsFallback")
  public List<ServiceAccountInfo> listServiceAccounts(Long orgId) {
    log.debug("🔍 Listing Grafana service accounts in orgId: {}", orgId);

    String url = grafanaUrl + "/api/serviceaccounts/search";
    HttpHeaders headers = createAuthHeaders();
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<ServiceAccountSearchResponse> response = restTemplate.exchange(url,
          HttpMethod.GET, entity, ServiceAccountSearchResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        List<ServiceAccountInfo> accounts = response.getBody().getServiceAccounts();
        log.debug("✅ Found {} service accounts in orgId: {}", accounts.size(), orgId);
        return accounts;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to list Grafana service accounts in orgId: {}", orgId, e);
      throw new GrafanaApiException("Failed to list service accounts: " + e.getMessage(), e);
    }
  }

  /**
   * 🔐 CREATE AUTH HEADERS
   * Vytvoří HTTP headers s Basic Auth
   */
  private HttpHeaders createAuthHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String auth = adminUsername + ":" + adminPassword;
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set("Authorization", "Basic " + encodedAuth);
    return headers;
  }

  // ==================== FALLBACK METHODS ====================

  @SuppressWarnings("unused")
  private CreateOrgResponse createOrganizationFallback(String orgName, Exception e) {
    log.error("⚠️ Circuit breaker: createOrganization fallback for {}", orgName, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private CreateServiceAccountResponse createServiceAccountFallback(Long orgId, String name,
      String role, Exception e) {
    log.error("⚠️ Circuit breaker: createServiceAccount fallback for {} in orgId: {}", name,
        orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private CreateServiceAccountTokenResponse createServiceAccountTokenFallback(Long orgId,
      Long serviceAccountId, String tokenName, Exception e) {
    log.error("⚠️ Circuit breaker: createServiceAccountToken fallback for {} in SA: {}, orgId: {}",
        tokenName, serviceAccountId, orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private void deleteOrganizationFallback(Long orgId, Exception e) {
    log.error("⚠️ Circuit breaker: deleteOrganization fallback for orgId: {}", orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private List<ServiceAccountInfo> listServiceAccountsFallback(Long orgId, Exception e) {
    log.error("⚠️ Circuit breaker: listServiceAccounts fallback for orgId: {}", orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }
}
