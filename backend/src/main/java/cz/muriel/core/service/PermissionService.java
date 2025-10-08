package cz.muriel.core.service;

import cz.muriel.core.config.PermissionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔐 Permission Service Centralizovaná logika pro zjišťování permissions na
 * základě rolí
 */
@Service @RequiredArgsConstructor @Slf4j
public class PermissionService {

  private final PermissionConfig permissionConfig;

  /**
   * Získá všechny API permissions pro danou roli
   */
  public Set<String> getApiPermissions(String roleName) {
    PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(roleName);
    if (rolePerms == null) {
      log.warn("No permissions found for role: {}", roleName);
      return Collections.emptySet();
    }
    return new HashSet<>(rolePerms.getApiPermissions());
  }

  /**
   * Získá všechny API permissions pro seznam rolí (merged)
   */
  public Set<String> getApiPermissions(List<String> roles) {
    return roles.stream().map(this::getApiPermissions).flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Získá všechny menu items pro danou roli
   */
  public List<PermissionConfig.MenuItem> getMenuItems(String roleName) {
    PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(roleName);
    if (rolePerms == null) {
      return Collections.emptyList();
    }
    return rolePerms.getMenuItems();
  }

  /**
   * Získá všechny menu items pro seznam rolí (merged, deduplicated by id)
   */
  public List<PermissionConfig.MenuItem> getMenuItems(List<String> roles) {
    Map<String, PermissionConfig.MenuItem> menuMap = new LinkedHashMap<>();

    for (String role : roles) {
      List<PermissionConfig.MenuItem> items = getMenuItems(role);
      for (PermissionConfig.MenuItem item : items) {
        menuMap.putIfAbsent(item.getId(), item);
      }
    }

    return new ArrayList<>(menuMap.values());
  }

  /**
   * Získá features pro danou roli
   */
  public Set<String> getFeatures(String roleName) {
    PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(roleName);
    if (rolePerms == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(rolePerms.getFeatures());
  }

  /**
   * Získá features pro seznam rolí (merged)
   */
  public Set<String> getFeatures(List<String> roles) {
    return roles.stream().map(this::getFeatures).flatMap(Set::stream).collect(Collectors.toSet());
  }

  /**
   * Zjistí data scope - pokud má uživatel více rolí, vezme "nejvyšší" Priority:
   * all_tenants > own_tenant > own_data
   */
  public String getDataScope(List<String> roles) {
    for (String role : roles) {
      PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(role);
      if (rolePerms != null && "all_tenants".equals(rolePerms.getDataScope())) {
        return "all_tenants";
      }
    }

    for (String role : roles) {
      PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(role);
      if (rolePerms != null && "own_tenant".equals(rolePerms.getDataScope())) {
        return "own_tenant";
      }
    }

    return "own_data";
  }

  /**
   * Zkontroluje, zda role má dané API permission
   */
  public boolean hasPermission(String roleName, String permission) {
    Set<String> permissions = getApiPermissions(roleName);

    // Exact match
    if (permissions.contains(permission)) {
      return true;
    }

    // Wildcard match: users:* nebo *:*:all
    String[] parts = permission.split(":");
    if (parts.length == 3) {
      String resource = parts[0];
      // Check for resource:*
      if (permissions.contains(resource + ":*")) {
        return true;
      }
      // Check for resource:*:scope
      if (permissions.contains(resource + ":*:" + parts[2])) {
        return true;
      }
    }

    return false;
  }

  /**
   * Zkontroluje, zda role má dané API permission (pro seznam rolí)
   */
  public boolean hasPermission(List<String> roles, String permission) {
    return roles.stream().anyMatch(role -> hasPermission(role, permission));
  }

  /**
   * Získá kompletní permission info pro danou roli (pro FE)
   */
  public Map<String, Object> getPermissionInfo(List<String> roles) {
    Map<String, Object> info = new HashMap<>();
    info.put("api_permissions", getApiPermissions(roles));
    info.put("menu_items", getMenuItems(roles));
    info.put("features", getFeatures(roles));
    info.put("data_scope", getDataScope(roles));
    return info;
  }

  /**
   * Získá permission info pro jednu konkrétní roli (pro admin UI)
   */
  public Map<String, Object> getRolePermissionInfo(String roleName) {
    PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(roleName);
    if (rolePerms == null) {
      return Collections.emptyMap();
    }

    Map<String, Object> info = new HashMap<>();
    info.put("display_name", rolePerms.getDisplayName());
    info.put("description", rolePerms.getDescription());
    info.put("api_permissions", rolePerms.getApiPermissions());
    info.put("menu_items", rolePerms.getMenuItems());
    info.put("features", rolePerms.getFeatures());
    info.put("data_scope", rolePerms.getDataScope());
    return info;
  }
}
