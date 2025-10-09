package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 🎭 Role entity - synchronized from Keycloak Supports both REALM and CLIENT
 * roles with composite role hierarchies
 */
@Entity @Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_keycloak_tenant", columnNames = { "keycloak_role_id",
        "tenant_key" }) }) @Data @EqualsAndHashCode(callSuper = true, exclude = { "users",
            "childRoles", "parentRoles" }) @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RoleEntity extends MultiTenantEntity {

  @Id
  // ❌ REMOVED: @GeneratedValue - používáme deterministické UUID
  private UUID id;

  @Column(name = "keycloak_role_id", nullable = false)
  private String keycloakRoleId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Enumerated(EnumType.STRING) @Column(name = "role_type", nullable = false)
  private RoleType roleType;

  @Column(name = "client_id")
  private String clientId;

  @Column(name = "composite", nullable = false) @Builder.Default
  private Boolean composite = false;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // ✅ V5 NOVÉ: Kompozitní role hierarchie
  @ManyToMany @JoinTable(name = "role_composites", joinColumns = @JoinColumn(name = "parent_role_id"), inverseJoinColumns = @JoinColumn(name = "child_role_id")) @Builder.Default
  private Set<RoleEntity> childRoles = new HashSet<>();

  @ManyToMany(mappedBy = "childRoles") @Builder.Default
  private Set<RoleEntity> parentRoles = new HashSet<>();

  // Many-to-Many relationship with users
  @ManyToMany(mappedBy = "roles") @Builder.Default
  private Set<UserDirectoryEntity> users = new HashSet<>();

  /**
   * 🔐 DETERMINISTICKÉ UUID GENERATION Generates consistent UUID from
   * keycloakRoleId + tenantId This ensures same role always has same UUID across
   * database instances
   */
  @PrePersist
  protected void onCreate() {
    if (id == null && keycloakRoleId != null && getTenantId() != null) {
      id = generateUuidFromKeycloakId(keycloakRoleId, getTenantId());
    }
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

  /**
   * 🔐 STATIC HELPER: Generate deterministic UUID from Keycloak ID + Tenant ID
   */
  public static UUID generateUuidFromKeycloakId(String keycloakRoleId, UUID tenantId) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String composite = "role:" + tenantId.toString() + ":" + keycloakRoleId;
      byte[] hash = digest.digest(composite.getBytes(StandardCharsets.UTF_8));

      // Convert first 16 bytes to UUID
      long mostSigBits = 0;
      long leastSigBits = 0;

      for (int i = 0; i < 8; i++) {
        mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
      }
      for (int i = 8; i < 16; i++) {
        leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
      }

      // Set UUID version 4 variant bits
      mostSigBits &= ~0x0000f000L;
      mostSigBits |= 0x00004000L;
      leastSigBits &= ~0xc000000000000000L;
      leastSigBits |= 0x8000000000000000L;

      return new UUID(mostSigBits, leastSigBits);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  // =====================================================
  // ✅ V5 UTILITY METHODS: Composite Role Helpers
  // =====================================================

  /**
   * Checks if this role is a root role (no parents)
   */
  public boolean isRootRole() {
    return parentRoles == null || parentRoles.isEmpty();
  }

  /**
   * Checks if this role contains another role (direct child)
   */
  public boolean containsRole(RoleEntity role) {
    return childRoles.contains(role);
  }

  /**
   * Adds a child role to this composite role
   */
  public void addChildRole(RoleEntity childRole) {
    if (!this.composite) {
      this.composite = true;
    }
    this.childRoles.add(childRole);
    childRole.getParentRoles().add(this);
  }

  /**
   * Removes a child role from this composite role
   */
  public void removeChildRole(RoleEntity childRole) {
    this.childRoles.remove(childRole);
    childRole.getParentRoles().remove(this);

    // If no more children, mark as non-composite
    if (this.childRoles.isEmpty()) {
      this.composite = false;
    }
  }

  /**
   * Gets all roles recursively (this role + all descendants)
   */
  public Set<RoleEntity> getAllRolesRecursive() {
    Set<RoleEntity> allRoles = new HashSet<>();
    allRoles.add(this);

    for (RoleEntity child : childRoles) {
      allRoles.addAll(child.getAllRolesRecursive());
    }

    return allRoles;
  }

  /**
   * Role type enum
   */
  public enum RoleType {
    REALM, // Realm-level role
    CLIENT // Client-specific role
  }
}
