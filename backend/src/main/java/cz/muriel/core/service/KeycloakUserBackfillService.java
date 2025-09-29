package cz.muriel.core.service;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j @ConditionalOnProperty(value = "app.keycloak.backfill.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakUserBackfillService {

  private final TenantRepository tenantRepository;
  private final KeycloakEventProjectionService projectionService;
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.keycloak.admin.base-url:http://keycloak:8080}")
  private String keycloakBaseUrl;

  @Value("${app.keycloak.admin.realm:master}")
  private String adminRealm;

  @Value("${app.keycloak.admin.client-id:admin-cli}")
  private String adminClientId;

  @Value("${app.keycloak.admin.username:admin}")
  private String adminUsername;

  @Value("${app.keycloak.admin.password:admin}")
  private String adminPassword;

  @Scheduled(cron = "${app.keycloak.backfill.cron:0 25 3 * * *}")
  public void performDailyBackfill() {
    log.info("Starting daily Keycloak user backfill job");

    try {
      List<Tenant> tenants = tenantRepository.findAll();
      log.info("Found {} tenants for backfill", tenants.size());

      for (Tenant tenant : tenants) {
        try {
          backfillTenantUsers(tenant);
        } catch (Exception e) {
          log.error("Failed to backfill users for tenant: {}", tenant.getKey(), e);
        }
      }

      log.info("Daily backfill job completed successfully");

    } catch (Exception e) {
      log.error("Daily backfill job failed", e);
    }
  }

  private void backfillTenantUsers(Tenant tenant) {
    log.info("Backfilling users for tenant: {}", tenant.getKey());

    try {
      // Get admin access token
      String accessToken = getAdminAccessToken();
      if (accessToken == null) {
        log.error("Failed to get admin access token for tenant: {}", tenant.getKey());
        return;
      }

      // Fetch users from Keycloak for this realm (tenant)
      List<Map<String, Object>> users = fetchUsersFromKeycloak(tenant.getKey(), accessToken); // 🎯
                                                                                              // FIXED:
                                                                                              // Use
                                                                                              // getKey()
      log.debug("Fetched {} users from Keycloak for tenant: {}", users.size(), tenant.getKey());

      // Process each user through the same projection service
      for (Map<String, Object> keycloakUser : users) {
        try {
          KeycloakWebhookEventDto event = mapKeycloakUserToEvent(keycloakUser, tenant);
          projectionService.processEvent(event);
        } catch (Exception e) {
          log.warn("Failed to process user {} for tenant {}: {}", keycloakUser.get("username"),
              tenant.getKey(), e.getMessage());
        }
      }

      log.info("Successfully backfilled {} users for tenant: {}", users.size(), tenant.getKey());

    } catch (Exception e) {
      log.error("Failed to backfill users for tenant: {}", tenant.getKey(), e);
    }
  }

  private String getAdminAccessToken() {
    try {
      String tokenUrl = keycloakBaseUrl + "/realms/" + adminRealm
          + "/protocol/openid-connect/token";

      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/x-www-form-urlencoded");

      String requestBody = String.format("grant_type=password&client_id=%s&username=%s&password=%s",
          adminClientId, adminUsername, adminPassword);

      HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

      @SuppressWarnings("rawtypes")
      ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        Map<?, ?> responseBody = response.getBody();
        if (responseBody != null) {
          return (String) responseBody.get("access_token");
        }
      }

      log.error("Failed to get admin access token: {}", response.getStatusCode());
      return null;

    } catch (Exception e) {
      log.error("Failed to get admin access token", e);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> fetchUsersFromKeycloak(String realm, String accessToken) {
    try {
      String usersUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users?max=1000";

      HttpHeaders headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + accessToken);
      headers.add("Content-Type", "application/json");

      HttpEntity<?> request = new HttpEntity<>(headers);

      @SuppressWarnings("rawtypes")
      ResponseEntity<List> response = restTemplate.exchange(usersUrl, HttpMethod.GET, request,
          List.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        List<?> responseBody = response.getBody();
        if (responseBody != null) {
          return (List<Map<String, Object>>) responseBody;
        }
      }

      log.error("Failed to fetch users from Keycloak: {}", response.getStatusCode());
      return List.of();

    } catch (Exception e) {
      log.error("Failed to fetch users from Keycloak for realm: {}", realm, e);
      return List.of();
    }
  }

  @SuppressWarnings("unchecked")
  private KeycloakWebhookEventDto mapKeycloakUserToEvent(Map<String, Object> keycloakUser,
      Tenant tenant) {
    KeycloakWebhookEventDto event = new KeycloakWebhookEventDto();

    event.setEventType("USER_UPDATED"); // Backfill is always an update/upsert
    event.setTime(System.currentTimeMillis());
    event.setRealmId(tenant.getId().toString());
    event.setRealmName(tenant.getKey()); // 🎯 FIXED: Use getKey() - realm = key
    event.setTenantKey(tenant.getKey());

    // Map basic user fields
    event.setUserId((String) keycloakUser.get("id"));
    event.setUsername((String) keycloakUser.get("username"));
    event.setEmail((String) keycloakUser.get("email"));
    event.setFirstName((String) keycloakUser.get("firstName"));
    event.setLastName((String) keycloakUser.get("lastName"));
    event.setEnabled((Boolean) keycloakUser.getOrDefault("enabled", true));

    // Map attributes if present
    Object attributesObj = keycloakUser.get("attributes");
    if (attributesObj instanceof Map) {
      Map<String, List<String>> kcAttributes = (Map<String, List<String>>) attributesObj;
      Map<String, String> flatAttributes = new HashMap<>();

      kcAttributes.forEach((key, values) -> {
        if (values != null && !values.isEmpty()) {
          flatAttributes.put(key, String.join(";", values));
        }
      });

      event.setAttributes(flatAttributes);
    }

    // Note: For backfill, we're not fetching detailed roles/groups to keep it
    // simple
    // These will be updated through real-time webhooks when roles/groups change
    event.setRoles(Map.of("realm", List.of(), "clients", Map.of()));
    event.setGroups(List.of());

    return event;
  }
}
