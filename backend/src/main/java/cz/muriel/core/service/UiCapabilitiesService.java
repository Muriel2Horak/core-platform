package cz.muriel.core.service;

import cz.muriel.core.dto.UiCapabilitiesDto;
import cz.muriel.core.security.policy.PolicyModels;
import cz.muriel.core.security.policy.YamlPermissionAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🎨 UI Capabilities Service
 * 
 * Generuje UI capabilities (menu, features) pro frontend. Podporuje ETag/cache
 * invalidation přes permVersion.
 */
@Service @RequiredArgsConstructor @Slf4j
public class UiCapabilitiesService {

  @SuppressWarnings("deprecation")
  private final YamlPermissionAdapter yamlAdapter;
  private final PermissionService legacyPermissionService;

  // Cache pro perm version (invaliduje se při změně metamodelu)
  private static volatile String cachedPermVersion = null;
  private static volatile long lastMetamodelChange = System.currentTimeMillis();

  /**
   * Získá UI capabilities pro uživatele
   */
  public UiCapabilitiesDto getCapabilities(Authentication auth) {
    List<String> roles = getRoles(auth);

    // Menu
    List<UiCapabilitiesDto.MenuItem> menu = getMenuItems(roles);

    // Features
    List<String> features = getFeatures(roles);

    // Data scope
    String dataScope = getDataScope(roles);

    // Perm version
    String permVersion = getPermVersion();

    return UiCapabilitiesDto.builder().menu(menu).features(features).permVersion(permVersion)
        .dataScope(dataScope).build();
  }

  /**
   * Získá menu items podle rolí
   */
  private List<UiCapabilitiesDto.MenuItem> getMenuItems(List<String> roles) {
    // TODO: Načíst z metamodelu
    // List<PolicyModels.MenuPolicy> policies = metamodelRegistry.getMenuPolicies();

    // Fallback na YAML
    List<PolicyModels.MenuPolicy> yamlMenus = yamlAdapter.getMenuPolicies();

    List<UiCapabilitiesDto.MenuItem> result = new ArrayList<>();

    for (PolicyModels.MenuPolicy policy : yamlMenus) {
      if (evaluateMenuRule(policy.getRule(), roles)) {
        UiCapabilitiesDto.MenuItem item = UiCapabilitiesDto.MenuItem.builder().id(policy.getId())
            .label(policy.getLabel()).path(policy.getPath()).icon(policy.getIcon())
            .order(policy.getOrder()).build();

        // Submenu
        if (policy.getSubmenu() != null && !policy.getSubmenu().isEmpty()) {
          List<UiCapabilitiesDto.SubMenuItem> submenuItems = policy.getSubmenu().stream()
              .filter(sub -> evaluateMenuRule(sub.getRule(), roles))
              .map(sub -> UiCapabilitiesDto.SubMenuItem.builder().label(sub.getLabel())
                  .path(sub.getPath()).build())
              .collect(Collectors.toList());
          item.setSubmenu(submenuItems);
        }

        result.add(item);
      }
    }

    // Deduplikace a řazení
    return result.stream().distinct().sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
        .collect(Collectors.toList());
  }

  /**
   * Získá features podle rolí
   */
  private List<String> getFeatures(List<String> roles) {
    // TODO: Načíst z metamodelu

    // Fallback na YAML
    List<PolicyModels.FeaturePolicy> yamlFeatures = yamlAdapter.getFeaturePolicies();

    return yamlFeatures.stream().filter(policy -> evaluateFeatureRule(policy.getRule(), roles))
        .map(PolicyModels.FeaturePolicy::getFeatureId).distinct().collect(Collectors.toList());
  }

  /**
   * Získá data scope (priority: all_tenants > own_tenant > own_data)
   */
  private String getDataScope(List<String> roles) {
    // TODO: Načíst z metamodelu podle rolí

    // Fallback na legacy PermissionService
    return legacyPermissionService.getDataScope(roles);
  }

  /**
   * Generuje perm version (hash metamodelu + timestamp poslední změny)
   */
  public String getPermVersion() {
    if (cachedPermVersion != null) {
      return cachedPermVersion;
    }

    try {
      // TODO: Hash z metamodelu
      String input = "metamodel:" + lastMetamodelChange;
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }

      cachedPermVersion = hexString.substring(0, 16); // První 16 znaků
      return cachedPermVersion;
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 not available", e);
      return String.valueOf(lastMetamodelChange);
    }
  }

  /**
   * Invaliduje perm version (volat při změně metamodelu)
   */
  public static void invalidatePermVersion() {
    cachedPermVersion = null;
    lastMetamodelChange = System.currentTimeMillis();
    log.info("Permission version invalidated at {}", Instant.ofEpochMilli(lastMetamodelChange));
  }

  /**
   * Vyhodnotí menu rule
   */
  private boolean evaluateMenuRule(PolicyModels.PolicyRule rule, List<String> roles) {
    if (rule == null) {
      return true; // No rule = visible for all
    }

    if (rule.getType() == PolicyModels.PolicyRule.RuleType.ROLE) {
      String roleName = rule.getExpression().replaceAll(".*hasRole\\('([^']+)'\\).*", "$1");
      return roles.contains(roleName);
    }

    // TODO: Implementovat další typy rules
    return false;
  }

  /**
   * Vyhodnotí feature rule
   */
  private boolean evaluateFeatureRule(PolicyModels.PolicyRule rule, List<String> roles) {
    return evaluateMenuRule(rule, roles); // Stejná logika
  }

  /**
   * Získá seznam rolí z Authentication
   */
  private List<String> getRoles(Authentication auth) {
    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
        .map(a -> a.replace("ROLE_", "")).collect(Collectors.toList());
  }
}
