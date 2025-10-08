package cz.muriel.core.locks;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * Edit lock entity for optimistic concurrency control
 */
@Entity @Table(name = "edit_locks") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EditLock {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "lock_type", nullable = false) @Builder.Default
  private String lockType = "soft";

  @Column(name = "acquired_at", nullable = false) @Builder.Default
  private Instant acquiredAt = Instant.now();

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
