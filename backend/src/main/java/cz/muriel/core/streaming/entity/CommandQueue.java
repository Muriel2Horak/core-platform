package cz.muriel.core.streaming.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Command Queue Entity Represents an async command waiting to be processed
 */
@Data @Entity @Table(name = "command_queue", indexes = {
    @Index(name = "idx_command_queue_status_priority_available", columnList = "status, priority, available_at"),
    @Index(name = "idx_command_queue_entity_entity_id", columnList = "entity, entity_id"),
    @Index(name = "idx_command_queue_operation_id", columnList = "operation_id"),
    @Index(name = "idx_command_queue_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_command_queue_tenant_id", columnList = "tenant_id") })
public class CommandQueue {

  @Id @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String entity;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 20)
  private String operation; // CREATE, UPDATE, DELETE, BULK_UPDATE

  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(nullable = false, length = 20)
  private String priority = "normal"; // critical, high, normal, bulk

  @Column(nullable = false, length = 20)
  private String status = "pending"; // pending, processing, completed, failed, dlq

  @Column(name = "available_at", nullable = false)
  private Instant availableAt = Instant.now();

  @Column(name = "operation_id")
  private UUID operationId;

  @Column(name = "correlation_id", nullable = false)
  private UUID correlationId = UUID.randomUUID();

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Column(name = "max_retries", nullable = false)
  private Integer maxRetries = 3;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
