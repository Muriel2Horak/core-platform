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
    Optional<GroupEntity> findByKeycloakGroupIdAndTenantId(String keycloakGroupId, UUID tenantId);

    /**
     * Find group by name and tenant
     */
    Optional<GroupEntity> findByNameAndTenantId(String name, UUID tenantId);

    /**
     * Find group by path and tenant
     */
    Optional<GroupEntity> findByPathAndTenantId(String path, UUID tenantId);

    /**
     * Find all groups for a tenant
     */
    List<GroupEntity> findByTenantId(UUID tenantId);

    /**
     * Check if group exists by Keycloak ID and tenant
     */
    boolean existsByKeycloakGroupIdAndTenantId(String keycloakGroupId, UUID tenantId);

    // =====================================================
    // 🌲 HIERARCHICAL QUERIES
    // =====================================================

    /**
     * Find all root groups (groups with no parent)
     */
    @Query("SELECT g FROM GroupEntity g WHERE g.tenantId = :tenantId AND g.parentGroup IS NULL")
    List<GroupEntity> findRootGroups(@Param("tenantId") UUID tenantId);

    /**
     * Find direct children of a parent group
     */
    List<GroupEntity> findByParentGroupIdAndTenantId(UUID parentGroupId, UUID tenantId);

    /**
     * Find all groups at specific depth level
     */
    @Query("SELECT g FROM GroupEntity g WHERE g.tenantId = :tenantId "
            + "AND LENGTH(g.path) - LENGTH(REPLACE(g.path, '/', '')) = :level + 1")
    List<GroupEntity> findByLevel(@Param("level") int level, @Param("tenantId") UUID tenantId);

    /**
     * Find all descendants of a group (using path prefix)
     */
    @Query("SELECT g FROM GroupEntity g WHERE g.tenantId = :tenantId "
            + "AND g.path LIKE CONCAT(:parentPath, '/%')")
    List<GroupEntity> findDescendants(@Param("parentPath") String parentPath,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all groups in a path hierarchy Example: "/admin/users" returns [admin,
     * admin/users]
     */
    @Query("SELECT g FROM GroupEntity g WHERE g.tenantId = :tenantId "
            + "AND :path LIKE CONCAT(g.path, '%')")
    List<GroupEntity> findPathHierarchy(@Param("path") String path,
            @Param("tenantId") UUID tenantId);

    // =====================================================
    // 📊 STATISTICS & SEARCH
    // =====================================================

    /**
     * Count groups by tenant
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count root groups
     */
    @Query("SELECT COUNT(g) FROM GroupEntity g WHERE g.tenantId = :tenantId AND g.parentGroup IS NULL")
    long countRootGroups(@Param("tenantId") UUID tenantId);

    /**
     * Search groups by name pattern
     */
    @Query("SELECT g FROM GroupEntity g " + "WHERE g.tenantId = :tenantId "
            + "AND LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<GroupEntity> searchGroups(@Param("query") String query, @Param("tenantId") UUID tenantId);

    /**
     * Find groups containing specific users (by path pattern)
     */
    @Query("SELECT DISTINCT g FROM GroupEntity g " + "JOIN g.users u "
            + "WHERE g.tenantId = :tenantId AND u.id = :userId")
    List<GroupEntity> findGroupsByUserId(@Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId);
}
