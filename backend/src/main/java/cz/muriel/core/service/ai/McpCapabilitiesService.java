package cz.muriel.core.service.ai;

import cz.muriel.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🔐 MCP Capabilities Service
 *
 * Resolves user capabilities for MCP tools using the existing permission model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpCapabilitiesService {

  private static final List<String> SCOPES = List.of("all", "tenant");
  private static final List<String> WRITE_ACTIONS = List.of("create", "update", "delete",
      "assign");

  private final PermissionService permissionService;

  /**
   * Resolve capabilities for a given routeId.
   *
   * @param auth Authentication context
   * @param routeId MCP route ID (e.g., "users.detail")
   * @return Map with canView/canEdit/canExecute
   */
  @Cacheable(value = "mcp-capabilities", key = "T(cz.muriel.core.service.ai.McpCapabilitiesService).cacheKey(#auth, #routeId)")
  public Map<String, Object> getCapabilities(Authentication auth, String routeId) {
    if (routeId == null || routeId.isBlank()) {
      throw new IllegalArgumentException("routeId required");
    }

    String resource = extractResource(routeId);
    String viewKind = extractViewKind(routeId);
    List<String> roles = extractRoles(auth);

    boolean canView = hasPermission(roles, resource, "read");
    boolean canEdit = resolveCanEdit(roles, resource, viewKind);

    Set<String> executable = new LinkedHashSet<>();
    if (hasPermission(roles, resource, "create")) {
      executable.add("create");
    }
    if (hasPermission(roles, resource, "update")) {
      executable.add("update");
    }
    if (hasPermission(roles, resource, "delete")) {
      executable.add("delete");
    }
    if (hasPermission(roles, resource, "assign")) {
      executable.add("assign");
    }

    log.debug("MCP capabilities: route={}, roles={}, view={}, canView={}, canEdit={}", routeId,
        roles, viewKind, canView, canEdit);

    return Map.of("canView", canView, "canEdit", canEdit, "canExecute",
        new ArrayList<>(executable), "note", "Capabilities resolved via PermissionService");
  }

  public static String cacheKey(Authentication auth, String routeId) {
    String safeRoute = routeId == null ? "unknown" : routeId;
    if (auth == null) {
      return safeRoute + ":anonymous";
    }
    String subject = auth.getName() != null ? auth.getName() : "unknown";
    String roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .map(McpCapabilitiesService::normalizeRole).sorted().collect(Collectors.joining(","));
    return safeRoute + ":" + subject + ":" + roles;
  }

  private boolean resolveCanEdit(List<String> roles, String resource, String viewKind) {
    return switch (viewKind) {
    case "create" -> hasPermission(roles, resource, "create");
    case "edit" -> hasPermission(roles, resource, "update");
    default -> WRITE_ACTIONS.stream().anyMatch(action -> hasPermission(roles, resource, action));
    };
  }

  private boolean hasPermission(List<String> roles, String resource, String action) {
    for (String scope : SCOPES) {
      if (permissionService.hasPermission(roles, resource + ":" + action + ":" + scope)) {
        return true;
      }
    }
    return permissionService.hasPermission(roles, resource + ":" + action + ":*");
  }

  private String extractResource(String routeId) {
    String[] parts = routeId.split("\\.");
    String resource = parts[0];
    return resource.isBlank() ? routeId : resource;
  }

  private String extractViewKind(String routeId) {
    String[] parts = routeId.split("\\.");
    return parts.length > 1 && !parts[1].isBlank() ? parts[1] : "list";
  }

  private List<String> extractRoles(Authentication auth) {
    if (auth == null) {
      return List.of();
    }
    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .map(McpCapabilitiesService::normalizeRole).collect(Collectors.toList());
  }

  private static String normalizeRole(String authority) {
    return authority.replace("ROLE_", "");
  }
}
