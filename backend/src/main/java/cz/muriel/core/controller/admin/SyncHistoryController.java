package cz.muriel.core.controller.admin;

import cz.muriel.core.entity.SyncExecution;
import cz.muriel.core.entity.SyncExecution.SyncStatus;
import cz.muriel.core.repository.SyncExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📊 REST API pro historii synchronizací
 */
@RestController
@RequestMapping("/api/admin/sync-history")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
public class SyncHistoryController {

  private final SyncExecutionRepository syncExecutionRepository;

  /**
   * 📋 Seznam všech synchronizací s paginací a filtrací
   */
  @GetMapping
  public ResponseEntity<Page<SyncExecution>> listSyncExecutions(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String tenantKey,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "startTime") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<SyncExecution> results;

    if (status != null && tenantKey != null) {
      SyncStatus syncStatus = SyncStatus.valueOf(status.toUpperCase());
      results = syncExecutionRepository.findByStatusAndTenantKey(syncStatus, tenantKey, pageable);
    } else if (status != null) {
      SyncStatus syncStatus = SyncStatus.valueOf(status.toUpperCase());
      results = syncExecutionRepository.findByStatus(syncStatus, pageable);
    } else if (tenantKey != null) {
      results = syncExecutionRepository.findByTenantKey(tenantKey, pageable);
    } else {
      results = syncExecutionRepository.findAll(pageable);
    }

    return ResponseEntity.ok(results);
  }

  /**
   * 🔍 Detail jedné synchronizace
   */
  @GetMapping("/{id}")
  public ResponseEntity<SyncExecution> getSyncExecution(@PathVariable String id) {
    return syncExecutionRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * 🗑️ Smazání synchronizace z historie
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSyncExecution(@PathVariable String id) {
    if (!syncExecutionRepository.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    syncExecutionRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * 🗑️ Vyčištění starých záznamů
   */
  @DeleteMapping("/cleanup")
  public ResponseEntity<Integer> cleanupOldExecutions(
      @RequestParam(defaultValue = "30") int daysOld) {
    // TODO: Implementovat cleanup starších než X dní
    return ResponseEntity.ok(0);
  }

  /**
   * 📊 Statistiky synchronizací
   */
  @GetMapping("/stats")
  public ResponseEntity<?> getSyncStats() {
    List<SyncExecution> running = syncExecutionRepository.findByStatusIn(
        List.of(SyncStatus.RUNNING));
    
    long completedCount = syncExecutionRepository.findByStatusIn(
        List.of(SyncStatus.COMPLETED)).size();
    
    long failedCount = syncExecutionRepository.findByStatusIn(
        List.of(SyncStatus.FAILED)).size();

    return ResponseEntity.ok(java.util.Map.of(
        "running", running.size(),
        "completed", completedCount,
        "failed", failedCount
    ));
  }
}
