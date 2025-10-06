package cz.muriel.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 🔄 V5 Change Event Processor - POLLING ONLY (CDC)
 * 
 * Polluje change_events tabulku v Keycloak DB každých 10 sekund.
 * 
 * ✅ CLEAN: Používá čistě CDC data z databáze ❌ ODSTRANĚNO: Webhook SPI
 * dependency ❌ ODSTRANĚNO: ChangeEventEnrichmentService (nepotřebné - CDC má
 * všechna data)
 */
@Service @RequiredArgsConstructor @Slf4j
public class ChangeEventProcessor {

  @Qualifier("keycloakDataSource")
  private final DataSource keycloakDataSource;

  // ✅ Direct processing - no enrichment needed, CDC has full data
  private final KeycloakEventProjectionService eventProjectionService;

  @Value("${app.change-events.polling-interval-ms:10000}")
  private long pollingIntervalMs;

  @Value("${app.change-events.batch-size:100}")
  private int batchSize;

  @Value("${app.change-events.cleanup-after-days:7}")
  private int cleanupAfterDays;

  // Statistics
  private volatile long totalEventsProcessed = 0;
  private volatile long lastProcessedEventId = 0;
  private volatile LocalDateTime lastPollTime = null;

  /**
   * 🔄 Main polling job - zpracovává nepřečtené eventy z Keycloak DB
   */
  @Scheduled(fixedDelayString = "${app.change-events.polling-interval-ms:10000}") @Transactional
  public void pollAndProcessEvents() {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    try {
      lastPollTime = LocalDateTime.now();

      // ✅ Fetch CDC events - payload removed, data fetched from API instead
      String fetchSql = """
          SELECT id, event_type, entity_id, realm_id, created_at
          FROM change_events
          WHERE NOT processed
          ORDER BY id ASC
          LIMIT ?
          """;

      List<Map<String, Object>> events = keycloakJdbc.queryForList(fetchSql, batchSize);

      if (events.isEmpty()) {
        log.debug("📊 No new events to process");
        return;
      }

      log.info("📥 Processing {} new events from Keycloak DB", events.size());

      // Process events
      List<Long> processedEventIds = new ArrayList<>();

      for (Map<String, Object> event : events) {
        try {
          processEvent(event);
          processedEventIds.add((Long) event.get("id"));
          totalEventsProcessed++;

        } catch (Exception e) {
          log.error("❌ Failed to process event {}: {}", event.get("id"), e.getMessage(), e);
          // Continue with next events
        }
      }

      // Mark as processed
      if (!processedEventIds.isEmpty()) {
        markEventsAsProcessed(keycloakJdbc, processedEventIds);
        lastProcessedEventId = processedEventIds.get(processedEventIds.size() - 1);
      }

      log.info("✅ Processed {} events successfully", processedEventIds.size());

    } catch (Exception e) {
      log.error("❌ Error during event polling: {}", e.getMessage(), e);
    }
  }

  /**
   * ✅ Process individual CDC event - data fetched from Keycloak API
   */
  private void processEvent(Map<String, Object> event) {
    String eventType = (String) event.get("event_type");
    String entityId = (String) event.get("entity_id");
    String realmId = (String) event.get("realm_id");

    String tenantKey = mapRealmToTenant(realmId);

    log.debug("🔄 Processing CDC event: type={}, entity={}, realm={}, tenant={}", eventType,
        entityId, realmId, tenantKey);

    // ✅ Process directly - data fetched from Keycloak API (no payload in DB)
    eventProjectionService.processCdcEvent(eventType, entityId, realmId, tenantKey, null);
  }

  /**
   * Map Keycloak realm_id to tenant_key
   */
  private String mapRealmToTenant(String realmId) {
    if ("master".equals(realmId)) {
      return "admin";
    }
    return realmId;
  }

  /**
   * Mark events as processed
   */
  private void markEventsAsProcessed(JdbcTemplate jdbc, List<Long> eventIds) {
    if (eventIds.isEmpty()) {
      return;
    }

    String updateSql = """
        UPDATE change_events
        SET processed = true, processed_at = NOW()
        WHERE id = ANY(?)
        """;

    Long[] idsArray = eventIds.toArray(new Long[0]);
    int updated = jdbc.update(updateSql, (Object) idsArray);

    log.debug("✅ Marked {} events as processed", updated);
  }

