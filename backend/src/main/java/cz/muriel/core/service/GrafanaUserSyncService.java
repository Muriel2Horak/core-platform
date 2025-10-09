package cz.muriel.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 Grafana User Sync Service Automaticky synchronizuje uživatele s monitoring
 * rolemi do Grafany
 * 
 * Klíčové features: - ✅ Zpracovává composite roles (např. CORE_ROLE_ADMIN
 * obsahuje CORE_ROLE_MONITORING) - ✅ Kontroluje existenci uživatele v Grafaně
 * před deaktivací - ✅ Automaticky vytváří/aktualizuje/deaktivuje Grafana účty -
 * ✅ Manuální sync všech monitoring uživatelů
 * 
 * Aktivace při CDC eventech: - USER_ROLE_ASSIGNED → kontrola monitoring rolí →
 * create/update Grafana user - USER_ROLE_REMOVED → kontrola monitoring rolí →
 * deactivate Grafana user (pokud existuje)
 */
@Slf4j @Service @RequiredArgsConstructor
public class GrafanaUserSyncService {

    private final RestTemplate restTemplate;
    private final Keycloak keycloakAdminClient;

    @Value("${grafana.url:http://grafana:3000}")
    private String grafanaUrl;

    @Value("${grafana.admin.user:admin}")
    private String grafanaAdminUser;

    @Value("${grafana.admin.password:admin}")
    private String grafanaAdminPassword;

    // 🎭 Monitoring role names (including composite role members)
    private static final String CORE_ROLE_MONITORING = "CORE_ROLE_MONITORING";
    private static final String CORE_ROLE_TENANT_MONITORING = "CORE_ROLE_TENANT_MONITORING";
    private static final String CORE_ROLE_ADMIN = "CORE_ROLE_ADMIN";
    private static final String CORE_TENANT_ADMIN = "CORE_TENANT_ADMIN";

    // 📊 Statistics
    private int totalSyncedUsers = 0;
    private int totalFailedSyncs = 0;
    private long lastSyncTimestamp = 0;

    /**
     * 🔄 Zpracuje USER_ROLE_ASSIGNED/REMOVED eventy
     * 
     * DŮLEŽITÉ: Zpracovává i composite roles! - Pokud uživatel má CORE_ROLE_ADMIN,
     * automaticky má i CORE_ROLE_MONITORING - Pokud uživatel má CORE_TENANT_ADMIN,
     * automaticky má i CORE_ROLE_TENANT_MONITORING
     * 
     * Workflow: 1. Načti VŠECHNY role uživatele z Keycloak (včetně composite) 2.
     * Zkontroluj, jestli JAKÁKOLIV role je monitoring role 3a. Pokud ANO →
     * Vytvoř/aktualizuj Grafana uživatele 3b. Pokud NE → Zkontroluj existenci v
     * Grafaně a deaktivuj (pokud existuje)
     */
    public void handleUserRoleChange(Map<String, Object> event) {
        try {
            String userId = (String) event.get("entity_id");
            String realmId = (String) event.get("realm_id");
            String eventType = (String) event.get("event_type");

            log.info("🔄 Processing role change: {} for user: {} in realm: {}", eventType, userId,
                    realmId);

            // ⚠️ MULTI-TENANCY LIMITATION: Grafana OAuth/JWT jsou fixně na admin realm
            // → Synchronizujeme JEN uživatele z admin realmu
            // → Tenant monitoring users musí být v admin realmu (s tenant_key attributem)
            if (!"admin".equals(realmId)) {
                log.debug(
                        "⏭️ Skipping Grafana sync for non-admin realm: {} (user: {}). Grafana only supports admin realm authentication.",
                        realmId, userId);
                return;
            }

            // STEP 1: Načíst uživatele z Keycloak
            UserRepresentation user = getUserFromKeycloak(userId, realmId);
            if (user == null) {
                log.warn("⚠️ User not found in Keycloak: userId={}, realm={}", userId, realmId);
                return;
            }

            // STEP 2: Načíst VŠECHNY role uživatele (včetně composite members)
            Set<String> allUserRoles = getEffectiveUserRoles(userId, realmId);
            log.debug("📋 User {} effective roles: {}", user.getUsername(), allUserRoles);

            // STEP 3: Zkontrolovat monitoring přístup
            boolean hasMonitoringAccess = hasAnyMonitoringRole(allUserRoles);

            if (hasMonitoringAccess) {
                // STEP 3a: Má monitoring přístup → sync do Grafany
                String grafanaRole = determineGrafanaRole(allUserRoles);
                createOrUpdateGrafanaUser(user.getUsername(), user.getEmail(), getFullName(user),
                        grafanaRole);
            } else {
                // STEP 3b: NEMÁ monitoring přístup → deaktivuj v Grafaně (pokud existuje)
                deactivateGrafanaUserIfExists(user.getUsername());
            }

        } catch (Exception e) {
            log.error("❌ Failed to sync Grafana user for event: {}", event, e);
        }
    }

