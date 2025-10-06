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
  Optional<GroupEntity> findByKeycloakGroupIdAndTenantKey(String keycloakGroupId, String tenantKey);

  /**
   * Find group by path and tenant
   */
  Optional<GroupEntity> findByPathAndTenantKey(String path, String tenantKey);

  /**
   * Find all groups for a tenant
   */
  List<GroupEntity> findByTenantKey(String tenantKey);

  /**
   * Find root groups (no parent) for a tenant
   */
  List<GroupEntity> findByTenantKeyAndParentGroupIsNull(String tenantKey);

  /**
   * Find child groups of a parent group
   */
  List<GroupEntity> findByParentGroup(GroupEntity parentGroup);

  /**
   * Find groups by name pattern (case-insensitive)
   */
  @Query("SELECT g FROM GroupEntity g WHERE g.tenantKey = :tenantKey AND LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))")
  List<GroupEntity> findByTenantKeyAndNameContainingIgnoreCase(@Param("tenantKey") String tenantKey,
      @Param("name") String name);

  /**
   * Check if group exists by Keycloak ID
   */
  boolean existsByKeycloakGroupIdAndTenantKey(String keycloakGroupId, String tenantKey);

  /**
   * Delete group by Keycloak ID and tenant
   */
  void deleteByKeycloakGroupIdAndTenantKey(String keycloakGroupId, String tenantKey);
}
