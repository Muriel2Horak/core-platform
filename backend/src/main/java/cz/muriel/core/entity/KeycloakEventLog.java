package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity @Table(name = "kc_event_log") @Data @NoArgsConstructor @AllArgsConstructor
public class KeycloakEventLog {

  @Id @Column(name = "event_hash", length = 64)
  private String eventHash;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