    /**
     * 👤 Načte uživatele z Keycloak
     */
    private UserRepresentation getUserFromKeycloak(String userId, String realmId) {
        try {
            return keycloakAdminClient.realm(realmId).users().get(userId).toRepresentation();
        } catch (Exception e) {
            log.error("❌ Failed to get user from Keycloak: userId={}, realm={}", userId, realmId,
                    e);
            return null;
        }
    }

    /**
     * 🎭 Načte VŠECHNY efektivní role uživatele (včetně composite role members)
     * 
     * Klíčová metoda pro composite role handling! - Vrací i role z composite rolí
     * (např. CORE_ROLE_ADMIN obsahuje CORE_ROLE_MONITORING)
     */
    private Set<String> getEffectiveUserRoles(String userId, String realmId) {
        try {
            List<RoleRepresentation> effectiveRoles = keycloakAdminClient.realm(realmId).users()
                    .get(userId).roles().realmLevel().listEffective(); // ← Toto vrací i composite
                                                                       // role members!

            return effectiveRoles.stream().map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("❌ Failed to get effective roles from Keycloak: userId={}, realm={}", userId,
                    realmId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 📝 Vrátí full name uživatele (firstName + lastName)
     */
    private String getFullName(UserRepresentation user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }

    /**
     * 🔍 Zkontroluje, jestli uživatel má JAKOUKOLIV monitoring roli (včetně
     * composite role members)
     * 
     * Monitoring roles: - CORE_ROLE_ADMIN (composite → obsahuje
     * CORE_ROLE_MONITORING) - CORE_ROLE_MONITORING (globální monitoring) -
     * CORE_TENANT_ADMIN (composite → obsahuje CORE_ROLE_TENANT_MONITORING) -
     * CORE_ROLE_TENANT_MONITORING (tenant monitoring)
     */
    private boolean hasAnyMonitoringRole(Set<String> allRoles) {
        return allRoles.contains(CORE_ROLE_ADMIN) || allRoles.contains(CORE_ROLE_MONITORING)
                || allRoles.contains(CORE_TENANT_ADMIN)
                || allRoles.contains(CORE_ROLE_TENANT_MONITORING);
    }

    /**
     * 🎭 Určí Grafana roli na základě Keycloak rolí Priority (sestupně): 1.
     * CORE_ROLE_ADMIN → Admin (plný přístup) 2. CORE_ROLE_MONITORING → Editor (může
     * editovat) 3. CORE_TENANT_ADMIN → Editor (composite obsahující monitoring) 4.
     * CORE_ROLE_TENANT_MONITORING → Viewer (pouze čtení)
     */
    private String determineGrafanaRole(Set<String> keycloakRoles) {
        if (keycloakRoles.contains(CORE_ROLE_ADMIN)) {
            return "Admin";
        } else if (keycloakRoles.contains(CORE_ROLE_MONITORING)) {
            return "Editor";
        } else if (keycloakRoles.contains(CORE_TENANT_ADMIN)) {
            return "Editor"; // CORE_TENANT_ADMIN je composite obsahující monitoring
        } else if (keycloakRoles.contains(CORE_ROLE_TENANT_MONITORING)) {
            return "Viewer";
        }
        return "Viewer"; // fallback
    }

    /**
     * 🔍 Zkontroluje, jestli uživatel existuje v Grafaně
     */
    @SuppressWarnings("rawtypes")
    private boolean userExistsInGrafana(String username) {
        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    grafanaUrl + "/api/users/lookup?loginOrEmail=" + username, HttpMethod.GET,
                    new HttpEntity<>(createGrafanaHeaders()), Map[].class);

            if (response.getBody() == null) {
                return false;
            }
            return response.getBody().length > 0;

        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.warn("⚠️ Could not check Grafana user existence: {}", username, e);
            return false;
        }
    }

    /**
     * ❌ Deaktivuje Grafana uživatele POUZE pokud existuje
     */
    @SuppressWarnings("rawtypes")
    private void deactivateGrafanaUserIfExists(String username) {
        try {
            // STEP 1: Zkontroluj existenci
            if (!userExistsInGrafana(username)) {
                log.debug("ℹ️ User {} doesn't exist in Grafana - skipping deactivation", username);
                return;
            }

            // STEP 2: Najdi user ID
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    grafanaUrl + "/api/users/lookup?loginOrEmail=" + username, HttpMethod.GET,
                    new HttpEntity<>(createGrafanaHeaders()), Map[].class);

            if (response.getBody() == null || response.getBody().length == 0) {
                log.debug("ℹ️ User {} not found in Grafana", username);
                return;
            }

            Integer userId = (Integer) response.getBody()[0].get("id");

            // STEP 3: Smaž uživatele
            log.info("🗑️ Deleting Grafana user: {} (id={})", username, userId);
            restTemplate.exchange(grafanaUrl + "/api/admin/users/" + userId, HttpMethod.DELETE,
                    new HttpEntity<>(createGrafanaHeaders()), String.class);

            log.info("✅ Grafana user deleted: {}", username);

        } catch (Exception e) {
            log.error("❌ Failed to deactivate Grafana user: {}", username, e);
        }
    }

