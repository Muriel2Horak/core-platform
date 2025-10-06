package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity @Table(name = "users_directory") @Data @EqualsAndHashCode(callSuper = true, exclude = {
    "roles", "groups" }) @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class UserDirectoryEntity extends MultiTenantEntity {

  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "keycloak_user_id")
  private String keycloakUserId;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "email")
  private String email;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "is_federated", nullable = false) @Builder.Default
  private Boolean isFederated = false;

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "manager_id")
  private UserDirectoryEntity manager;

  @Column(name = "status") @Builder.Default
  private String status = "ACTIVE";

  // New columns for Keycloak sync
  @Column(name = "active", nullable = false) @Builder.Default
  private Boolean active = true;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "roles_json", columnDefinition = "TEXT")
  private String rolesJson;

  @Column(name = "groups_json", columnDefinition = "TEXT")
  private String groupsJson;

  // Additional user attributes
  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "department")
  private String department;

  @Column(name = "title")
  private String title;

  // ✅ OPRAVENO: Rozšířené organizační atributy (bez konfliktu manager)
  @Column(name = "position")
  private String position;

  @Column(name = "manager_username")
  private String managerUsername; // ✅ Přejmenováno aby nekonflikovalo s manager entitou

  @Column(name = "cost_center")
  private String costCenter;

  @Column(name = "location")
  private String location;

  @Column(name = "phone")
  private String phone;

  // ✅ NOVÉ: Zástupství
  @Column(name = "deputy_username")
  private String deputy;

  @Column(name = "deputy_from")
  private java.time.LocalDate deputyFrom;

  @Column(name = "deputy_to")
  private java.time.LocalDate deputyTo;

  @Column(name = "deputy_reason")
  private String deputyReason;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // ✅ V5 NOVÉ: Many-to-Many relationship s rolemi
  @ManyToMany @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id")) @Builder.Default
  private Set<RoleEntity> roles = new HashSet<>();

  // ✅ V5 NOVÉ: Many-to-Many relationship se skupinami
  @ManyToMany @JoinTable(name = "user_groups", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "group_id")) @Builder.Default
  private Set<GroupEntity> groups = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Helper methods
  public boolean isActiveUser() {
    return active != null && active && deletedAt == null;
  }

  public void setActive(Boolean active) {
    this.active = active;
    // Clear deleted_at when activating user
    if (Boolean.TRUE.equals(active)) {
      this.deletedAt = null;
    }
  }
}
