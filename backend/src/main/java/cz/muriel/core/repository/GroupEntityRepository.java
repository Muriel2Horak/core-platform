package cz.muriel.core.repository;

import cz.muriel.core.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 📁 Repository for GroupEntity with hierarchical queries
 */
@Repository
public interface GroupEntityRepository extends JpaRepository<GroupEntity, UUID> {

  // =====================================================
  // 🔍 BASIC LOOKUPS
  // =====================================================

  /**
   * Find group by Keycloak Group ID and tenant
   */
  Optional<GroupEntity> findByKeycloakGroupIdAndTenantKey(String keycloakGroupId, String tenantKey);

  /**
   * Find group by name and tenant
   */
  Optional<GroupEntity> findByNameAndTenantKey(String name, String tenantKey);

  /**
   * Find group by path and tenant
   */
  Optional<GroupEntity> findByPathAndTenantKey(String path, String tenantKey);

  /**
   * Find all groups for a tenant
   */
  List<GroupEntity> findByTenantKey(String tenantKey);

  /**
   * Check if group exists by Keycloak ID and tenant
   */
  boolean existsByKeycloakGroupIdAndTenantKey(String keycloakGroupId, String tenantKey);

  // =====================================================
  // 🌲 HIERARCHICAL QUERIES
  // =====================================================

  /**
   * Find all root groups (groups with no parent)
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantKey = :tenantKey AND g.parentGroup IS NULL")
  List<GroupEntity> findRootGroups(@Param("tenantKey") String tenantKey);

  /**
   * Find direct children of a parent group
   */
  List<GroupEntity> findByParentGroupIdAndTenantKey(UUID parentGroupId, String tenantKey);

  /**
   * Find all groups at specific depth level
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantKey = :tenantKey "
      + "AND LENGTH(g.path) - LENGTH(REPLACE(g.path, '/', '')) = :level + 1")
  List<GroupEntity> findByLevel(@Param("level") int level, @Param("tenantKey") String tenantKey);

  /**
   * Find all descendants of a group (using path prefix)
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantKey = :tenantKey "
      + "AND g.path LIKE CONCAT(:parentPath, '/%')")
  List<GroupEntity> findDescendants(@Param("parentPath") String parentPath,
      @Param("tenantKey") String tenantKey);

  /**
   * Find all groups in a path hierarchy Example: "/admin/users" returns [admin,
   * admin/users]
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantKey = :tenantKey "
      + "AND :path LIKE CONCAT(g.path, '%')")
  List<GroupEntity> findPathHierarchy(@Param("path") String path,
      @Param("tenantKey") String tenantKey);

  // =====================================================
  // 📊 STATISTICS & SEARCH
  // =====================================================

  /**
   * Count groups by tenant
   */
  long countByTenantKey(String tenantKey);

  /**
   * Count root groups
   */
  @Query("SELECT COUNT(g) FROM GroupEntity g WHERE g.tenantKey = :tenantKey AND g.parentGroup IS NULL")
  long countRootGroups(@Param("tenantKey") String tenantKey);

  /**
   * Search groups by name pattern
   */
  @Query("SELECT g FROM GroupEntity g " + "WHERE g.tenantKey = :tenantKey "
      + "AND LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<GroupEntity> searchGroups(@Param("query") String query,
      @Param("tenantKey") String tenantKey);

  /**
   * Find groups containing specific users (by path pattern)
   */
  @Query("SELECT DISTINCT g FROM GroupEntity g " + "JOIN g.users u "
      + "WHERE g.tenantKey = :tenantKey AND u.id = :userId")
  List<GroupEntity> findGroupsByUserId(@Param("userId") UUID userId,
      @Param("tenantKey") String tenantKey);
}
