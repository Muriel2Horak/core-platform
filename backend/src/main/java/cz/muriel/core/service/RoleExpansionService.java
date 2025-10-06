package cz.muriel.core.service;

import cz.muriel.core.entity.RoleEntity;
import cz.muriel.core.repository.RoleEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎭 SERVICE: Role Expansion & Hierarchy Management Handles composite role
 * resolution and flattening
 */
@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class RoleExpansionService {

  private final RoleEntityRepository roleRepository;

  // =====================================================
  // 🔍 COMPOSITE ROLE EXPANSION
  // =====================================================

  /**
   * Expands a set of roles to include all composite child roles recursively
   * Example: User has [ADMIN] → returns [ADMIN, MANAGER, USER]
   */
  public Set<RoleEntity> expandRoles(Set<RoleEntity> roles) {
    Set<RoleEntity> expandedRoles = new HashSet<>();

    for (RoleEntity role : roles) {
      expandedRoles.addAll(expandRoleRecursive(role));
    }

    log.debug("🎭 Expanded {} roles to {} total roles", roles.size(), expandedRoles.size());
    return expandedRoles;
  }

  /**
   * Expands a single role recursively
   */
  private Set<RoleEntity> expandRoleRecursive(RoleEntity role) {
    Set<RoleEntity> result = new HashSet<>();
    result.add(role);

    if (role.getComposite() && role.getChildRoles() != null) {
      for (RoleEntity childRole : role.getChildRoles()) {
        result.addAll(expandRoleRecursive(childRole));
      }
    }

    return result;
  }

  /**
   * Gets all effective role names for a user (flattened)
   */
  public Set<String> getEffectiveRoleNames(Set<RoleEntity> userRoles) {
    Set<RoleEntity> expanded = expandRoles(userRoles);
    return expanded.stream().map(RoleEntity::getName).collect(Collectors.toSet());
  }

  /**
   * Checks if a user has a specific role (including through composite roles)
   */
  public boolean hasEffectiveRole(Set<RoleEntity> userRoles, String roleName) {
    Set<String> effectiveRoles = getEffectiveRoleNames(userRoles);
    return effectiveRoles.contains(roleName);
  }

  // =====================================================
  // 🌳 HIERARCHY QUERIES
  // =====================================================

  /**
   * Gets the complete role hierarchy tree for a tenant
   */
  public List<RoleHierarchyNode> getRoleHierarchyTree(String tenantKey) {
    List<RoleEntity> rootRoles = roleRepository.findRootRoles(tenantKey);

    return rootRoles.stream().map(this::buildHierarchyNode).collect(Collectors.toList());
  }

  /**
   * Builds a hierarchy node with all children recursively
   */
  private RoleHierarchyNode buildHierarchyNode(RoleEntity role) {
    RoleHierarchyNode node = new RoleHierarchyNode();
    node.setId(role.getId());
    node.setName(role.getName());
    node.setDescription(role.getDescription());
    node.setComposite(role.getComposite());
    node.setRoleType(role.getRoleType().name());

    if (role.getComposite() && role.getChildRoles() != null) {
      List<RoleHierarchyNode> children = role.getChildRoles().stream().map(this::buildHierarchyNode)
          .sorted(Comparator.comparing(RoleHierarchyNode::getName)).collect(Collectors.toList());
      node.setChildren(children);
    } else {
      node.setChildren(new ArrayList<>());
    }

    return node;
  }

  /**
   * Gets all parent roles of a given role
   */
  public Set<RoleEntity> getParentRoles(RoleEntity role) {
    return new HashSet<>(role.getParentRoles());
  }

  /**
   * Gets all child roles of a given role (direct only)
   */
  public Set<RoleEntity> getChildRoles(RoleEntity role) {
    return new HashSet<>(role.getChildRoles());
  }

  /**
   * Gets all roles that this role contains (recursively)
   */
  public Set<RoleEntity> getAllContainedRoles(RoleEntity role) {
    return role.getAllRolesRecursive();
  }

  // =====================================================
  // 🔧 MANAGEMENT OPERATIONS
  // =====================================================

  /**
   * Synchronizes composite role relationships from Keycloak
   */
  @Transactional
  public void syncCompositeRelationships(RoleEntity parentRole, Set<String> childRoleNames,
      String tenantKey) {
    log.debug("🔄 Syncing composite relationships for role: {}", parentRole.getName());

    // Clear existing relationships
    Set<RoleEntity> currentChildren = new HashSet<>(parentRole.getChildRoles());
    for (RoleEntity child : currentChildren) {
      parentRole.removeChildRole(child);
    }

    // Add new relationships
    for (String childRoleName : childRoleNames) {
      Optional<RoleEntity> childRole = roleRepository.findByNameAndTenantKey(childRoleName,
          tenantKey);
      childRole.ifPresent(parentRole::addChildRole);
    }

    roleRepository.save(parentRole);
    log.info("✅ Synced {} child roles for {}", childRoleNames.size(), parentRole.getName());
  }

  /**
   * Detects circular dependencies in role hierarchy
   */
  public boolean hasCircularDependency(RoleEntity role) {
    return detectCircular(role, new HashSet<>());
  }

  private boolean detectCircular(RoleEntity role, Set<UUID> visited) {
    if (visited.contains(role.getId())) {
      log.warn("⚠️ Circular dependency detected in role: {}", role.getName());
      return true;
    }

    visited.add(role.getId());

    for (RoleEntity child : role.getChildRoles()) {
      if (detectCircular(child, new HashSet<>(visited))) {
        return true;
      }
    }

    return false;
  }

  // =====================================================
  // 📊 DTO CLASSES
  // =====================================================

  /**
   * DTO for role hierarchy tree representation
   */
  @lombok.Data
  public static class RoleHierarchyNode {
    private UUID id;
    private String name;
    private String description;
    private Boolean composite;
    private String roleType;
    private List<RoleHierarchyNode> children;
  }
}
