package cz.muriel.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "users_directory") @Data @EqualsAndHashCode(callSuper = true) @NoArgsConstructor @AllArgsConstructor @SuperBuilder
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

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