  /**
   * 🗑️ Cleanup job - maže staré zpracované eventy
   */
  @Scheduled(cron = "${app.change-events.cleanup-cron:0 0 2 * * ?}") // 2:00 AM denně
  @Transactional
  public void cleanupOldEvents() {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    try {
      LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupAfterDays);

      String deleteSql = """
          DELETE FROM change_events
          WHERE processed = true
          AND processed_at < ?
          """;

      int deleted = keycloakJdbc.update(deleteSql, Timestamp.valueOf(cutoffDate));

      if (deleted > 0) {
        log.info("🗑️ Cleaned up {} old processed events (older than {} days)", deleted,
            cleanupAfterDays);
      }

    } catch (Exception e) {
      log.error("❌ Failed to cleanup old events: {}", e.getMessage(), e);
    }
  }

  /**
   * 📊 Health check endpoint data
   */
  public Map<String, Object> getHealthInfo() {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    Map<String, Object> health = new HashMap<>();

    try {
      // Event counts
      Long unprocessedCount = keycloakJdbc
          .queryForObject("SELECT COUNT(*) FROM change_events WHERE NOT processed", Long.class);

      Long totalCount = keycloakJdbc.queryForObject("SELECT COUNT(*) FROM change_events",
          Long.class);

      health.put("unprocessed_events", unprocessedCount != null ? unprocessedCount : 0);
      health.put("total_events", totalCount != null ? totalCount : 0);
      health.put("total_processed_lifetime", totalEventsProcessed);
      health.put("last_processed_event_id", lastProcessedEventId);
      health.put("last_poll_time", lastPollTime);
      health.put("polling_interval_ms", pollingIntervalMs);
      health.put("batch_size", batchSize);
      health.put("status", "ACTIVE");

      // Event type breakdown
      List<Map<String, Object>> eventTypeStats = keycloakJdbc.queryForList("""
          SELECT event_type, COUNT(*) as count
          FROM change_events
          WHERE NOT processed
          GROUP BY event_type
          ORDER BY count DESC
          """);

      health.put("unprocessed_by_type", eventTypeStats);

    } catch (Exception e) {
      health.put("status", "ERROR");
      health.put("error", e.getMessage());
    }

    return health;
  }

  /**
   * 📊 Detailní statistiky
   */
  public Map<String, Object> getDetailedStats() {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    Map<String, Object> stats = new HashMap<>();

    try {
      // Processing stats
      stats.put("total_events_processed", totalEventsProcessed);
      stats.put("last_processed_event_id", lastProcessedEventId);
      stats.put("last_poll_time", lastPollTime);

      // Event counts by realm
      List<Map<String, Object>> realmStats = keycloakJdbc.queryForList("""
          SELECT realm_id, COUNT(*) as count
          FROM change_events
          WHERE NOT processed
          GROUP BY realm_id
          """);

      stats.put("unprocessed_by_realm", realmStats);

      // Recent activity (last 1 hour)
      Long recentCount = keycloakJdbc.queryForObject("""
          SELECT COUNT(*)
          FROM change_events
          WHERE created_at > NOW() - INTERVAL '1 hour'
          """, Long.class);

      stats.put("events_last_hour", recentCount != null ? recentCount : 0);

      // Oldest unprocessed event
      Map<String, Object> oldestEvent = keycloakJdbc.queryForMap("""
          SELECT id, event_type, created_at
          FROM change_events
          WHERE NOT processed
          ORDER BY id ASC
          LIMIT 1
          """);

      stats.put("oldest_unprocessed_event", oldestEvent);

    } catch (Exception e) {
      stats.put("error", e.getMessage());
    }

    return stats;
  }

  /**
   * 🔧 Force processing (pro monitoring endpoint)
   */
  public Map<String, Object> forceProcessing() {
    Map<String, Object> result = new HashMap<>();

    try {
      log.info("🔧 Manual force processing triggered");

      pollAndProcessEvents();

      result.put("success", true);
      result.put("timestamp", LocalDateTime.now());
      result.put("message", "Force processing completed");

    } catch (Exception e) {
      result.put("success", false);
      result.put("error", e.getMessage());
      result.put("timestamp", LocalDateTime.now());
    }

    return result;
  }

  /**
   * 📊 Configuration info
   */
  public Map<String, Object> getConfigInfo() {
    Map<String, Object> config = new HashMap<>();
    config.put("polling_interval_ms", pollingIntervalMs);
    config.put("batch_size", batchSize);
    config.put("cleanup_after_days", cleanupAfterDays);
    config.put("mode", "POLLING_ONLY");
    config.put("deprecated_features", Map.of("notify_listen", "REMOVED", "webhook_spi", "REMOVED"));

    return config;
  }

  /**
   * 📊 Database statistics (used by admin controller)
   */
  public Map<String, Object> getDatabaseStats() {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    Map<String, Object> stats = new HashMap<>();

    try {
      // Total events
      Long totalEvents = keycloakJdbc.queryForObject("SELECT COUNT(*) FROM change_events",
          Long.class);

      // Processed vs unprocessed
      Long processedEvents = keycloakJdbc
          .queryForObject("SELECT COUNT(*) FROM change_events WHERE processed = true", Long.class);

      Long unprocessedEvents = keycloakJdbc
          .queryForObject("SELECT COUNT(*) FROM change_events WHERE processed = false", Long.class);

      stats.put("total_events", totalEvents != null ? totalEvents : 0);
      stats.put("processed_events", processedEvents != null ? processedEvents : 0);
      stats.put("unprocessed_events", unprocessedEvents != null ? unprocessedEvents : 0);

      // Average processing time (if you track it)
      stats.put("total_processed_lifetime", totalEventsProcessed);
      stats.put("last_processed_event_id", lastProcessedEventId);

    } catch (Exception e) {
      log.error("Failed to get database stats: {}", e.getMessage(), e);
      stats.put("error", e.getMessage());
    }

    return stats;
  }

  /**
   * 🔄 Flush pending changes (force immediate processing)
   */
  public void flushPendingChanges() {
    log.info("🔄 Flushing pending changes - forcing immediate poll");
    pollAndProcessEvents();
  }

  /**
   * 🔌 Force reconnect (no-op in polling mode, kept for compatibility)
   */
  public void forceReconnect() {
    log.info("🔌 Force reconnect called - no-op in polling mode");
    // V polling módu není co reconnectovat, ale zalogujeme pro info
  }

  /**
   * 🗑️ Cleanup old events with custom parameters (overloaded method)
   */
  @Transactional
  public Map<String, Object> cleanupOldEvents(int days, int batchSize) {
    JdbcTemplate keycloakJdbc = new JdbcTemplate(keycloakDataSource);

    Map<String, Object> result = new HashMap<>();

    try {
      LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

      String deleteSql = """
          DELETE FROM change_events
          WHERE processed = true
          AND processed_at < ?
          LIMIT ?
          """;

      int deleted = keycloakJdbc.update(deleteSql, Timestamp.valueOf(cutoffDate), batchSize);

      if (deleted > 0) {
        log.info("🗑️ Cleaned up {} old processed events (older than {} days)", deleted, days);
      }

      result.put("success", true);
      result.put("deleted_count", deleted);
      result.put("days_old", days);
      result.put("batch_size", batchSize);
      result.put("timestamp", LocalDateTime.now());

    } catch (Exception e) {
      log.error("❌ Failed to cleanup old events: {}", e.getMessage(), e);
      result.put("success", false);
      result.put("error", e.getMessage());
      result.put("timestamp", LocalDateTime.now());
    }

    return result;
  }

  /**
   * 🔧 Ensure triggers installed (compatibility method - returns success in
   * polling mode)
   */
  public Map<String, Object> ensureTriggersInstalled() {
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("message", "Polling mode - no triggers needed");
    result.put("mode", "POLLING_ONLY");

    log.debug("📊 ensureTriggersInstalled called - polling mode doesn't use triggers");

    return result;
  }

  /**
   * 🔄 Process fallback events (compatibility method)
   */
  public void processFallbackEvents() {
    log.info("🔄 Processing fallback events - running manual poll");
    pollAndProcessEvents();
  }

  /**
   * ⏸️ Stop listening (no-op in polling mode, kept for compatibility)
   */
  public void stopListening() {
    log.info("⏸️ Stop listening called - no-op in polling mode (scheduled job continues)");
    // V polling módu nemůžeme zastavit @Scheduled job běhově
    // Pro skutečné zastavení by bylo potřeba použít TaskScheduler s dynamic
    // scheduling
  }

  /**
   * ▶️ Start listening (no-op in polling mode, kept for compatibility)
   */
  public void startListening() {
    log.info("▶️ Start listening called - no-op in polling mode (scheduled job auto-starts)");
    // V polling módu je @Scheduled job vždy aktivní
  }
}
