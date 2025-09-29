package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

@Entity @Table(name = "tenants") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "key", unique = true, nullable = false)
  private String key; // 🎯 SIMPLIFIED: This IS the realm name

  /**
   * 🎯 OPTIMIZED: Realm name is the same as tenant key
   */
  public String getRealm() {
    return key; // realm = tenant key
  }

  /**
   * 🎯 OPTIMIZED: Get display name from Keycloak realm (not stored in DB) This
   * method should be used sparingly - for display purposes only
   */
  @Transient
  public String getName() {
    // This will be lazy-loaded from Keycloak when needed
    // Implementation will be provided by TenantService
    return "Tenant " + key; // Fallback if Keycloak not available
  }
}
