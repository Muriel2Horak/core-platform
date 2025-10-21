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

  /**
   * 👤 LIST USERS IN ORGANIZATION Gets all users in a specific organization
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "listOrgUsersFallback")
  public List<OrgUserInfo> listOrgUsers(Long orgId) {
    log.debug("👥 Listing users in Grafana org: {}", orgId);

    String url = grafanaUrl + "/api/orgs/" + orgId + "/users";
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<List<OrgUserInfo>> response = restTemplate.exchange(url, HttpMethod.GET,
          entity, new ParameterizedTypeReference<List<OrgUserInfo>>() {
          });

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.debug("✅ Found {} users in org {}", response.getBody().size(), orgId);
        return response.getBody();
      }

      log.warn("⚠️  Unexpected response from Grafana: {}", response.getStatusCode());
      return List.of();
    } catch (Exception e) {
      log.error("❌ Failed to list users in org {}: {}", orgId, e.getMessage());
      return List.of();
    }
  }

  /**
   * ➕ ADD USER TO ORGANIZATION Adds an existing user to a specific organization
   * with given role
   * 
   * ✨ IDEMPOTENT: If user is already member, returns success
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "addUserToOrgFallback")
  public AddOrgUserResponse addUserToOrg(Long orgId, String loginOrEmail, String role) {
    log.info("➕ Adding user {} to Grafana org {} with role {}", loginOrEmail, orgId, role);

    // Check if user is already a member
    try {
      List<OrgUserInfo> existingUsers = listOrgUsers(orgId);
      boolean alreadyMember = existingUsers.stream().anyMatch(
          user -> user.getLogin().equals(loginOrEmail) || user.getEmail().equals(loginOrEmail));

      if (alreadyMember) {
        log.info("ℹ️  User {} is already member of org {}", loginOrEmail, orgId);
        AddOrgUserResponse response = new AddOrgUserResponse();
        response.setMessage("User is already member of organization");
        return response;
      }
    } catch (Exception e) {
      log.debug("Could not check existing membership (will try to add): {}", e.getMessage());
    }

    String url = grafanaUrl + "/api/orgs/" + orgId + "/users";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    AddOrgUserRequest request = new AddOrgUserRequest(loginOrEmail, role);
    HttpEntity<AddOrgUserRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<AddOrgUserResponse> response = restTemplate.exchange(url, HttpMethod.POST,
          entity, AddOrgUserResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.info("✅ User {} added to org {} with role {}", loginOrEmail, orgId, role);
        return response.getBody();
      }

      log.warn("⚠️  Unexpected response from Grafana: {}", response.getStatusCode());
      throw new GrafanaApiException("Failed to add user to org: " + response.getStatusCode());
    } catch (Exception e) {
      log.error("❌ Failed to add user {} to org {}: {}", loginOrEmail, orgId, e.getMessage());
      throw new GrafanaApiException("Failed to add user to organization", e);
    }
  }

  @SuppressWarnings("unused")
  private List<OrgUserInfo> listOrgUsersFallback(Long orgId, Exception e) {
    log.error("⚠️ Circuit breaker: listOrgUsers fallback for orgId: {}", orgId, e);
    return List.of();
  }

  @SuppressWarnings("unused")
  private AddOrgUserResponse addUserToOrgFallback(Long orgId, String loginOrEmail, String role,
      Exception e) {
    log.error("⚠️ Circuit breaker: addUserToOrg fallback for user {} in orgId: {}", loginOrEmail,
        orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  // ==================== USER MANAGEMENT METHODS ====================

  /**
   * 🔍 LOOKUP USER BY EMAIL
   * GET /api/users/lookup?loginOrEmail={email}
   * Returns user info or throws 404 if not found
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "lookupUserFallback")
  public Optional<UserLookupResponse> lookupUser(String loginOrEmail) {
    log.debug("🔍 Looking up Grafana user: {}", loginOrEmail);

    String url = grafanaUrl + "/api/users/lookup?loginOrEmail=" + loginOrEmail;
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<UserLookupResponse> response = restTemplate.exchange(
          url, HttpMethod.GET, entity, UserLookupResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        UserLookupResponse user = response.getBody();
        log.debug("✅ Found user: {} (id: {})", loginOrEmail, user.getId());
        return Optional.of(user);
      }

      return Optional.empty();
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
      log.debug("❌ User not found: {}", loginOrEmail);
      return Optional.empty();
    } catch (Exception e) {
      log.error("❌ Failed to lookup user {}: {}", loginOrEmail, e.getMessage());
      throw new GrafanaApiException("Failed to lookup user", e);
    }
  }

  /**
   * ➕ CREATE USER
   * POST /api/admin/users
   * Creates new Grafana user with email, name, and random password
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "createUserFallback")
  public CreateUserResponse createUser(String email, String name) {
    log.info("➕ Creating Grafana user: {} ({})", email, name);

    String url = grafanaUrl + "/api/admin/users";
    HttpHeaders headers = createAuthHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Generate random password (user will use SSO anyway)
    String randomPassword = java.util.UUID.randomUUID().toString();
    
    CreateUserRequest request = new CreateUserRequest(email, name, email, randomPassword);
    HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

    try {
      ResponseEntity<CreateUserResponse> response = restTemplate.exchange(
          url, HttpMethod.POST, entity, CreateUserResponse.class);

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        CreateUserResponse body = response.getBody();
        log.info("✅ User created: {} (id: {})", email, body.getId());
        return body;
      }

      throw new GrafanaApiException("Unexpected response: " + response.getStatusCode());
    } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
      // User already exists - lookup and return
      log.warn("⚠️  User {} already exists (409), looking up existing user", email);
      Optional<UserLookupResponse> existing = lookupUser(email);
      if (existing.isPresent()) {
        CreateUserResponse response = new CreateUserResponse();
        response.setId(existing.get().getId());
        response.setMessage("User already exists");
        return response;
      }
      throw new GrafanaApiException("User exists but lookup failed", e);
    } catch (Exception e) {
      log.error("❌ Failed to create user {}: {}", email, e.getMessage());
      throw new GrafanaApiException("Failed to create user", e);
    }
  }

  /**
   * 📋 GET USER ORGANIZATIONS
   * GET /api/users/{userId}/orgs
   * Returns list of organizations user belongs to
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "getUserOrgsFallback")
  public List<UserOrgInfo> getUserOrgs(Long userId) {
    log.debug("📋 Getting organizations for user: {}", userId);

    String url = grafanaUrl + "/api/users/" + userId + "/orgs";
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<List<UserOrgInfo>> response = restTemplate.exchange(
          url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<UserOrgInfo>>() {});

      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        log.debug("✅ User {} is member of {} orgs", userId, response.getBody().size());
        return response.getBody();
      }

      return List.of();
    } catch (Exception e) {
      log.error("❌ Failed to get orgs for user {}: {}", userId, e.getMessage());
      return List.of();
    }
  }

  /**
   * 🔄 SET USER ACTIVE ORGANIZATION
   * POST /api/users/{userId}/using/{orgId}
   * Sets the active/default organization for user's UI session
   * 
   * ✨ IDEMPOTENT: Can be called multiple times safely
   */
  @CircuitBreaker(name = "grafana", fallbackMethod = "setUserActiveOrgFallback")
  public void setUserActiveOrg(Long userId, Long orgId) {
    log.info("🔄 Setting active org {} for user {}", orgId, userId);

    String url = grafanaUrl + "/api/users/" + userId + "/using/" + orgId;
    HttpHeaders headers = createAuthHeaders();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<SwitchOrgResponse> response = restTemplate.exchange(
          url, HttpMethod.POST, entity, SwitchOrgResponse.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        log.info("✅ Active org set to {} for user {}", orgId, userId);
      } else {
        log.warn("⚠️  Unexpected response: {}", response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("❌ Failed to set active org {} for user {}: {}", orgId, userId, e.getMessage());
      throw new GrafanaApiException("Failed to set active org", e);
    }
  }

  // ==================== FALLBACK METHODS FOR USER MANAGEMENT ====================

  @SuppressWarnings("unused")
  private Optional<UserLookupResponse> lookupUserFallback(String loginOrEmail, Exception e) {
    log.error("⚠️ Circuit breaker: lookupUser fallback for {}", loginOrEmail, e);
    return Optional.empty();
  }

  @SuppressWarnings("unused")
  private CreateUserResponse createUserFallback(String email, String name, Exception e) {
    log.error("⚠️ Circuit breaker: createUser fallback for {}", email, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }

  @SuppressWarnings("unused")
  private List<UserOrgInfo> getUserOrgsFallback(Long userId, Exception e) {
    log.error("⚠️ Circuit breaker: getUserOrgs fallback for user {}", userId, e);
    return List.of();
  }

  @SuppressWarnings("unused")
  private void setUserActiveOrgFallback(Long userId, Long orgId, Exception e) {
    log.error("⚠️ Circuit breaker: setUserActiveOrg fallback for user {} org {}", userId, orgId, e);
    throw new GrafanaApiException("Grafana service unavailable (circuit open)", e);
  }
}
