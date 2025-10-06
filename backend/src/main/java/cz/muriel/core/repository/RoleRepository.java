package cz.muriel.core.repository;

import cz.muriel.core.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * 🎭 Repository for Role entities synchronized from Keycloak
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

  /**
   * Find role by Keycloak role ID and tenant
   */
  Optional<RoleEntity> findByKeycloakRoleIdAndTenantKey(String keycloakRoleId, String tenantKey);

  /**
   * Find role by name and tenant
   */
  Optional<RoleEntity> findByNameAndTenantKey(String name, String tenantKey);

  /**
   * Find all roles for a tenant
   */
  List<RoleEntity> findByTenantKey(String tenantKey);

  /**
   * Find all roles of specific type for a tenant
   */
  List<RoleEntity> findByTenantKeyAndRoleType(String tenantKey, RoleEntity.RoleType roleType);

  /**
   * Check if role exists by Keycloak ID
   */
  boolean existsByKeycloakRoleIdAndTenantKey(String keycloakRoleId, String tenantKey);

  /**
   * Delete role by Keycloak ID and tenant
   */
  void deleteByKeycloakRoleIdAndTenantKey(String keycloakRoleId, String tenantKey);
}
