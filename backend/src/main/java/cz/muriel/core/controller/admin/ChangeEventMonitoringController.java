package cz.muriel.core.controller.admin;

import cz.muriel.core.service.ChangeEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 🔄 Change Event Monitoring Controller
 * 
 * Admin endpoints pro monitoring a správu PostgreSQL trigger synchronization
 * systému. Provides detailed insights into event processing, health, and
 * performance metrics.
 */
@RestController @RequestMapping("/api/admin/change-events") @RequiredArgsConstructor @Slf4j @PreAuthorize("hasRole('ADMIN')")
public class ChangeEventMonitoringController {

  private final ChangeEventProcessor processor;

  /**
   * 🏥 Enhanced health check s detailním stavem
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getHealth() {
    Map<String, Object> health = processor.getHealthInfo();

    String status = (Boolean) health.get("listening") ? "UP" : "DOWN";
    health.put("status", status);
    health.put("timestamp", LocalDateTime.now());

    return ResponseEntity.ok(health);
  }

  /**
   * ⚙️ Konfigurace systému a aktivní optimalizace
   */
  @GetMapping("/config")
  public ResponseEntity<Map<String, Object>> getConfig() {
    Map<String, Object> config = processor.getConfigInfo();
    return ResponseEntity.ok(config);
  }

  /**
   * 📊 Detailní statistiky výkonu
   */
  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getStats() {
    Map<String, Object> stats = processor.getDetailedStats();
    return ResponseEntity.ok(stats);
  }

  /**
   * 🗃️ Database statistiky přes views
   */
  @GetMapping("/db-stats")
  public ResponseEntity<Map<String, Object>> getDatabaseStats() {
    Map<String, Object> dbStats = processor.getDatabaseStats();
    return ResponseEntity.ok(dbStats);
  }

  /**
   * 🚀 Manuální flush pending changes
   */
  @PostMapping("/flush")
  public ResponseEntity<Map<String, Object>> flushChanges() {
    log.info("🚀 Manual flush requested");
    processor.flushPendingChanges();

    return ResponseEntity.ok(Map.of("status", "success", "message", "Flush triggered manually",
        "timestamp", LocalDateTime.now()));
  }

  /**
   * 🔄 Force reconnect LISTEN spojení
   */
  @PostMapping("/reconnect")
  public ResponseEntity<Map<String, Object>> reconnect() {
    log.info("🔄 Manual reconnect requested");

    try {
      processor.forceReconnect();

      return ResponseEntity.ok(Map.of("status", "success", "message",
          "Reconnect triggered successfully", "timestamp", LocalDateTime.now()));

    } catch (Exception e) {
      log.error("❌ Failed to trigger reconnect: {}", e.getMessage(), e);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("status", "error", "message",
              "Failed to trigger reconnect: " + e.getMessage(), "timestamp", LocalDateTime.now()));
    }
  }

  /**
   * 🧹 Batch cleanup starých zpracovaných eventů
   */
  @PostMapping("/cleanup")
  public ResponseEntity<Map<String, Object>> cleanup(@RequestParam(defaultValue = "7") int daysOld,
      @RequestParam(defaultValue = "1000") int batchSize) {

    log.info("🧹 Manual cleanup requested: daysOld={}, batchSize={}", daysOld, batchSize);

    try {
      Map<String, Object> result = processor.cleanupOldEvents(daysOld, batchSize);
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("❌ Failed to cleanup old events: {}", e.getMessage(), e);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error",
          "message", "Failed to cleanup: " + e.getMessage(), "timestamp", LocalDateTime.now()));
    }
  }

  /**
   * 🔧 NOVÝ: Endpoint pro dokončení instalace triggerů Volá se pokud se triggery
   * nepodařilo nainstalovat při startu
   */
  @PostMapping("/install-triggers") @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> installTriggers() {
    log.info("🔧 Manual trigger installation requested");

    Map<String, Object> result = processor.ensureTriggersInstalled();

    if ((Boolean) result.get("success")) {
      return ResponseEntity.ok(result);
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
  }
}
