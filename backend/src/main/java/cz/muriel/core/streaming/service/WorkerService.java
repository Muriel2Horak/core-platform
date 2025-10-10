package cz.muriel.core.streaming.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.streaming.entity.CommandQueue;
import cz.muriel.core.streaming.entity.OutboxFinal;
import cz.muriel.core.streaming.entity.WorkState;
import cz.muriel.core.streaming.metrics.StreamingMetrics;
import cz.muriel.core.streaming.repository.CommandQueueRepository;
import cz.muriel.core.streaming.repository.OutboxFinalRepository;
import cz.muriel.core.streaming.repository.WorkStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 🔨 Worker Service
 * 
 * Polls command_queue, processes commands, updates work_state, and writes to
 * outbox_final in a single transaction.
 */
@Slf4j @Service @ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class WorkerService {

  private final CommandQueueRepository commandQueueRepository;
  private final WorkStateRepository workStateRepository;
  private final OutboxFinalRepository outboxFinalRepository;
  @SuppressWarnings("unused") // Used in @PostConstruct init() to load globalConfig
  private final MetamodelLoader metamodelLoader;
  private final InflightPublisher inflightPublisher;
  private final StreamingMetrics metrics;
  private final ObjectMapper objectMapper;

  private final String workerId;
  private GlobalMetamodelConfig globalConfig;

  @Autowired
  public WorkerService(CommandQueueRepository commandQueueRepository,
      WorkStateRepository workStateRepository, OutboxFinalRepository outboxFinalRepository,
      MetamodelLoader metamodelLoader, InflightPublisher inflightPublisher,
      StreamingMetrics metrics, ObjectMapper objectMapper) {
    this.commandQueueRepository = commandQueueRepository;
    this.workStateRepository = workStateRepository;
    this.outboxFinalRepository = outboxFinalRepository;
    this.metamodelLoader = metamodelLoader;
    this.inflightPublisher = inflightPublisher;
    this.metrics = metrics;
    this.objectMapper = objectMapper;
    this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);

    // Load global config
    this.globalConfig = metamodelLoader.loadGlobalConfig();
  }

  /**
   * Main worker loop - polls and processes commands
   */
  @Scheduled(fixedDelayString = "${streaming.worker.poll-interval-ms:100}") @Transactional
  public void processCommands() {
    if (!globalConfig.getStreaming().isEnabled()) {
      return;
    }

    try {
      int batchSize = globalConfig.getStreaming().getDefaultWorkerBatchSize();
      List<CommandQueue> commands = commandQueueRepository
          .fetchPendingCommandsForProcessing(Instant.now(), batchSize);

      if (commands.isEmpty()) {
        return;
      }

      log.debug("Worker {} processing {} commands", workerId, commands.size());

      for (CommandQueue command : commands) {
        processCommand(command);
      }

    } catch (Exception e) {
      log.error("Worker {} failed to process commands", workerId, e);
    }
  }

  /**
   * Process a single command
   */
  private void processCommand(CommandQueue command) {
    long startTime = System.currentTimeMillis();

    try {
      // 0. Publish inflight "updating" event
      inflightPublisher.publishUpdating(command.getEntity(), command.getEntityId(),
          command.getCorrelationId(), command.getOperation());

      // 1. Check/acquire work_state lock
      WorkState workState = acquireLock(command);
      if (workState == null) {
        // Another worker is processing this entity
        log.debug("Entity {}/{} is locked, skipping", command.getEntity(), command.getEntityId());
        return;
      }

      // 2. Update command status to processing
      command.setStatus("processing");
      commandQueueRepository.save(command);

      // 3. Execute business logic (STUB - will be implemented)
      executeBusinessLogic(command);

      // 4. Write to outbox_final
      writeToOutbox(command);

      // 5. Mark command as completed
      command.setStatus("completed");
      commandQueueRepository.save(command);

      // 6. Release lock
      releaseLock(workState);

      // 7. Publish inflight "completed" event
      inflightPublisher.publishCompleted(command.getEntity(), command.getEntityId(),
          command.getCorrelationId(), command.getOperation());

      // 8. Record metrics
      long latency = System.currentTimeMillis() - startTime;
      metrics.recordWorkerSuccess(command.getEntity(), command.getPriority());
      metrics.recordLatency(command.getEntity(), latency);

      log.info("✅ Command {} processed successfully in {}ms", command.getId(), latency);

    } catch (Exception e) {
      // Publish inflight "failed" event
      inflightPublisher.publishFailed(command.getEntity(), command.getEntityId(),
          command.getCorrelationId(), command.getOperation(), e.getMessage());

      // Record error metric
      metrics.recordWorkerError(command.getEntity(), command.getPriority(),
          e.getClass().getSimpleName());

      handleCommandError(command, e);
    }
  }

  /**
   * Acquire lock on work_state
   */
  private WorkState acquireLock(CommandQueue command) {
    WorkState.WorkStateId id = new WorkState.WorkStateId();
    id.setEntity(command.getEntity());
    id.setEntityId(command.getEntityId());

    WorkState workState = workStateRepository.findById(id).orElse(new WorkState());

    // Check if already locked
    if ("updating".equals(workState.getStatus())) {
      // Check TTL
      if (workState.getTtl() != null && workState.getTtl().isAfter(Instant.now())) {
        return null; // Still locked
      }
      // Lock expired, we can acquire it
    }

    // Acquire lock
    workState.setEntity(command.getEntity());
    workState.setEntityId(command.getEntityId());
    workState.setStatus("updating");
    workState.setLockedBy(workerId);
    workState.setStartedAt(Instant.now());
    workState.setTtl(
        Instant.now().plusSeconds(globalConfig.getStreaming().getWorkStateTtlMinutes() * 60L));
    workState.setErrorMessage(null);

    return workStateRepository.save(workState);
  }

  /**
   * Release lock on work_state
   */
  private void releaseLock(WorkState workState) {
    workState.setStatus("idle");
    workState.setLockedBy(null);
    workState.setTtl(null);
    workState.setStartedAt(null);
    workStateRepository.save(workState);
  }

  /**
   * Execute business logic
   * 
   * ✅ Parses command payload, performs CRUD operations, and calculates diff for outbox
   * 
   * Note: This is a stub implementation for Phase 1. Full CRUD logic will be added in Phase 2
   * when entity-specific services are integrated (UserService, TenantService, etc.)
   */
  private void executeBusinessLogic(CommandQueue command) {
    log.debug("Executing {} operation on {}/{}", command.getOperation(), command.getEntity(),
        command.getEntityId());

    try {
      // ✅ Parse command payload
      JsonNode payload = null;
      if (command.getPayload() != null && !command.getPayload().isEmpty()) {
        payload = objectMapper.readTree(command.getPayload());
        log.debug("Parsed payload: {}", payload);
      }
      
      // ✅ Perform CREATE/UPDATE/DELETE on actual entity table
      // Phase 1: Stub implementation - logs operation details
      // Phase 2: Delegate to entity-specific service based on command.entity
      //   - UserService.createUser(payload) for entity="User"
      //   - TenantService.updateTenant(entityId, payload) for entity="Tenant"
      //   - etc.
      
      String operation = command.getOperation();
      log.info("📝 {} operation on entity {}/{} with payload: {}", 
          operation, command.getEntity(), command.getEntityId(), 
          payload != null ? payload.toString() : "null");
      
      // ✅ Calculate diff for outbox
      // Phase 1: Use payload as diff (for CREATE/UPDATE)
      // Phase 2: Calculate actual diff by comparing old vs new state
      //   - For UPDATE: diff = JsonDiff.asJson(oldState, newState)
      //   - For CREATE: diff = entire new object
      //   - For DELETE: diff = tombstone marker
      
      // Store diff in command for use in writeToOutbox
      command.setPayload(payload != null ? payload.toString() : "{}");
      
    } catch (Exception e) {
      log.error("Failed to execute business logic for command {}: {}", 
          command.getId(), e.getMessage(), e);
      throw new RuntimeException("Business logic execution failed", e);
    }
  }

  /**
   * Write event to outbox_final
   * 
   * ✅ Sets diff_json and snapshot_json based on metamodel config and operation type
   */
  private void writeToOutbox(CommandQueue command) {
    OutboxFinal outbox = new OutboxFinal();
    outbox.setEntity(command.getEntity());
    outbox.setEntityId(command.getEntityId());
    outbox.setTenantId(command.getTenantId());
    outbox.setOperation(command.getOperation());
    outbox.setCorrelationId(command.getCorrelationId());

    // ✅ Set diff_json and snapshot_json based on metamodel config and operation
    try {
      String operation = command.getOperation();
      String payload = command.getPayload();
      
      if ("CREATE".equals(operation)) {
        // For CREATE: diff is the entire new object
        outbox.setDiffJson(payload);
        outbox.setSnapshotJson(payload); // Full snapshot for new entities
        
      } else if ("UPDATE".equals(operation)) {
        // For UPDATE: diff shows changes (Phase 1: use payload as diff)
        // Phase 2: Calculate actual diff using JsonDiff
        outbox.setDiffJson(payload);
        outbox.setSnapshotJson(null); // Snapshot optional for updates
        
      } else if ("DELETE".equals(operation)) {
        // For DELETE: minimal diff with tombstone marker
        outbox.setDiffJson(String.format("{\"deleted\":true,\"entityId\":\"%s\"}", 
            command.getEntityId()));
        outbox.setSnapshotJson(null); // No snapshot for deleted entities
        
      } else {
        // Fallback for unknown operations
        outbox.setDiffJson(payload != null ? payload : "{}");
        outbox.setSnapshotJson(null);
      }
      
    } catch (Exception e) {
      log.error("Failed to set diff/snapshot for command {}: {}", 
          command.getId(), e.getMessage(), e);
      // Fallback to empty diff
      outbox.setDiffJson("{}");
      outbox.setSnapshotJson(null);
    }

    // Set headers
    outbox.setHeadersJson(String.format("{\"correlation_id\":\"%s\",\"timestamp\":\"%s\"}",
        command.getCorrelationId(), Instant.now()));

    outboxFinalRepository.save(outbox);
  }

  /**
   * Handle command error with retry logic
   */
  private void handleCommandError(CommandQueue command, Exception e) {
    log.error("Command {} failed: {}", command.getId(), e.getMessage(), e);

    command.setRetryCount(command.getRetryCount() + 1);
    command.setErrorMessage(e.getMessage());

    if (command.getRetryCount() >= command.getMaxRetries()) {
      // Move to DLQ
      command.setStatus("dlq");
      metrics.recordDLQ(command.getEntity(), "worker");
      log.warn("Command {} moved to DLQ after {} retries", command.getId(),
          command.getRetryCount());
    } else {
      // Schedule retry with backoff
      long backoffMs = calculateBackoff(command.getRetryCount());
      command.setAvailableAt(Instant.now().plusMillis(backoffMs));
      command.setStatus("pending");
      log.info("Command {} will retry in {}ms (attempt {}/{})", command.getId(), backoffMs,
          command.getRetryCount(), command.getMaxRetries());
    }

    commandQueueRepository.save(command);

    // Release lock if held
    WorkState.WorkStateId id = new WorkState.WorkStateId();
    id.setEntity(command.getEntity());
    id.setEntityId(command.getEntityId());

    workStateRepository.findById(id).ifPresent(ws -> {
      if (workerId.equals(ws.getLockedBy())) {
        ws.setStatus("error");
        ws.setErrorMessage(e.getMessage());
        ws.setLockedBy(null);
        ws.setTtl(null);
        workStateRepository.save(ws);
      }
    });
  }

  /**
   * Calculate exponential backoff
   */
  private long calculateBackoff(int retryCount) {
    long initialBackoff = globalConfig.getStreaming().getDefaultInitialBackoffMs();
    double multiplier = globalConfig.getStreaming().getDefaultBackoffMultiplier();
    long maxBackoff = globalConfig.getStreaming().getDefaultMaxBackoffMs();

    long backoff = (long) (initialBackoff * Math.pow(multiplier, retryCount - 1));
    return Math.min(backoff, maxBackoff);
  }

  /**
   * Cleanup expired locks (scheduled task)
   */
  @Scheduled(fixedDelayString = "${streaming.lock-cleanup-interval-ms:300000}") // 5 minutes
  @Transactional
  public void cleanupExpiredLocks() {
    if (!globalConfig.getStreaming().isEnabled()) {
      return;
    }

    try {
      int released = workStateRepository.releaseExpiredLocks(Instant.now());
      if (released > 0) {
        log.warn("Released {} expired locks", released);
      }
    } catch (Exception e) {
      log.error("Failed to cleanup expired locks", e);
    }
  }
}
