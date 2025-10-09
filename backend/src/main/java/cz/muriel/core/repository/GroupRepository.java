package cz.muriel.core.repository;

import cz.muriel.core.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * 📁 Repository for Group entities synchronized from Keycloak
 */
@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {

  /**
   * Find group by Keycloak group ID and tenant
   */
  Optional<GroupEntity> findByKeycloakGroupIdAndTenantId(String keycloakGroupId, UUID tenantId);

  /**
   * Find group by path and tenant
   */
  Optional<GroupEntity> findByPathAndTenantId(String path, UUID tenantId);

  /**
   * Find all groups for a tenant
   */
  List<GroupEntity> findByTenantId(UUID tenantId);

  /**
   * Find root groups (no parent) for a tenant
   */
  List<GroupEntity> findByTenantIdAndParentGroupIsNull(UUID tenantId);

  /**
   * Find child groups of a parent group
   */
  List<GroupEntity> findByParentGroup(GroupEntity parentGroup);

  /**
   * Find groups by name pattern (case-insensitive)
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantId = :tenantId AND LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  List<GroupEntity> findByTenantIdAndNameContainingIgnoreCase(@Param("tenantId") UUID tenantId,
      @Param("name") String name);

  /**
   * Check if group exists by Keycloak ID
   */
  boolean existsByKeycloakGroupIdAndTenantId(String keycloakGroupId, UUID tenantId);

  /**
   * Delete group by Keycloak ID and tenant
   */
  void deleteByKeycloakGroupIdAndTenantId(String keycloakGroupId, UUID tenantId);
}
