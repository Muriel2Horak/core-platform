package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 📁 Group entity - synchronized from Keycloak Supports hierarchical group
 * structure with path-based navigation
 */
@Entity @Table(name = "groups", uniqueConstraints = {
    @UniqueConstraint(name = "uk_group_keycloak_tenant", columnNames = { "keycloak_group_id",
        "tenant_key" }),
    @UniqueConstraint(name = "uk_group_path_tenant", columnNames = { "path",
        "tenant_key" }) }) @Data @EqualsAndHashCode(callSuper = true, exclude = { "parentGroup",
            "subGroups", "users" }) @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class GroupEntity extends MultiTenantEntity {

  @Id
  private UUID id;

  @Column(name = "keycloak_group_id", nullable = false)
  private String keycloakGroupId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "path", nullable = false)
  private String path;

  // Self-referencing relationship for hierarchy
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_group_id")
  private GroupEntity parentGroup;

  @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default
  private Set<GroupEntity> subGroups = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  // Many-to-Many relationship with users
  @ManyToMany(mappedBy = "groups") @Builder.Default
  private Set<UserDirectoryEntity> users = new HashSet<>();

  /**
   * 🔐 DETERMINISTICKÉ UUID GENERATION Generates consistent UUID from
   * keycloakGroupId + tenantKey
   */
  @PrePersist
  protected void onCreate() {
    if (id == null && keycloakGroupId != null && getTenantKey() != null) {
      id = generateUuidFromKeycloakId(keycloakGroupId, getTenantKey());
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
   * 🔐 STATIC HELPER: Generate deterministic UUID from Keycloak ID + Tenant
   */
  public static UUID generateUuidFromKeycloakId(String keycloakGroupId, String tenantKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String composite = "group:" + tenantKey + ":" + keycloakGroupId;
      byte[] hash = digest.digest(composite.getBytes(StandardCharsets.UTF_8));

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
  // ✅ V5 UTILITY METHODS: Hierarchical Navigation
  // =====================================================

  /**
   * Check if this is a root-level group (no parent)
   */
  public boolean isRootGroup() {
    return parentGroup == null;
  }

  /**
   * Get the depth level in hierarchy (0 = root)
   */
  public int getLevel() {
    if (path == null)
      return 0;
    return (int) path.chars().filter(ch -> ch == '/').count() - 1;
  }

  /**
   * Checks if this group is a descendant of another group Example: /admin/users
   * is descendant of /admin
   */
  public boolean isDescendantOf(GroupEntity ancestor) {
    if (ancestor == null || this.path == null || ancestor.getPath() == null) {
      return false;
    }
    return this.path.startsWith(ancestor.getPath() + "/");
  }

  /**
   * Checks if this group is an ancestor of another group
   */
  public boolean isAncestorOf(GroupEntity descendant) {
    if (descendant == null) {
      return false;
    }
    return descendant.isDescendantOf(this);
  }

  /**
   * Gets the full path hierarchy as list of group names Example:
   * "/admin/users/managers" → ["admin", "users", "managers"]
   */
  public List<String> getPathHierarchy() {
    if (path == null || path.isEmpty() || path.equals("/")) {
      return new ArrayList<>();
    }

    String[] parts = path.substring(1).split("/"); // Remove leading "/"
    return List.of(parts);
  }

  /**
   * Gets all ancestor groups recursively
   */
  public Set<GroupEntity> getAllAncestors() {
    Set<GroupEntity> ancestors = new HashSet<>();
    GroupEntity current = this.parentGroup;

    while (current != null) {
      ancestors.add(current);
      current = current.getParentGroup();
    }

    return ancestors;
  }

  /**
   * Gets all descendant groups recursively (depth-first)
   */
  public Set<GroupEntity> getAllDescendants() {
    Set<GroupEntity> descendants = new HashSet<>();

    for (GroupEntity subGroup : subGroups) {
      descendants.add(subGroup);
      descendants.addAll(subGroup.getAllDescendants());
    }

    return descendants;
  }

  /**
   * Adds a subgroup to this group
   */
  public void addSubGroup(GroupEntity subGroup) {
    this.subGroups.add(subGroup);
    subGroup.setParentGroup(this);
  }

  /**
   * Removes a subgroup from this group
   */
  public void removeSubGroup(GroupEntity subGroup) {
    this.subGroups.remove(subGroup);
    subGroup.setParentGroup(null);
  }
}
