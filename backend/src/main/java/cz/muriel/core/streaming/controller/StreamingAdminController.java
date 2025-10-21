package cz.muriel.core.streaming.controller;

import cz.muriel.core.metamodel.MetamodelLoader;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StreamingEntityConfig;
import cz.muriel.core.metamodel.schema.StreamingGlobalConfig;
import cz.muriel.core.streaming.dto.*;
import cz.muriel.core.streaming.entity.CommandQueue;
import cz.muriel.core.streaming.metrics.StreamingMetrics;
import cz.muriel.core.streaming.repository.CommandQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j @RestController @RequestMapping("/api/admin/streaming") @RequiredArgsConstructor @ConditionalOnProperty(name = "streaming.enabled", havingValue = "true") @PreAuthorize("hasAnyRole('PlatformAdmin', 'Ops')")
public class StreamingAdminController {

  private final MetamodelLoader metamodelLoader;
  private final CommandQueueRepository commandQueueRepository;
  private final StreamingMetrics streamingMetrics;

  @GetMapping("/config")
  public ResponseEntity<StreamingConfigResponse> getConfig() {
    StreamingGlobalConfig globalConfig = metamodelLoader.loadGlobalConfig().getStreaming();

    StreamingGlobalConfigDto globalDto = StreamingGlobalConfigDto.builder()
        .enabled(globalConfig.isEnabled()).bootstrapServers("kafka:9092") // From env or config
        .replicationFactor(globalConfig.getDefaultReplicationFactor())
        .defaultPartitions(globalConfig.getDefaultPartitions()).build();

    Map<String, EntitySchema> schemas = metamodelLoader.loadSchemas();
    List<StreamingEntityConfigDto> entities = schemas.values().stream()
        .filter(e -> e.getStreaming() != null && Boolean.TRUE.equals(e.getStreaming().getEnabled()))
        .map(this::toEntityDto).collect(Collectors.toList());

    StreamingConfigResponse response = StreamingConfigResponse.builder().global(globalDto)
        .entities(entities).build();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/metrics")
  public ResponseEntity<Map<String, Object>> getMetrics() {
    // Get metrics from StreamingMetrics and CommandQueue repository
    long pendingCount = commandQueueRepository.countByStatus("pending");
    long processingCount = commandQueueRepository.countByStatus("processing");
    long dlqCount = commandQueueRepository.countByStatus("dlq");
    long completedCount = commandQueueRepository.countByStatus("completed");

    Map<String, Object> metrics = Map.of("queueStatus", Map.of("pending", pendingCount,
        "processing", processingCount, "dlq", dlqCount, "completed", completedCount), "timestamp",
        System.currentTimeMillis());

    return ResponseEntity.ok(metrics);
  }

  @GetMapping("/dlq")
  public ResponseEntity<Page<DlqMessageDto>> getDlqMessages(
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) String errorType, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("movedToDlqAt").descending());

    Page<CommandQueue> dlqPage;
    if (entity != null && errorType != null) {
      dlqPage = commandQueueRepository.findByStatusAndEntityAndErrorType("dlq", entity, errorType,
          pageable);
    } else if (entity != null) {
      dlqPage = commandQueueRepository.findByStatusAndEntity("dlq", entity, pageable);
    } else {
      dlqPage = commandQueueRepository.findByStatus("dlq", pageable);
    }

    Page<DlqMessageDto> response = dlqPage.map(this::toDlqDto);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/dlq/{id}/retry")
  public ResponseEntity<Void> retryDlqMessage(@PathVariable UUID id) {
    CommandQueue cmd = commandQueueRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("DLQ message not found: " + id));

    if (!"dlq".equals(cmd.getStatus())) {
      throw new IllegalArgumentException("Message is not in DLQ: " + id);
    }

    cmd.setStatus("pending");
    cmd.setRetryCount(0);
    cmd.setErrorMessage(null);
    commandQueueRepository.save(cmd);

    log.info("Retried DLQ message: {}", id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/dlq/retry-all")
  public ResponseEntity<Integer> retryAllDlq(@RequestParam(required = false) String entity,
      @RequestParam(required = false) String errorType) {

    List<CommandQueue> dlqMessages;
    if (entity != null && errorType != null) {
      dlqMessages = commandQueueRepository
          .findByStatusAndEntityAndErrorType("dlq", entity, errorType, Pageable.unpaged())
          .getContent();
    } else if (entity != null) {
      dlqMessages = commandQueueRepository.findByStatusAndEntity("dlq", entity, Pageable.unpaged())
          .getContent();
    } else {
      dlqMessages = commandQueueRepository.findByStatus("dlq", Pageable.unpaged()).getContent();
    }

    dlqMessages.forEach(cmd -> {
      cmd.setStatus("pending");
      cmd.setRetryCount(0);
      cmd.setErrorMessage(null);
    });
    commandQueueRepository.saveAll(dlqMessages);

    log.info("Retried {} DLQ messages", dlqMessages.size());
    return ResponseEntity.ok(dlqMessages.size());
  }

  @DeleteMapping("/dlq/{id}")
  public ResponseEntity<Void> deleteDlqMessage(@PathVariable UUID id) {
    CommandQueue cmd = commandQueueRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("DLQ message not found: " + id));

    if (!"dlq".equals(cmd.getStatus())) {
      throw new IllegalArgumentException("Message is not in DLQ: " + id);
    }

    commandQueueRepository.delete(cmd);
    log.info("Deleted DLQ message: {}", id);
    return ResponseEntity.ok().build();
  }

  private StreamingEntityConfigDto toEntityDto(EntitySchema schema) {
    StreamingEntityConfig sc = schema.getStreaming();
    return StreamingEntityConfigDto.builder().entityName(schema.getEntity())
        .enabled(sc.getEnabled())
        .topicName(schema.getEntity().toLowerCase().replace("_", "-") + "-events")
        .partitions(sc.getPartitions()).eventStrategy(sc.getEventPayloadMode())
        .retentionDays(sc.getRetentionHours() / 24).build();
  }

  private DlqMessageDto toDlqDto(CommandQueue cmd) {
    return DlqMessageDto.builder().id(cmd.getId()).entity(cmd.getEntity())
        .entityId(cmd.getEntityId()).operation(cmd.getOperation()).errorType(cmd.getErrorMessage())
        .errorMessage(cmd.getErrorMessage())
        .movedToDlqAt(cmd.getUpdatedAt() != null ? java.time.LocalDateTime
            .ofInstant(cmd.getUpdatedAt(), java.time.ZoneId.systemDefault()) : null)
        .attemptCount(cmd.getRetryCount()).payload(cmd.getPayload()).build();
  }
}
