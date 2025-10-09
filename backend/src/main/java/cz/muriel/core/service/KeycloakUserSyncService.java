package cz.muriel.core.service;

import cz.muriel.core.cdc.ChangeEvent;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.entity.UserChangeEventEntity;
import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.repository.UserDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 V5 Keycloak User Sync Service - CDC ONLY
 * 
 * ✅ CLEAN: Bulk synchronizace uživatelů přímo přes CDC ❌ ODSTRANĚNO:
 * KeycloakWebhookEventDto dependency
 */
@Service @RequiredArgsConstructor @Slf4j
public class KeycloakUserSyncService {

  private final KeycloakEventProjectionService projectionService;
  private final TenantService tenantService;
  private final UserDirectoryRepository userDirectoryRepository;

  /**
   * ✅ CDC Bulk synchronizace uživatelů z change events
   */
  @Transactional
  public void syncUsersFromEvents(String tenantKey, List<UserChangeEventEntity> events) {
    if (events.isEmpty()) {
      return;
    }

    log.info("🔄 Starting CDC bulk sync for tenant: {} with {} events", tenantKey, events.size());

    // Ověříme, že tenant existuje
    Optional<Tenant> tenant = tenantService.findTenantByKey(tenantKey);
    if (tenant.isEmpty()) {
      log.warn("🚫 Tenant not found in registry: {}", tenantKey);
      return;
    }

    try {
      // Seskupíme eventy podle user_id pro deduplicaci
      Map<UUID, List<UserChangeEventEntity>> eventsByUser = events.stream()
          .collect(Collectors.groupingBy(UserChangeEventEntity::getUserId));

      // Získáme unikátní user IDs
      Set<UUID> userIds = eventsByUser.keySet();

      log.debug("📋 Processing {} unique users from {} events", userIds.size(), events.size());

      // Zpracujeme každého uživatele přímo přes CDC
      for (Map.Entry<UUID, List<UserChangeEventEntity>> entry : eventsByUser.entrySet()) {
        UUID userId = entry.getKey();
        List<UserChangeEventEntity> userEvents = entry.getValue();

        try {
          syncSingleUserCdc(tenant.get(), userId, userEvents);
        } catch (Exception e) {
          log.error("❌ Failed to sync user {}: {}", userId, e.getMessage(), e);
          // Pokračujeme s dalšími uživateli
        }
      }

      log.info("✅ CDC bulk sync completed for tenant: {}", tenantKey);

    } catch (Exception e) {
      log.error("❌ Failed CDC bulk sync for tenant {}: {}", tenantKey, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * ✅ CDC synchronizace jednoho uživatele
   */
  private void syncSingleUserCdc(Tenant tenant, UUID userId,
      List<UserChangeEventEntity> userEvents) {
    // Seřadíme eventy podle času
    userEvents.sort(Comparator.comparing(UserChangeEventEntity::getCreatedAt));

    // Určíme finální operaci
    UserChangeEventEntity lastEvent = userEvents.get(userEvents.size() - 1);
    UserChangeEventEntity.OperationType finalOperation = lastEvent.getOperation();

    log.debug("🔄 Syncing user {} with {} events, final operation: {}", userId, userEvents.size(),
        finalOperation);

    try {
      // Zmapujeme operaci na event type
      String eventType = mapOperationToEventType(finalOperation);

      // Použijeme novou CDC metodu v projection service
      projectionService.processCdcEvent(eventType, userId.toString(), tenant.getKey(),
          tenant.getKey(), null);

      log.debug("✅ User {} synced via CDC", userId);

    } catch (Exception e) {
      log.error("❌ Failed to sync user {}: {}", userId, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * ✅ Mapování CDC operace na event type
   */
  private String mapOperationToEventType(UserChangeEventEntity.OperationType operation) {
    return switch (operation) {
    case INSERT -> "USER_CREATED";
    case UPDATE -> "USER_UPDATED";
    case DELETE -> "USER_DELETED";
    };
  }

  /**
   * Health check metoda pro monitoring
   */
  public Map<String, Object> getSyncStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("service", "KeycloakUserSyncService");
    stats.put("mode", "CDC");
    stats.put("status", "active");
    return stats;
  }

  /**
   * ✅ NOVÁ metoda: Synchronizuje všechny uživatele pro daný tenant pomocí CDC dat
   */
  @Transactional
  public void syncAllUsersForTenant(String tenantKey, List<ChangeEvent> cdcEvents) {
    log.info("Syncing {} users for tenant: {}", cdcEvents.size(), tenantKey);

    int synced = 0;
    int failed = 0;

    for (ChangeEvent event : cdcEvents) {
      try {
        projectionService.processCdcEvent(event);
        synced++;
      } catch (Exception e) {
        String userId = event.getFieldValue("keycloak_user_id");
        log.error("Failed to sync user {} for tenant {}: {}", userId, tenantKey, e.getMessage(), e);
        failed++;
      }
    }

    log.info("✅ Tenant user sync completed: tenant={}, synced={}, failed={}", tenantKey, synced,
        failed);
  }

  /**
   * ✅ NOVÁ metoda: Synchronizuje jednoho uživatele pomocí CDC eventu
   */
  @Transactional
  public void syncUser(ChangeEvent cdcEvent) {
    String userId = cdcEvent.getFieldValue("keycloak_user_id");
    String tenantKey = cdcEvent.getFieldValue("tenant_key");

    log.debug("Syncing single user: userId={}, tenant={}", userId, tenantKey);

    projectionService.processCdcEvent(cdcEvent);

    String username = cdcEvent.getFieldValue("username");
    log.info("✅ User synced: userId={}, username={}, tenant={}", userId, username, tenantKey);
  }

  /**
   * Získá uživatele z lokální projekce
   */
  public Optional<UserDirectoryEntity> getUserProjection(String keycloakUserId, String tenantKey) {
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
    return userDirectoryRepository.findByTenantIdAndKeycloakUserId(tenantId, keycloakUserId);
  }

  /**
   * Získá všechny uživatele pro tenant z lokální projekce
   */
  public List<UserDirectoryEntity> getAllUsersForTenant(String tenantKey) {
    UUID tenantId = tenantService.getTenantIdFromKey(tenantKey);
    return userDirectoryRepository.findByTenantId(tenantId);
  }
}
