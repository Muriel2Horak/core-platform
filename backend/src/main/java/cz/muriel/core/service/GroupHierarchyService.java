package cz.muriel.core.service;

import cz.muriel.core.entity.GroupEntity;
import cz.muriel.core.repository.GroupEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 📁 SERVICE: Group Hierarchy Management Handles hierarchical group navigation
 * and path-based queries
 */
@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class GroupHierarchyService {

  private final GroupEntityRepository groupRepository;

  // =====================================================
  // 🌳 HIERARCHY NAVIGATION
  // =====================================================

  /**
   * Gets the complete group hierarchy tree for a tenant
   */
  public List<GroupHierarchyNode> getGroupHierarchyTree(String tenantKey) {
    List<GroupEntity> rootGroups = groupRepository.findRootGroups(tenantKey);

    return rootGroups.stream().map(this::buildHierarchyNode)
        .sorted(Comparator.comparing(GroupHierarchyNode::getName)).collect(Collectors.toList());
  }

  /**
   * Builds a hierarchy node with all children recursively
   */
  private GroupHierarchyNode buildHierarchyNode(GroupEntity group) {
    GroupHierarchyNode node = new GroupHierarchyNode();
    node.setId(group.getId());
    node.setName(group.getName());
    node.setPath(group.getPath());
    node.setLevel(group.getLevel());
    node.setIsRoot(group.isRootGroup());

    if (group.getSubGroups() != null && !group.getSubGroups().isEmpty()) {
      List<GroupHierarchyNode> children = group.getSubGroups().stream()
          .map(this::buildHierarchyNode).sorted(Comparator.comparing(GroupHierarchyNode::getName))
          .collect(Collectors.toList());
      node.setChildren(children);
    } else {
      node.setChildren(new ArrayList<>());
    }

    return node;
  }

  /**
   * Gets all ancestor groups of a group (path-based)
   */
  public List<GroupEntity> getAncestors(GroupEntity group) {
    List<GroupEntity> ancestors = new ArrayList<>(group.getAllAncestors());
    ancestors.sort(Comparator.comparing(GroupEntity::getLevel));
    return ancestors;
  }

  /**
   * Gets all descendant groups of a group (recursive)
   */
  public List<GroupEntity> getDescendants(GroupEntity group) {
    return new ArrayList<>(group.getAllDescendants());
  }

  /**
   * Gets the breadcrumb path for a group Example: /admin/users/managers → [admin,
   * users, managers]
   */
  public List<GroupBreadcrumb> getBreadcrumbPath(GroupEntity group, String tenantKey) {
    List<String> pathParts = group.getPathHierarchy();
    List<GroupBreadcrumb> breadcrumbs = new ArrayList<>();

    StringBuilder currentPath = new StringBuilder();
    for (String part : pathParts) {
      currentPath.append("/").append(part);
      String path = currentPath.toString();

      Optional<GroupEntity> groupAtPath = groupRepository.findByPathAndTenantKey(path, tenantKey);
      groupAtPath.ifPresent(g -> {
        GroupBreadcrumb breadcrumb = new GroupBreadcrumb();
        breadcrumb.setId(g.getId());
        breadcrumb.setName(g.getName());
        breadcrumb.setPath(g.getPath());
        breadcrumbs.add(breadcrumb);
      });
    }

    return breadcrumbs;
  }

  // =====================================================
  // 🔍 PATH-BASED QUERIES
  // =====================================================

  /**
   * Finds groups at specific depth level
   */
  public List<GroupEntity> getGroupsByLevel(int level, String tenantKey) {
    return groupRepository.findByLevel(level, tenantKey);
  }

  /**
   * Checks if group1 is ancestor of group2
   */
  public boolean isAncestor(GroupEntity group1, GroupEntity group2) {
    return group1.isAncestorOf(group2);
  }

  /**
   * Checks if group1 is descendant of group2
   */
  public boolean isDescendant(GroupEntity group1, GroupEntity group2) {
    return group1.isDescendantOf(group2);
  }

  /**
   * Gets all groups that share the same parent
   */
  public List<GroupEntity> getSiblings(GroupEntity group, String tenantKey) {
    if (group.isRootGroup()) {
      // Root groups - get all other root groups
      return groupRepository.findRootGroups(tenantKey).stream()
          .filter(g -> !g.getId().equals(group.getId())).collect(Collectors.toList());
    } else {
      // Non-root - get siblings from same parent
      return groupRepository
          .findByParentGroupIdAndTenantKey(group.getParentGroup().getId(), tenantKey).stream()
          .filter(g -> !g.getId().equals(group.getId())).collect(Collectors.toList());
    }
  }

  // =====================================================
  // 🔧 MANAGEMENT OPERATIONS
  // =====================================================

  /**
   * Synchronizes group hierarchy from Keycloak
   */
  @Transactional
  public void syncGroupHierarchy(GroupEntity group, GroupEntity parent) {
    log.debug("🔄 Syncing group hierarchy: {} under parent: {}", group.getName(),
        parent != null ? parent.getName() : "ROOT");

    group.setParentGroup(parent);

    // Update path based on parent
    if (parent == null) {
      group.setPath("/" + group.getName());
    } else {
      group.setPath(parent.getPath() + "/" + group.getName());
    }

    groupRepository.save(group);
    log.info("✅ Synced group hierarchy for: {}", group.getName());
  }

  /**
   * Moves a group to a new parent (re-parents)
   */
  @Transactional
  public void moveGroup(GroupEntity group, GroupEntity newParent, String tenantKey) {
    log.info("📦 Moving group {} to new parent {}", group.getName(),
        newParent != null ? newParent.getName() : "ROOT");

    // Validate: Cannot move group under its own descendant
    if (newParent != null && newParent.isDescendantOf(group)) {
      throw new IllegalArgumentException(
          "Cannot move group under its own descendant: " + newParent.getName());
    }

    // Update parent relationship
    if (group.getParentGroup() != null) {
      group.getParentGroup().removeSubGroup(group);
    }

    if (newParent != null) {
      newParent.addSubGroup(group);
    } else {
      group.setParentGroup(null);
    }

    // Recalculate paths for this group and all descendants
    recalculatePaths(group, newParent);

    groupRepository.save(group);
    log.info("✅ Group moved successfully");
  }

  /**
   * Recalculates paths for group and all descendants
   */
  private void recalculatePaths(GroupEntity group, GroupEntity newParent) {
    String newPath;
    if (newParent == null) {
      newPath = "/" + group.getName();
    } else {
      newPath = newParent.getPath() + "/" + group.getName();
    }

    group.setPath(newPath);

    // Recursively update all descendants
    for (GroupEntity child : group.getSubGroups()) {
      recalculatePaths(child, group);
    }
  }

  /**
   * Validates group hierarchy integrity
   */
  public List<String> validateHierarchy(String tenantKey) {
    List<String> issues = new ArrayList<>();
    List<GroupEntity> allGroups = groupRepository.findByTenantKey(tenantKey);

    for (GroupEntity group : allGroups) {
      // Check path consistency
      if (group.getParentGroup() != null) {
        String expectedPath = group.getParentGroup().getPath() + "/" + group.getName();
        if (!group.getPath().equals(expectedPath)) {
          issues.add(String.format("Path mismatch for %s: expected %s but got %s", group.getName(),
              expectedPath, group.getPath()));
        }
      } else {
        String expectedPath = "/" + group.getName();
        if (!group.getPath().equals(expectedPath)) {
          issues.add(String.format("Root path mismatch for %s: expected %s but got %s",
              group.getName(), expectedPath, group.getPath()));
        }
      }

      // Check for orphaned groups
      if (group.getPath().contains("/") && group.getPath().lastIndexOf("/") > 0
          && group.getParentGroup() == null) {
        issues.add(String.format("Orphaned group detected: %s with path %s has no parent",
            group.getName(), group.getPath()));
      }
    }

    return issues;
  }

  // =====================================================
  // 📊 DTO CLASSES
  // =====================================================

  /**
   * DTO for group hierarchy tree representation
   */
  @lombok.Data
  public static class GroupHierarchyNode {
    private UUID id;
    private String name;
    private String path;
    private Integer level;
    private Boolean isRoot;
    private List<GroupHierarchyNode> children;
  }

  /**
   * DTO for breadcrumb navigation
   */
  @lombok.Data
  public static class GroupBreadcrumb {
    private UUID id;
    private String name;
    private String path;
  }
}
