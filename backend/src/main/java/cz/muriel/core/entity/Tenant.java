package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "tenants") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "key", unique = true, nullable = false)
  private String key;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "realm", nullable = false)
  private String realm;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
