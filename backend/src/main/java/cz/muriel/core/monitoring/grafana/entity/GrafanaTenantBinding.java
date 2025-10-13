package cz.muriel.core.monitoring.grafana.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 🔗 GRAFANA TENANT BINDING
 * 
 * Perzistentní mapování mezi tenanty a Grafana organizacemi
 * Obsahuje service account token pro autentizaci Grafana API
 */
@Entity
@Table(name = "grafana_tenant_bindings", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id", unique = true),
    @Index(name = "idx_grafana_org_id", columnList = "grafana_org_id")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrafanaTenantBinding {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, unique = true, length = 100)
  private String tenantId;

  @Column(name = "grafana_org_id", nullable = false)
  private Long grafanaOrgId;

  @Column(name = "service_account_id", nullable = false)
  private Long serviceAccountId;

  @Column(name = "service_account_name", nullable = false, length = 200)
  private String serviceAccountName;

  @Column(name = "service_account_token", nullable = false, length = 500)
  private String serviceAccountToken;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
