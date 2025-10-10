package cz.muriel.core.streaming.service;

import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.streaming.entity.CommandQueue;
import cz.muriel.core.streaming.entity.OutboxFinal;
import cz.muriel.core.streaming.entity.WorkState;
import cz.muriel.core.streaming.repository.CommandQueueRepository;
import cz.muriel.core.streaming.repository.OutboxFinalRepository;
import cz.muriel.core.streaming.repository.WorkStateRepository;
import lombok.extern.slf4j.Slf4j;
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
 * Polls command_queue, processes commands, updates work_state, 
 * and writes to outbox_final in a single transaction.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "streaming.enabled", havingValue = "true")
public class WorkerService {

    private final CommandQueueRepository commandQueueRepository;
    private final WorkStateRepository workStateRepository;
    private final OutboxFinalRepository outboxFinalRepository;
    private final MetamodelLoader metamodelLoader;
    private final InflightPublisher inflightPublisher;
    
    private final String workerId;
    private GlobalMetamodelConfig globalConfig;

    public WorkerService(
            CommandQueueRepository commandQueueRepository,
            WorkStateRepository workStateRepository,
            OutboxFinalRepository outboxFinalRepository,
            MetamodelLoader metamodelLoader,
            InflightPublisher inflightPublisher) {
        this.commandQueueRepository = commandQueueRepository;
        this.workStateRepository = workStateRepository;
        this.outboxFinalRepository = outboxFinalRepository;
        this.metamodelLoader = metamodelLoader;
        this.inflightPublisher = inflightPublisher;
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        
        // Load global config
        this.globalConfig = metamodelLoader.loadGlobalConfig();
    }

    /**
     * Main worker loop - polls and processes commands
     */
    @Scheduled(fixedDelayString = "${streaming.worker.poll-interval-ms:100}")
    @Transactional
    public void processCommands() {
        if (!globalConfig.getStreaming().isEnabled()) {
            return;
        }

        try {
            int batchSize = globalConfig.getStreaming().getDefaultWorkerBatchSize();
            List<CommandQueue> commands = commandQueueRepository.fetchPendingCommandsForProcessing(
                Instant.now(), 
                batchSize
            );

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
        try {
            // 0. Publish inflight "updating" event
            inflightPublisher.publishUpdating(
                command.getEntity(), 
                command.getEntityId(), 
                command.getCorrelationId(), 
                command.getOperation()
            );

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
            inflightPublisher.publishCompleted(
                command.getEntity(), 
                command.getEntityId(), 
                command.getCorrelationId(), 
                command.getOperation()
            );

            log.info("✅ Command {} processed successfully", command.getId());

        } catch (Exception e) {
            // Publish inflight "failed" event
            inflightPublisher.publishFailed(
                command.getEntity(), 
                command.getEntityId(), 
                command.getCorrelationId(), 
                command.getOperation(),
                e.getMessage()
            );
            
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

        WorkState workState = workStateRepository.findById(id)
            .orElse(new WorkState());

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
        workState.setTtl(Instant.now().plusSeconds(
            globalConfig.getStreaming().getWorkStateTtlMinutes() * 60L
        ));
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
     * Execute business logic (STUB)
     * TODO: Implement actual CRUD operations on entity tables
     */
    private void executeBusinessLogic(CommandQueue command) {
        log.debug("Executing {} operation on {}/{}", 
            command.getOperation(), command.getEntity(), command.getEntityId());
        
        // TODO: Parse command.payload
        // TODO: Perform CREATE/UPDATE/DELETE on actual entity table
        // TODO: Calculate diff for outbox
        
        // For now, just a stub
    }

    /**
     * Write event to outbox_final
     */
    private void writeToOutbox(CommandQueue command) {
        OutboxFinal outbox = new OutboxFinal();
        outbox.setEntity(command.getEntity());
        outbox.setEntityId(command.getEntityId());
        outbox.setTenantId(command.getTenantId());
        outbox.setOperation(command.getOperation());
        outbox.setCorrelationId(command.getCorrelationId());
        
        // TODO: Set diff_json and snapshot_json based on metamodel config
        outbox.setDiffJson("{}"); // Stub
        outbox.setSnapshotJson(null);
        
        // Set headers
        outbox.setHeadersJson(String.format(
            "{\"correlation_id\":\"%s\",\"timestamp\":\"%s\"}",
            command.getCorrelationId(), Instant.now()
        ));

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
            log.warn("Command {} moved to DLQ after {} retries", command.getId(), command.getRetryCount());
        } else {
            // Schedule retry with backoff
            long backoffMs = calculateBackoff(command.getRetryCount());
            command.setAvailableAt(Instant.now().plusMillis(backoffMs));
            command.setStatus("pending");
            log.info("Command {} will retry in {}ms (attempt {}/{})", 
                command.getId(), backoffMs, command.getRetryCount(), command.getMaxRetries());
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
