package cz.muriel.core.security.policy;

import cz.muriel.core.config.PermissionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 YAML Permission Adapter
 * 
 * Zpětná kompatibilita - adaptuje stávající permissions.yml na PolicyEngine format.
 * 
 * @deprecated Použij MetamodelPolicyEngine přímo s metamodelem.
 *             Tato třída bude odstraněna po migraci všech definic do metamodelu.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Deprecated(since = "2.0", forRemoval = true)
public class YamlPermissionAdapter {

  private final PermissionConfig permissionConfig;

  /**
   * Převede YAML permissions na AccessPolicy
   */
  public List<PolicyModels.AccessPolicy> getAccessPolicies() {
    List<PolicyModels.AccessPolicy> policies = new ArrayList<>();

    if (permissionConfig.getRoles() == null) {
      log.warn("No roles found in permission config");
      return policies;
    }

    permissionConfig.getRoles().forEach((roleName, rolePerms) -> {
      if (rolePerms.getApiPermissions() == null) {
        return;
      }

      rolePerms.getApiPermissions().forEach(perm -> {
        String[] parts = perm.split(":");
        if (parts.length != 3) {
          log.warn("Invalid permission format: {}", perm);
          return;
        }

        String entityType = capitalizeFirst(parts[0]); // users -> Users
        String action = parts[1];
        String scope = parts[2];

        PolicyModels.AccessPolicy policy = PolicyModels.AccessPolicy.builder()
            .entityType(entityType).action(action)
            .rule(PolicyModels.PolicyRule.builder().type(PolicyModels.PolicyRule.RuleType.ROLE)
                .expression("hasRole('" + roleName + "')").build())
            .priority(calculatePriority(scope)).build();

        policies.add(policy);
      });
    });

    return policies;
  }

  /**
   * Převede YAML menu items na MenuPolicy
   */
  public List<PolicyModels.MenuPolicy> getMenuPolicies() {
    List<PolicyModels.MenuPolicy> policies = new ArrayList<>();

    if (permissionConfig.getRoles() == null) {
      return policies;
    }

    permissionConfig.getRoles().forEach((roleName, rolePerms) -> {
      if (rolePerms.getMenuItems() == null) {
        return;
      }

      rolePerms.getMenuItems().forEach(item -> {
        PolicyModels.MenuPolicy policy = PolicyModels.MenuPolicy.builder().id(item.getId())
            .label(item.getLabel()).path(item.getPath()).icon(item.getIcon())
            .rule(PolicyModels.PolicyRule.builder().type(PolicyModels.PolicyRule.RuleType.ROLE)
                .expression("hasRole('" + roleName + "')").build())
            .order(0).build();

        policies.add(policy);
      });
    });

    // Deduplikace podle ID
    Map<String, PolicyModels.MenuPolicy> uniqueMenus = new LinkedHashMap<>();
    for (PolicyModels.MenuPolicy policy : policies) {
      uniqueMenus.putIfAbsent(policy.getId(), policy);
    }

    return new ArrayList<>(uniqueMenus.values());
  }

  /**
   * Převede YAML features na FeaturePolicy
   */
  public List<PolicyModels.FeaturePolicy> getFeaturePolicies() {
    List<PolicyModels.FeaturePolicy> policies = new ArrayList<>();

    if (permissionConfig.getRoles() == null) {
      return policies;
    }

    permissionConfig.getRoles().forEach((roleName, rolePerms) -> {
      if (rolePerms.getFeatures() == null) {
        return;
      }

      rolePerms.getFeatures().forEach(feature -> {
        PolicyModels.FeaturePolicy policy = PolicyModels.FeaturePolicy.builder().featureId(feature)
            .description("Feature from YAML: " + feature)
            .rule(PolicyModels.PolicyRule.builder().type(PolicyModels.PolicyRule.RuleType.ROLE)
                .expression("hasRole('" + roleName + "')").build())
            .build();

        policies.add(policy);
      });
    });

    return policies;
  }

  /**
   * Zkontroluje, zda role má permission
   */
  public boolean hasPermission(Authentication auth, String permission) {
    List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .map(a -> a.replace("ROLE_", "")).collect(Collectors.toList());

    for (String role : roles) {
      PermissionConfig.RolePermissions rolePerms = permissionConfig.getRoles().get(role);
      if (rolePerms == null || rolePerms.getApiPermissions() == null) {
        continue;
      }

      if (rolePerms.getApiPermissions().contains(permission)) {
        return true;
      }

      // Wildcard match
      String[] parts = permission.split(":");
      if (parts.length == 3) {
        String resource = parts[0];
        if (rolePerms.getApiPermissions().contains(resource + ":*")
            || rolePerms.getApiPermissions().contains(resource + ":*:" + parts[2])) {
          return true;
        }
      }
    }

    return false;
  }

  private String capitalizeFirst(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  private int calculatePriority(String scope) {
    return switch (scope) {
    case "all" -> 100;
    case "tenant" -> 50;
    case "self" -> 10;
    default -> 0;
    };
  }
}
