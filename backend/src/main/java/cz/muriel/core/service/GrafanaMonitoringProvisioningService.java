package cz.muriel.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 📊 GRAFANA MONITORING AUTO-PROVISIONING SERVICE
 * 
 * Automaticky provisionuje Axiom Monitoring dashboardy při vytvoření tenantu:
 * - Vytvoří Grafana organizaci pro tenanta
 * - Provisionuje všechny Axiom dashboardy (7 dashboardů)
 * - Nastaví correct org context pro tenant isolation
 * 
 * Workflow:
 * 1. TenantManagementController.createTenant() → volá tento service
 * 2. Service vytvoří Grafana org (nebo najde existující)
 * 3. Service import všechny dashboardy do tenant org
 * 4. Tenant admin má okamžitě přístup k monitoring
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GrafanaMonitoringProvisioningService {

  private final RestTemplate restTemplate;

  @Value("${GRAFANA_PUBLIC_URL:http://localhost:3000}")
  private String grafanaUrl;

  @Value("${GRAFANA_ADMIN_USER:admin}")
  private String grafanaAdminUser;

  @Value("${GRAFANA_ADMIN_PASSWORD:admin}")
  private String grafanaAdminPassword;

  private static final String[] AXIOM_DASHBOARD_UIDS = {
      "axiom_sys_overview",     // System Overview (flagship)
      "axiom_adv_runtime",      // JVM Runtime
      "axiom_adv_db",           // Database Operations
      "axiom_adv_redis",        // Redis Cache
      "axiom_kafka_lag",        // Kafka Consumer Lag
      "axiom_security",         // Security & Auth
      "axiom_audit"             // Audit & Governance
  };

  /**
   * 🚀 MAIN ENTRY POINT: Auto-provision monitoring dashboards for new tenant
   * 
   * Called by TenantManagementController after tenant creation
   */
  public void provisionMonitoringForTenant(String tenantKey, String tenantDisplayName) {
    try {
      log.info("📊 Starting Grafana monitoring provisioning for tenant: {}", tenantKey);

      // Step 1: Create or find Grafana organization
      Long orgId = createOrFindGrafanaOrg(tenantKey, tenantDisplayName);

      if (orgId == null) {
        log.error("❌ Failed to create/find Grafana org for tenant: {}", tenantKey);
        return;
      }

      log.info("✅ Grafana org created/found: orgId={} for tenant={}", orgId, tenantKey);

      // Step 2: Import all Axiom dashboards to tenant org
      for (String dashboardUid : AXIOM_DASHBOARD_UIDS) {
        try {
          importDashboardToOrg(dashboardUid, orgId, tenantKey);
          log.info("  ✅ Dashboard {} imported to org {}", dashboardUid, orgId);
        } catch (Exception e) {
          log.warn("  ⚠️ Failed to import dashboard {} to org {}: {}", 
              dashboardUid, orgId, e.getMessage());
          // Continue with other dashboards
        }
      }

      log.info("🎉 Monitoring provisioning completed for tenant: {} (orgId={})", 
          tenantKey, orgId);

    } catch (Exception e) {
      log.error("❌ Failed to provision monitoring for tenant {}: {}", 
          tenantKey, e.getMessage(), e);
      // Don't throw - tenant creation should succeed even if monitoring provisioning fails
    }
  }

  /**
   * 🏢 Create Grafana organization for tenant (or find existing)
   * 
   * Returns orgId or null if failed
   */
  private Long createOrFindGrafanaOrg(String tenantKey, String tenantDisplayName) {
    try {
      String orgName = "Tenant: " + tenantKey;

      // Check if org already exists
      String searchUrl = grafanaUrl + "/api/orgs/name/" + orgName;
      
      try {
        ResponseEntity<Map> searchResponse = restTemplate.exchange(
            searchUrl,
            HttpMethod.GET,
            createAuthHeaders(),
            Map.class
        );

        if (searchResponse.getStatusCode() == HttpStatus.OK) {
          Map<String, Object> orgData = searchResponse.getBody();
          if (orgData != null && orgData.containsKey("id")) {
            Long orgId = ((Number) orgData.get("id")).longValue();
            log.info("📍 Found existing Grafana org: {} (id={})", orgName, orgId);
            return orgId;
          }
        }
      } catch (Exception e) {
        // Org doesn't exist, continue to create
        log.debug("Org {} not found, will create new", orgName);
      }

      // Create new organization
      String createUrl = grafanaUrl + "/api/orgs";
      Map<String, String> createRequest = new HashMap<>();
      createRequest.put("name", orgName);

      ResponseEntity<Map> createResponse = restTemplate.exchange(
          createUrl,
          HttpMethod.POST,
          createAuthHeaders(createRequest),
          Map.class
      );

      if (createResponse.getStatusCode() == HttpStatus.OK) {
        Map<String, Object> responseBody = createResponse.getBody();
        if (responseBody != null && responseBody.containsKey("orgId")) {
          Long orgId = ((Number) responseBody.get("orgId")).longValue();
          log.info("🆕 Created new Grafana org: {} (id={})", orgName, orgId);
          return orgId;
        }
      }

      log.error("❌ Failed to create Grafana org: {}", createResponse);
      return null;

    } catch (Exception e) {
      log.error("❌ Error creating/finding Grafana org: {}", e.getMessage(), e);
      return null;
    }
  }

  /**
   * 📊 Import dashboard to specific organization
   * 
   * Uses Grafana Dashboard API to copy dashboard from default org to tenant org
   */
  private void importDashboardToOrg(String dashboardUid, Long orgId, String tenantKey) 
      throws Exception {
    
    // Step 1: Get dashboard JSON from default org (orgId=1)
    String getDashboardUrl = grafanaUrl + "/api/dashboards/uid/" + dashboardUid;
    
    ResponseEntity<Map> getDashboardResponse = restTemplate.exchange(
        getDashboardUrl,
        HttpMethod.GET,
        createAuthHeaders(),
        Map.class
    );

    if (getDashboardResponse.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Failed to get dashboard " + dashboardUid);
    }

    Map<String, Object> dashboardData = getDashboardResponse.getBody();
    if (dashboardData == null || !dashboardData.containsKey("dashboard")) {
      throw new RuntimeException("Invalid dashboard data for " + dashboardUid);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> dashboard = (Map<String, Object>) dashboardData.get("dashboard");

    // Step 2: Prepare dashboard for import to tenant org
    dashboard.remove("id"); // Remove ID to allow import
    dashboard.put("uid", dashboardUid); // Keep UID for consistency
    
    // Update tenant variable default value
    if (dashboard.containsKey("templating")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> templating = (Map<String, Object>) dashboard.get("templating");
      
      if (templating.containsKey("list")) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> variables = 
            (java.util.List<Map<String, Object>>) templating.get("list");
        
        for (Map<String, Object> variable : variables) {
          if ("tenant".equals(variable.get("name"))) {
            // Set tenant variable default to current tenant
            @SuppressWarnings("unchecked")
            Map<String, Object> current = (Map<String, Object>) variable.get("current");
            if (current != null) {
              current.put("value", tenantKey);
              current.put("text", tenantKey);
            }
            break;
          }
        }
      }
    }

    // Step 3: Import dashboard to tenant org
    Map<String, Object> importRequest = new HashMap<>();
    importRequest.put("dashboard", dashboard);
    importRequest.put("overwrite", true);
    importRequest.put("folderUid", ""); // Root folder

    String importUrl = grafanaUrl + "/api/dashboards/db";
    
    // Create headers with org context
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBasicAuth(grafanaAdminUser, grafanaAdminPassword);
    headers.set("X-Grafana-Org-Id", String.valueOf(orgId)); // Target specific org
    
    HttpEntity<Map<String, Object>> importEntity = new HttpEntity<>(importRequest, headers);

    ResponseEntity<Map> importResponse = restTemplate.exchange(
        importUrl,
        HttpMethod.POST,
        importEntity,
        Map.class
    );

    if (importResponse.getStatusCode() != HttpStatus.OK) {
      throw new RuntimeException("Failed to import dashboard " + dashboardUid + 
          " to org " + orgId);
    }

    log.debug("📊 Dashboard {} imported to org {} successfully", dashboardUid, orgId);
  }

  /**
   * 🔐 Create HTTP headers with basic auth
   */
  private HttpEntity<?> createAuthHeaders() {
    return createAuthHeaders(null);
  }

  /**
   * 🔐 Create HTTP headers with basic auth and optional body
   */
  private HttpEntity<?> createAuthHeaders(Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBasicAuth(grafanaAdminUser, grafanaAdminPassword);
    
    if (body != null) {
      return new HttpEntity<>(body, headers);
    }
    
    return new HttpEntity<>(headers);
  }

  /**
   * 📋 Get list of provisioned dashboard UIDs for tenant
   * 
   * Used for verification and testing
   */
  public String[] getProvisionedDashboards() {
    return AXIOM_DASHBOARD_UIDS;
  }
}
