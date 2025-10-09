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
  Optional<RoleEntity> findByKeycloakRoleIdAndTenantId(String keycloakRoleId, UUID tenantId);

  /**
   * Find role by name and tenant
   */
  Optional<RoleEntity> findByNameAndTenantId(String name, UUID tenantId);

  /**
   * Find all roles for a tenant
   */
  List<RoleEntity> findByTenantId(UUID tenantId);

  /**
   * Find all roles of specific type for a tenant
   */
  List<RoleEntity> findByTenantIdAndRoleType(UUID tenantId, RoleEntity.RoleType roleType);

  /**
   * Check if role exists by Keycloak ID
   */
  boolean existsByKeycloakRoleIdAndTenantId(String keycloakRoleId, UUID tenantId);

  /**
   * Delete role by Keycloak ID and tenant
   */
  void deleteByKeycloakRoleIdAndTenantId(String keycloakRoleId, UUID tenantId);
}
