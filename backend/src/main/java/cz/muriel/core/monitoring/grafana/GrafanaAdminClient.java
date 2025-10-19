package cz.muriel.core.monitoring.grafana;

import cz.muriel.core.monitoring.grafana.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🔧 GRAFANA ADMIN API CLIENT
 * 
 * REST klient pro Grafana Admin API s podporou: - Organization management
 * (create/delete) - Service Account management (create/delete/list) - Service
 * Account Token generation - Circuit breaker pro resilience
 */
@Slf4j @Component @RequiredArgsConstructor
public class GrafanaAdminClient {

  private final RestTemplate restTemplate;

  @Value("${grafana.admin.url:http://grafana:3000}")
  private String grafanaUrl;

  @Value("${grafana.admin.username:admin}")
  private String adminUsername;

  @Value("${grafana.admin.password:admin}")
  private String adminPassword;

  /**
   * 🏢 CREATE ORGANIZATION Vytvoří novou Grafana organizaci NEBO vrátí existující
   * pokud už existuje
   * 
   * ✨ IDEMPOTENT: Pokud organizace už existuje (409 Conflict), najde ji a vrátí
   * její ID místo chyby
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createOrganizationFallback")
  public CreateOrgResponse createOrganization(String orgName) {
    log.info("🏢 Creating Grafana organization: {}", orgName);

    // 🆕 STEP 1: Check if organization already exists
    try {
      Optional<OrgInfo> existing = findOrgByName(orgName);
      if (existing.isPresent()) {
        log.info("ℹ️  Grafana organization already exists: {} (orgId: {})", orgName,
            existing.get().getId());
        // Return existing org as if we created it (idempotent behavior)
        CreateOrgResponse response = new CreateOrgResponse();
        response.setOrgId(existing.get().getId());
        response.setMessage("Organization already exists");
        return response;
      }
    } catch (Exception e) {
      log.debug("Could not check for existing org (will try to create): {}", e.getMessage());
      // Continue to creation attempt
    }

    // STEP 2: Create new organization
    String url = grafanaUrl + "/api/orgs";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    CreateOrgRequest request = new CreateOrgRequest(orgName);
    HttpEntity<CreateOrgRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateOrgResponse> response = restTemplate.exchange(url, HttpMethod.POST,
          entity, CreateOrgResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        CreateOrgResponse orgResponse = response.getBody();
        log.info("✅ Grafana organization created: {} (orgId: {})", orgName, orgResponse.getOrgId());
        return orgResponse;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
      // 🆕 HANDLE 409 CONFLICT: Organization name already taken
      log.warn("⚠️  Organization '{}' already exists (409 Conflict), fetching existing org ID...",
          orgName);

      try {
        Optional<OrgInfo> existing = findOrgByName(orgName);
        if (existing.isPresent()) {
          log.info("✅ Found existing organization: {} (orgId: {})", orgName,
              existing.get().getId());
          CreateOrgResponse response = new CreateOrgResponse();
          response.setOrgId(existing.get().getId());
          response.setMessage("Organization already exists (recovered from 409)");
          return response;
        } else {
          throw new GrafanaApiException(
              "Organization exists but could not be found by name: " + orgName);
        }
      } catch (Exception ex) {
        log.error("❌ Failed to recover from 409 Conflict for org: {}", orgName, ex);
        throw new GrafanaApiException("Failed to handle existing organization: " + ex.getMessage(),
            ex);
      }
    } catch (Exception e) {
      log.error("❌ Failed to create Grafana organization: {}", orgName, e);
      throw new GrafanaApiException("Failed to create organization: " + e.getMessage(), e);
    }
  }

  /**
   * 🔍 FIND SERVICE ACCOUNT BY NAME Finds existing service account in
   * organization
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "findServiceAccountByNameFallback")
  public Optional<CreateServiceAccountResponse> findServiceAccountByName(Long orgId, String name) {
    log.debug("🔍 Searching for service account: {} in orgId: {}", name, orgId);

    String url = grafanaUrl + "/api/serviceaccounts/search?query=" + name;
    HttpHeaders headers = createAuthHeaders();
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));

    try {
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET,
          new HttpEntity<>(headers), new ParameterizedTypeReference<Map<String, Object>>() {
          });

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> serviceAccounts = (List<Map<String, Object>>) body
            .get("serviceAccounts");

        if (serviceAccounts != null && !serviceAccounts.isEmpty()) {
          for (Map<String, Object> sa : serviceAccounts) {
            // Match by 'name' field (not 'login' which has 'sa-' prefix)
            if (name.equals(sa.get("name"))) {
              CreateServiceAccountResponse saResponse = new CreateServiceAccountResponse();
              saResponse.setId(((Number) sa.get("id")).longValue());
              saResponse.setName((String) sa.get("name"));
              saResponse.setLogin((String) sa.get("login"));
              saResponse.setRole((String) sa.get("role"));
              saResponse.setIsDisabled((Boolean) sa.getOrDefault("isDisabled", false));
              log.debug("✅ Found existing service account: {} (id: {})", name, saResponse.getId());
              return Optional.of(saResponse);
            }
          }
        }
      }
      return Optional.empty();
    } catch (Exception e) {
      log.warn("⚠️ Failed to search for service account: {}", name, e);
      return Optional.empty();
    }
  }

  /**
   * 🤖 CREATE SERVICE ACCOUNT Vytvoří service account v dané organizaci NEBO
   * vrátí existující
   * 
   * ✨ IDEMPOTENT: Pokud service account už existuje, najde ho a vrátí jeho ID
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createServiceAccountFallback")
  public CreateServiceAccountResponse createServiceAccount(Long orgId, String name, String role) {
    log.info("🤖 Creating Grafana service account: {} in orgId: {} with role: {}", name, orgId,
        role);

    // 🆕 STEP 1: Check if service account already exists
    Optional<CreateServiceAccountResponse> existing = findServiceAccountByName(orgId, name);
    if (existing.isPresent()) {
      CreateServiceAccountResponse saResponse = existing.get();
      log.info("ℹ️ Service account already exists: {} (id: {}) - using existing", name,
          saResponse.getId());
      return saResponse;
    }

    // STEP 2: Create new service account
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
   * 🔑 CREATE SERVICE ACCOUNT TOKEN Vygeneruje token pro service account
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createServiceAccountTokenFallback")
  public CreateServiceAccountTokenResponse createServiceAccountToken(Long orgId,
      Long serviceAccountId, String tokenName) {
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
      // Check if token already exists (400 Bad Request with "already exists" message)
      String errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.contains("400")
          && (errorMessage.contains("already exists")
              || errorMessage.contains("ErrTokenAlreadyExists"))) {
        log.warn(
            "⚠️ Service account token already exists: {} for SA: {} in orgId: {} - using placeholder token",
            tokenName, serviceAccountId, orgId);

        // Return dummy response since Grafana API doesn't support token retrieval
        // Token will be empty string - binding will be saved anyway for idempotency
        CreateServiceAccountTokenResponse dummyResponse = new CreateServiceAccountTokenResponse();
        dummyResponse.setName(tokenName);
        dummyResponse.setKey(""); // Empty token - cannot retrieve existing token from Grafana
        return dummyResponse;
      }

      log.error("❌ Failed to create Grafana service account token: {} for SA: {} in orgId: {}",
          tokenName, serviceAccountId, orgId, e);
      throw new GrafanaApiException("Failed to create service account token: " + e.getMessage(), e);
    }
  }

  /**
   * � FIND ORGANIZATION BY NAME Najde organizaci podle jména, vrátí Optional
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "findOrgByNameFallback")
  public Optional<OrgInfo> findOrgByName(String orgName) {
    log.debug("🔍 Finding Grafana organization by name: {}", orgName);

    String url = grafanaUrl + "/api/orgs";
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ParameterizedTypeReference<List<OrgInfo>> responseType = new ParameterizedTypeReference<List<OrgInfo>>() {
      };
      ResponseEntity<List<OrgInfo>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
          responseType);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody().stream().filter(org -> org.getName().equals(orgName)).findFirst();
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to find Grafana organization by name: {}", orgName, e);
      throw new GrafanaApiException("Failed to find organization: " + e.getMessage(), e);
    }
  }

  /**
   * �🗑️ DELETE ORGANIZATION Smaže Grafana organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "deleteOrganizationFallback")
  public void deleteOrganization(Long orgId) {
    log.info("🗑️ Deleting Grafana organization: {}", orgId);

    String url = grafanaUrl + "/api/orgs/" + orgId;
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.DELETE,
          entity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
          });

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
   * 🔍 LIST SERVICE ACCOUNTS Vrátí seznam service accounts v organizaci
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
   * � CREATE PROMETHEUS DATASOURCE Vytvoří Prometheus datasource v Grafana
   * organizaci
   * 
   * ✨ IDEMPOTENT: Pokud datasource už existuje, vrátí jeho ID
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createPrometheusDataSourceFallback")
  public CreateDataSourceResponse createPrometheusDataSource(Long orgId, String datasourceName) {
    log.info("📊 Creating Prometheus datasource: {} in orgId: {}", datasourceName, orgId);

    // STEP 1: Check if datasource already exists
    Optional<DataSourceInfo> existing = findDataSourceByName(orgId, datasourceName);
    if (existing.isPresent()) {
      DataSourceInfo ds = existing.get();
      log.info("✅ Prometheus datasource already exists: {} (id: {}, uid: {})", datasourceName,
          ds.getId(), ds.getUid());
      CreateDataSourceResponse response = new CreateDataSourceResponse();
      response.setId(ds.getId());
      response.setUid(ds.getUid());
      response.setName(ds.getName());
      response.setType(ds.getType());
      response.setUrl(ds.getUrl());
      response.setIsDefault(ds.getIsDefault());
      response.setMessage("Datasource already exists");
      return response;
    }

    // STEP 2: Create new datasource
    String url = grafanaUrl + "/api/datasources";
    HttpHeaders headers = createAuthHeaders();
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));
    headers.setContentType(MediaType.APPLICATION_JSON);

    CreateDataSourceRequest request = CreateDataSourceRequest.builder().name(datasourceName)
        .type("prometheus").url("http://prometheus:9090").access("proxy").isDefault(true)
        .jsonData(Map.of("httpMethod", "POST", "timeInterval", "30s")).build();

    HttpEntity<CreateDataSourceRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateDataSourceResponse> response = restTemplate.exchange(url,
          HttpMethod.POST, entity, CreateDataSourceResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        CreateDataSourceResponse body = response.getBody();
        log.info("✅ Prometheus datasource created: {} (id: {}, uid: {})", datasourceName,
            body.getId(), body.getUid());
        return body;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to create Prometheus datasource: {} in orgId: {}", datasourceName, orgId,
          e);
      throw new GrafanaApiException("Failed to create Prometheus datasource: " + e.getMessage(), e);
    }
  }

  /**
   * 🔍 FIND DATASOURCE BY NAME Najde datasource podle názvu v dané organizaci
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "findDataSourceByNameFallback")
  public Optional<DataSourceInfo> findDataSourceByName(Long orgId, String datasourceName) {
    log.debug("🔍 Searching for datasource: {} in orgId: {}", datasourceName, orgId);

    String url = grafanaUrl + "/api/datasources";
    HttpHeaders headers = createAuthHeaders();
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<List<DataSourceInfo>> response = restTemplate.exchange(url, HttpMethod.GET,
          entity, new ParameterizedTypeReference<List<DataSourceInfo>>() {
          });

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        Optional<DataSourceInfo> found = response.getBody().stream()
            .filter(ds -> ds.getName().equals(datasourceName)).findFirst();

        if (found.isPresent()) {
          log.debug("✅ Found datasource: {} (id: {})", datasourceName, found.get().getId());
        } else {
          log.debug("❌ Datasource not found: {}", datasourceName);
        }
        return found;
      } else {
        throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to search for datasource: {} in orgId: {}", datasourceName, orgId, e);
      throw new GrafanaApiException("Failed to search for datasource: " + e.getMessage(), e);
    }
  }

  /**
   * �🔐 CREATE AUTH HEADERS Vytvoří HTTP headers s Basic Auth
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
    log.error("⚠️ Circuit breaker: createServiceAccount fallback for {} in orgId: {}", name, orgId,
        e);
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
  private Optional<OrgInfo> findOrgByNameFallback(String orgName, Exception e) {
    log.error("⚠️ Circuit breaker: findOrgByName fallback for orgName: {}", orgName, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private Optional<CreateServiceAccountResponse> findServiceAccountByNameFallback(Long orgId,
      String name, Exception e) {
    log.error("⚠️ Circuit breaker: findServiceAccountByName fallback for {} in orgId: {}", name,
        orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private List<ServiceAccountInfo> listServiceAccountsFallback(Long orgId, Exception e) {
    log.error("⚠️ Circuit breaker: listServiceAccounts fallback for orgId: {}", orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private CreateDataSourceResponse createPrometheusDataSourceFallback(Long orgId,
      String datasourceName, Exception e) {
    log.error("⚠️ Circuit breaker: createPrometheusDataSource fallback for {} in orgId: {}",
        datasourceName, orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private Optional<DataSourceInfo> findDataSourceByNameFallback(Long orgId, String datasourceName,
      Exception e) {
    log.error("⚠️ Circuit breaker: findDataSourceByName fallback for {} in orgId: {}",
        datasourceName, orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }
}
