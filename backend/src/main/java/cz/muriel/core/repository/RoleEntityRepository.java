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
     * Find roles by type and tenant
     */
    List<RoleEntity> findByRoleTypeAndTenantId(RoleEntity.RoleType roleType, UUID tenantId);

    /**
     * Check if role exists by Keycloak ID and tenant
     */
    boolean existsByKeycloakRoleIdAndTenantId(String keycloakRoleId, UUID tenantId);

    // =====================================================
    // 🎭 COMPOSITE ROLE QUERIES
    // =====================================================

    /**
     * Find all composite roles (roles that contain other roles)
     */
    List<RoleEntity> findByCompositeAndTenantId(Boolean composite, UUID tenantId);

    /**
     * Find all root roles (roles with no parents)
     */
    @Query("SELECT r FROM RoleEntity r WHERE r.tenantId = :tenantId AND SIZE(r.parentRoles) = 0")
    List<RoleEntity> findRootRoles(@Param("tenantId") UUID tenantId);

    /**
     * Find all child roles of a parent role
     */
    @Query("SELECT DISTINCT child FROM RoleEntity parent " + "JOIN parent.childRoles child "
            + "WHERE parent.id = :parentId AND parent.tenantId = :tenantId")
    List<RoleEntity> findChildRoles(@Param("parentId") UUID parentId,
            @Param("tenantId") UUID tenantId);

    /**
     * Find all parent roles of a child role
     */
    @Query("SELECT DISTINCT parent FROM RoleEntity child " + "JOIN child.parentRoles parent "
            + "WHERE child.id = :childId AND child.tenantId = :tenantId")
    List<RoleEntity> findParentRoles(@Param("childId") UUID childId,
            @Param("tenantId") UUID tenantId);

    // =====================================================
    // 📊 STATISTICS & SEARCH
    // =====================================================

    /**
     * Count roles by tenant
     */
    long countByTenantId(UUID tenantId);

    /**
     * Count composite roles by tenant
     */
    long countByCompositeAndTenantId(Boolean composite, UUID tenantId);

    /**
     * Search roles by name pattern
     */
    @Query("SELECT r FROM RoleEntity r " + "WHERE r.tenantId = :tenantId "
            + "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<RoleEntity> searchRoles(@Param("query") String query, @Param("tenantId") UUID tenantId);
}