    /**
     * ✅ Vytvoří nebo aktualizuje Grafana uživatele
     */
    @SuppressWarnings("rawtypes")
    private void createOrUpdateGrafanaUser(String username, String email, String name,
            String grafanaRole) {
        try {
            log.info("✅ Creating/updating Grafana user: {} with role: {}", username, grafanaRole);

            // Připrav user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("login", username);
            userData.put("email", email != null ? email : username + "@local");
            userData.put("name", name);
            userData.put("password", UUID.randomUUID().toString()); // Random password (JWT auth)

            // STEP 1: Zkus vytvořit nového uživatele
            try {
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData,
                        createGrafanaHeaders());
                ResponseEntity<Map> createResponse = restTemplate.exchange(
                        grafanaUrl + "/api/admin/users", HttpMethod.POST, request, Map.class);

                if (createResponse.getBody() == null) {
                    log.error("❌ Empty response from Grafana when creating user: {}", username);
                    return;
                }

                Integer userId = (Integer) createResponse.getBody().get("id");
                log.info("✅ Created new Grafana user: {} (id={})", username, userId);

                // STEP 2: Nastav roli
                updateGrafanaUserRole(userId, grafanaRole);

            } catch (HttpClientErrorException.Conflict e) {
                // Uživatel už existuje → aktualizuj roli
                log.debug("ℹ️ User {} already exists in Grafana - updating role", username);

                // Najdi user ID
                ResponseEntity<Map[]> lookupResponse = restTemplate.exchange(
                        grafanaUrl + "/api/users/lookup?loginOrEmail=" + username, HttpMethod.GET,
                        new HttpEntity<>(createGrafanaHeaders()), Map[].class);

                if (lookupResponse.getBody() != null && lookupResponse.getBody().length > 0) {
                    Integer userId = (Integer) lookupResponse.getBody()[0].get("id");
                    updateGrafanaUserRole(userId, grafanaRole);
                }
            }

        } catch (Exception e) {
            log.error("❌ Failed to create/update Grafana user: {}", username, e);
        }
    }

    /**
     * 🔄 Aktualizuje roli uživatele v Grafana
     */
    private void updateGrafanaUserRole(Integer userId, String role) {
        try {
            Map<String, Object> roleData = new HashMap<>();
            roleData.put("role", role);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(roleData,
                    createGrafanaHeaders());
            restTemplate.exchange(grafanaUrl + "/api/orgs/1/users/" + userId, HttpMethod.PATCH,
                    request, String.class);

            log.info("✅ Updated Grafana user role: userId={}, role={}", userId, role);

        } catch (Exception e) {
            log.error("❌ Failed to update Grafana user role: userId={}, role={}", userId, role, e);
        }
    }

    /**
     * 🔐 Vytvoří HTTP headers pro Grafana Admin API
     */
    private HttpHeaders createGrafanaHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Basic auth pro Grafana Admin API
        String auth = grafanaAdminUser + ":" + grafanaAdminPassword;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }

    /**
     * 🔄 Synchronizuje VŠECHNY uživatele s MONITORING rolemi z Keycloak do Grafany
     * 
     * ⚠️ MULTI-TENANCY: Defaultně synchro jen admin realm (Grafana limitation)
     * 
     * Použití: - Při prvním spuštění systému - Manuální re-sync přes admin endpoint
     * - Oprava stavu po výpadku
     * 
     * @param realmId - Keycloak realm ID (default: "admin")
     */
    public Map<String, Object> syncAllMonitoringUsers(String realmId) {
        log.info("🔄 Starting full Grafana sync for realm: {}", realmId);

        // ⚠️ Warn if syncing non-admin realm (will create users, but they won't be able to login)
        if (!"admin".equals(realmId)) {
            log.warn(
                    "⚠️ Syncing non-admin realm '{}' - users will be created in Grafana but cannot login via OAuth/JWT (Grafana is configured for admin realm only)",
                    realmId);
        }

        int syncedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // STEP 1: Načti VŠECHNY uživatele z realmu
            List<UserRepresentation> allUsers = keycloakAdminClient.realm(realmId).users().list();
            log.info("📋 Found {} users in realm {}", allUsers.size(), realmId);

            // STEP 2: Pro každého uživatele zkontroluj monitoring role
            for (UserRepresentation user : allUsers) {
                try {
                    Set<String> effectiveRoles = getEffectiveUserRoles(user.getId(), realmId);
                    boolean hasMonitoringAccess = hasAnyMonitoringRole(effectiveRoles);

                    if (hasMonitoringAccess) {
                        String grafanaRole = determineGrafanaRole(effectiveRoles);
                        createOrUpdateGrafanaUser(user.getUsername(), user.getEmail(),
                                getFullName(user), grafanaRole);
                        syncedCount++;
                        log.debug("✅ Synced user: {} with role: {}", user.getUsername(),
                                grafanaRole);
                    } else {
                        skippedCount++;
                        log.debug("⏭️ Skipped user: {} (no monitoring roles)", user.getUsername());
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Failed to sync user: " + user.getUsername() + " - "
                            + e.getMessage();
                    errors.add(errorMsg);
                    log.error("❌ {}", errorMsg, e);
                }
            }

            // STEP 3: Update statistics
            this.totalSyncedUsers = syncedCount;
            this.totalFailedSyncs = errorCount;
            this.lastSyncTimestamp = System.currentTimeMillis();

            log.info("✅ Grafana sync completed: synced={}, skipped={}, errors={}, realm={}",
                    syncedCount, skippedCount, errorCount, realmId);

            return Map.of("success", true, "realm", realmId, "syncedUsers", syncedCount,
                    "skippedUsers", skippedCount, "errors", errorCount, "errorMessages", errors,
                    "timestamp", this.lastSyncTimestamp);

        } catch (Exception e) {
            log.error("❌ Failed to sync Grafana users for realm: {}", realmId, e);
            return Map.of("success", false, "error", e.getMessage(), "realm", realmId);
        }
    }

    /**
     * 📊 Vrátí status synchronizace
     */
    public Map<String, Object> getSyncStatus() {
        return Map.of("totalSyncedUsers", totalSyncedUsers, "totalFailedSyncs", totalFailedSyncs,
                "lastSyncTimestamp", lastSyncTimestamp, "lastSyncDate",
                lastSyncTimestamp > 0 ? new Date(lastSyncTimestamp).toString() : "Never",
                "grafanaUrl", grafanaUrl);
    }
}
