package cz.muriel.core.service;

import cz.muriel.core.entity.GroupEntity;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.GroupRepository;
import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 👥 Group Service - Business logic for application groups
 * 
 * Groups are synchronized from Keycloak, so this service primarily provides
 * read operations. Write operations should go through Keycloak sync.
 */
@Slf4j @Service @RequiredArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;

  /**
   * Get all groups for current tenant
   */
  @Transactional(readOnly = true)
  public List<GroupEntity> getAllGroups() {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      log.warn("No tenant context found when getting groups");
      return List.of();
    }

    UUID tenantId = Tenant.generateUuidFromKey(tenantKey);
    log.debug("Getting all groups for tenant: {}", tenantKey);
    return groupRepository.findByTenantId(tenantId);
  }

  /**
   * Get all root groups (top-level groups without parent) for current tenant
   */
  @Transactional(readOnly = true)
  public List<GroupEntity> getRootGroups() {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      log.warn("No tenant context found when getting root groups");
      return List.of();
    }

    UUID tenantId = Tenant.generateUuidFromKey(tenantKey);
    log.debug("Getting root groups for tenant: {}", tenantKey);
    return groupRepository.findByTenantIdAndParentGroupIsNull(tenantId);
  }

  /**
   * Get group by ID
   */
  @Transactional(readOnly = true)
  public Optional<GroupEntity> getGroupById(UUID id) {
    log.debug("Getting group by ID: {}", id);
    return groupRepository.findById(id);
  }

  /**
   * Get group by name
   */
  @Transactional(readOnly = true)
  public Optional<GroupEntity> getGroupByName(String name) {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      return Optional.empty();
    }

    UUID tenantId = Tenant.generateUuidFromKey(tenantKey);
    List<GroupEntity> groups = groupRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId,
        name);

    // Find exact match
    return groups.stream().filter(g -> g.getName().equals(name)).findFirst();
  }

  /**
   * Get child groups of a parent group
   */
  @Transactional(readOnly = true)
  public List<GroupEntity> getChildGroups(GroupEntity parentGroup) {
    log.debug("Getting child groups for: {}", parentGroup.getName());
    return groupRepository.findByParentGroup(parentGroup);
  }

  /**
   * Search groups by name pattern
   */
  @Transactional(readOnly = true)
  public List<GroupEntity> searchGroupsByName(String namePattern) {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      return List.of();
    }

    UUID tenantId = Tenant.generateUuidFromKey(tenantKey);
    log.debug("Searching groups by name pattern: {} for tenant: {}", namePattern, tenantKey);
    return groupRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, namePattern);
  }

  /**
   * Create a new group
   */
  @Transactional
  public GroupEntity createGroup(GroupEntity group) {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      throw new IllegalStateException("No tenant context available");
    }

    UUID tenantId = Tenant.generateUuidFromKey(tenantKey);
    group.setTenantId(tenantId);

    log.info("Creating new group: {} for tenant: {}", group.getName(), tenantKey);
    return groupRepository.save(group);
  }

  /**
   * Update existing group
   */
  @Transactional
  public GroupEntity updateGroup(UUID id, GroupEntity updatedGroup) {
    GroupEntity existingGroup = groupRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));

    existingGroup.setName(updatedGroup.getName());
    existingGroup.setPath(updatedGroup.getPath());
    existingGroup.setParentGroup(updatedGroup.getParentGroup());
    existingGroup.setKeycloakGroupId(updatedGroup.getKeycloakGroupId());

    log.info("Updating group: {}", id);
    return groupRepository.save(existingGroup);
  }

  /**
   * Delete group
   */
  @Transactional
  public void deleteGroup(UUID id) {
    log.info("Deleting group: {}", id);
    groupRepository.deleteById(id);
  }
}
