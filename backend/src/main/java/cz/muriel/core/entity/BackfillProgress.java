package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity @Table(name = "backfill_progress") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BackfillProgress {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, unique = true)
  private String tenantId;

  @Builder.Default @Column(name = "total_users", nullable = false)
  private Integer totalUsers = 0;

  @Builder.Default @Column(name = "processed_users", nullable = false)
  private Integer processedUsers = 0;

  @Builder.Default @Column(name = "failed_users", nullable = false)
  private Integer failedUsers = 0;

  @Builder.Default @Column(name = "status", nullable = false, length = 50)
  private String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, FAILED

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "last_processed_keycloak_id")
  private String lastProcessedKeycloakId;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
