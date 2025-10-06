package cz.muriel.core.service;

import cz.muriel.core.entity.ChangeEventEntity;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.keycloak.ChangeEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔄 Polling služba pro zpracování CDC eventů z change_events tabulky
 * 
 * Tato služba: - Polluje change_events tabulku každých X sekund - Načítá
 * nezpracované eventy (processed = false) - Deleguje zpracování na
 * KeycloakEventProjectionService - Označuje eventy jako zpracované - Pravidelně
 * čistí staré zpracované eventy
 */
@Service @Slf4j @RequiredArgsConstructor @ConditionalOnProperty(name = "app.change-events.listener-enabled", havingValue = "true", matchIfMissing = true)
public class ChangeEventPollingService {

  private final ChangeEventRepository changeEventRepository;
  private final KeycloakEventProjectionService projectionService;
  private final TenantService tenantService;

  @Value("${app.change-events.batch-size:100}")
  private int batchSize;

  @Value("${app.change-events.flush-interval-seconds:10}")
  private int flushIntervalSeconds;

  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final AtomicInteger processedCount = new AtomicInteger(0);
  private final AtomicInteger errorCount = new AtomicInteger(0);

  @PostConstruct
  public void init() {
    log.info("🔄 Change Event Polling Service initialized");
    log.info("   - Batch size: {}", batchSize);
    log.info("   - Flush interval: {} seconds", flushIntervalSeconds);
    log.info("   - Listener enabled: true");
  }

  /**
   * 🔄 Hlavní polling metoda - spouští se každých X sekund
   */
  @Scheduled(fixedDelayString = "${app.change-events.flush-interval-seconds}000", initialDelayString = "5000") // 5s
                                                                                                               // initial
                                                                                                               // delay
  public void pollAndProcessEvents() {
    // Prevent concurrent execution
    if (!isProcessing.compareAndSet(false, true)) {
      log.debug("⏭️ Skipping poll - previous execution still running");
      return;
    }

    try {
      // Načti nezpracované eventy
      List<ChangeEventEntity> events = changeEventRepository
          .findByProcessedFalseOrderByCreatedAtAsc();

      if (events.isEmpty()) {
        log.trace("📭 No unprocessed events found");
        return;
      }

      log.info("📨 Found {} unprocessed change events, processing...", events.size());

      // Zpracuj eventy v batch
      int processed = 0;
      int failed = 0;

      for (ChangeEventEntity event : events) {
        try {
          processEvent(event);
          processed++;
          processedCount.incrementAndGet();
        } catch (Exception e) {
          failed++;
          errorCount.incrementAndGet();
          log.error("❌ Failed to process event {}: {}", event.getId(), e.getMessage(), e);
          // Pokračuj se zpracováním dalších eventů
        }
      }

      log.info("✅ Processed {} events ({} successful, {} failed)", events.size(), processed,
          failed);

    } catch (Exception e) {
      log.error("❌ Error during event polling: {}", e.getMessage(), e);
    } finally {
      isProcessing.set(false);
    }
  }

  /**
   * 🔧 Zpracuje jednotlivý event
   */
  @Transactional
  protected void processEvent(ChangeEventEntity event) {
    log.debug("🔄 Processing event: id={}, type={}, entity={}, realm={}", event.getId(),
        event.getEventType(), event.getEntityId(), event.getRealmId());

    try {
      // Mapuj realm_id na tenant_key
      String tenantKey = mapRealmIdToTenantKey(event.getRealmId());

      if (tenantKey == null) {
        log.warn("⚠️ Cannot map realm_id {} to tenant_key, skipping event {}", event.getRealmId(),
            event.getId());
        // Označíme jako zpracovaný, aby se neopakoval
        event.markAsProcessed();
        changeEventRepository.save(event);
        return;
      }

      // Deleguj zpracování na projection service
      projectionService.processCdcEvent(event.getEventType(), event.getEntityId(),
          event.getRealmId(), tenantKey, null // Payload není potřeba, projection service si dotáhne
                                              // data z Keycloak API
      );

      // Označ event jako zpracovaný
      event.markAsProcessed();
      changeEventRepository.save(event);

      log.debug("✅ Event {} processed successfully", event.getId());

    } catch (Exception e) {
      log.error("❌ Failed to process event {}: {}", event.getId(), e.getMessage());
      throw e; // Re-throw pro statistiky
    }
  }

  /**
   * 🗺️ Mapuje realm_id z Keycloaku na tenant_key
   */
  private String mapRealmIdToTenantKey(String realmId) {
    if (realmId == null) {
      return null;
    }

    log.debug("🔍 Mapping realm_id to tenant_key: {}", realmId);

    // 1. PRIMARY: Zkus najít tenant podle realm_id (volá Keycloak API)
    Optional<Tenant> tenant = tenantService.findTenantByRealmId(realmId);

    if (tenant.isPresent()) {
      String tenantKey = tenant.get().getKey();
      log.debug("✅ Mapped realm_id {} to tenant_key {} via TenantService", realmId, tenantKey);
      return tenantKey;
    }

    // 2. FALLBACK: Partial match s existujícími tenanty
    log.debug("🔍 Attempting to find tenant by partial realm_id match: {}", realmId);
    List<Tenant> allTenants = tenantService.getAllTenants();

    for (Tenant t : allTenants) {
      if (realmId.contains(t.getKey()) || t.getKey().contains(realmId)) {
        log.debug("✅ Found tenant {} matching realm_id {}", t.getKey(), realmId);
        return t.getKey();
      }
    }

    // 3. LAST RESORT: master realm → admin tenant
    if (realmId.contains("master") || realmId.contains("admin")) {
      log.debug("✅ Mapping master/admin realm to 'admin' tenant");
      return "admin";
    }

    log.warn("⚠️ Could not map realm_id {} to any tenant", realmId);
    return null;
  }

  /**
   * 🧹 Cleanup starých zpracovaných eventů (spouští se denně ve 2:30)
   */
  @Scheduled(cron = "${app.change-events.cleanup-cron:0 30 2 * * *}") @Transactional
  public void cleanupProcessedEvents() {
    log.info("🧹 Starting cleanup of old processed events...");

    try {
      // Smaž eventy starší než 7 dní
      LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

      List<ChangeEventEntity> oldEvents = changeEventRepository
          .findByProcessedTrueAndProcessedAtBefore(cutoffDate);

      if (oldEvents.isEmpty()) {
        log.info("✅ No old events to clean up");
        return;
      }

      changeEventRepository.deleteAll(oldEvents);

      log.info("✅ Cleaned up {} old processed events (older than 7 days)", oldEvents.size());

    } catch (Exception e) {
      log.error("❌ Error during event cleanup: {}", e.getMessage(), e);
    }
  }

  /**
   * 📊 Vrátí statistiky zpracování
   */
  public PollingStats getStats() {
    long unprocessedCount = changeEventRepository.countByProcessedFalse();

    return new PollingStats(processedCount.get(), errorCount.get(), unprocessedCount,
        isProcessing.get());
  }

  /**
   * 🔄 Manuální trigger pro flush (pro admin API)
   */
  public void manualFlush() {
    log.info("🔄 Manual flush triggered");
    pollAndProcessEvents();
  }

  /**
   * 📊 Statistiky polling služby
   */
  public record PollingStats(int totalProcessed, int totalErrors, long unprocessedCount,
      boolean isCurrentlyProcessing) {
  }
}
