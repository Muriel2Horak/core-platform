package cz.muriel.core.streaming.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Work State Entity Tracks entity-level processing state and locking
 */
@Data @Entity @Table(name = "work_state", indexes = {
    @Index(name = "idx_work_state_status", columnList = "status"),
    @Index(name = "idx_work_state_ttl", columnList = "ttl") }) @IdClass(WorkState.WorkStateId.class)
public class WorkState {

  @Id @Column(nullable = false, length = 100)
  private String entity;

  @Id @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(nullable = false, length = 20)
  private String status = "idle"; // idle, updating, error

  @Column(name = "locked_by", length = 100)
  private String lockedBy;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ttl")
  private Instant ttl;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @UpdateTimestamp @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Composite primary key
   */
  @Data
  public static class WorkStateId implements Serializable {
    private String entity;
    private UUID entityId;
  }
}
