package cz.muriel.core.repository;

import cz.muriel.core.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 🎭 Repository for RoleEntity with support for composite role hierarchies
 */
@Repository
public interface RoleEntityRepository extends JpaRepository<RoleEntity, UUID> {

  // =====================================================
  // 🔍 BASIC LOOKUPS
  // =====================================================

  /**
   * Find role by Keycloak Role ID and tenant
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
   * Find roles by type and tenant
   */
  List<RoleEntity> findByRoleTypeAndTenantKey(RoleEntity.RoleType roleType, String tenantKey);

  /**
   * Check if role exists by Keycloak ID and tenant
   */
  boolean existsByKeycloakRoleIdAndTenantKey(String keycloakRoleId, String tenantKey);

  // =====================================================
  // 🎭 COMPOSITE ROLE QUERIES
  // =====================================================

  /**
   * Find all composite roles (roles that contain other roles)
   */
  List<RoleEntity> findByCompositeAndTenantKey(Boolean composite, String tenantKey);

  /**
   * Find all root roles (roles with no parents)
   */
  @Query("SELECT r FROM RoleEntity r WHERE r.tenantKey = :tenantKey AND SIZE(r.parentRoles) = 0")
  List<RoleEntity> findRootRoles(@Param("tenantKey") String tenantKey);

  /**
   * Find all child roles of a parent role
   */
  @Query("SELECT DISTINCT child FROM RoleEntity parent " + "JOIN parent.childRoles child "
      + "WHERE parent.id = :parentId AND parent.tenantKey = :tenantKey")
  List<RoleEntity> findChildRoles(@Param("parentId") UUID parentId,
      @Param("tenantKey") String tenantKey);

  /**
   * Find all parent roles of a child role
   */
  @Query("SELECT DISTINCT parent FROM RoleEntity child " + "JOIN child.parentRoles parent "
      + "WHERE child.id = :childId AND child.tenantKey = :tenantKey")
  List<RoleEntity> findParentRoles(@Param("childId") UUID childId,
      @Param("tenantKey") String tenantKey);

  // =====================================================
  // 📊 STATISTICS & SEARCH
  // =====================================================

  /**
   * Count roles by tenant
   */
  long countByTenantKey(String tenantKey);

  /**
   * Count composite roles by tenant
   */
  long countByCompositeAndTenantKey(Boolean composite, String tenantKey);

  /**
   * Search roles by name pattern
   */
  @Query("SELECT r FROM RoleEntity r " + "WHERE r.tenantKey = :tenantKey "
      + "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) "
      + "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
  List<RoleEntity> searchRoles(@Param("query") String query, @Param("tenantKey") String tenantKey);
}
