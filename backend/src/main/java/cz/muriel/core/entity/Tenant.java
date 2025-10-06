package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Entity @Table(name = "tenants") @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

  @Id
  // ❌ REMOVED: @GeneratedValue(strategy = GenerationType.AUTO) - náhodné UUID
  private UUID id;

  @Column(name = "key", unique = true, nullable = false)
  private String key; // 🎯 SIMPLIFIED: This IS the realm name

  @Column(name = "keycloak_realm_id", unique = true)
  private String keycloakRealmId; // 🆕 Keycloak realm UUID for CDC event mapping

  /**
   * 🎯 OPTIMIZED: Realm name is the same as tenant key
   */
  public String getRealm() {
    return key; // realm = tenant key
  }

  /**
   * 🎯 OPTIMIZED: Set realm (updates tenant key)
   */
  public void setRealm(String realm) {
    this.key = realm; // realm = tenant key
    this.id = generateUuidFromKey(realm); // regenerate deterministic ID
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

  /**
   * 🎯 OPTIMIZED: Set name (for test compatibility - doesn't persist)
   */
  @Transient
  public void setName(String name) {
    // This is a no-op since name is derived from Keycloak
    // Method exists for test compatibility only
  }

  /**
   * 🔧 DETERMINISTIC UUID: Generates consistent UUID based on tenant key This
   * ensures the same tenant key ALWAYS produces the same UUID, solving partial
   * data restore issues.
   */
  @PrePersist
  public void generateDeterministicId() {
    if (id == null && key != null) {
      id = generateUuidFromKey(key);
    }
  }

  /**
   * 🔐 DETERMINISTIC UUID GENERATION: Creates UUID from tenant key using SHA-256
   * This ensures consistent UUIDs across different database instances
   */
  public static UUID generateUuidFromKey(String tenantKey) {
    try {
      // Use SHA-256 to create deterministic hash from tenant key
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(("tenant:" + tenantKey).getBytes(StandardCharsets.UTF_8));

      // Convert first 16 bytes of hash to UUID
      long mostSigBits = 0;
      long leastSigBits = 0;

      for (int i = 0; i < 8; i++) {
        mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
      }
      for (int i = 8; i < 16; i++) {
        leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
      }

      // Set version (4) and variant bits to make it a valid UUID v4
      mostSigBits &= ~0x0000f000L; // Clear version
      mostSigBits |= 0x00004000L; // Set version to 4
      leastSigBits &= ~0xc000000000000000L; // Clear variant
      leastSigBits |= 0x8000000000000000L; // Set variant

      return new UUID(mostSigBits, leastSigBits);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * 🔧 HELPER: Manual ID setting (for migrations or tests)
   */
  public void setKeyAndGenerateId(String key) {
    this.key = key;
    this.id = generateUuidFromKey(key);
  }
}
