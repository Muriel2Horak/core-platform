package cz.muriel.core.service;

import cz.muriel.core.dto.UiCapabilitiesDto;
import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FeatureConfig;
import cz.muriel.core.metamodel.schema.MenuItemConfig;
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
 * Generuje UI capabilities (menu, features) pro frontend z metamodelu.
 * Podporuje ETag/cache invalidation přes permVersion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UiCapabilitiesService {

  private final MetamodelRegistry metamodelRegistry;
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
   * Získá menu items podle rolí z metamodelu
   */
  private List<UiCapabilitiesDto.MenuItem> getMenuItems(List<String> roles) {
    List<UiCapabilitiesDto.MenuItem> result = new ArrayList<>();

    // Procházet všechny schemas a jejich navigation config
    for (EntitySchema schema : metamodelRegistry.getAllSchemas().values()) {
      if (schema.getNavigation() != null && schema.getNavigation().getMenu() != null) {
        for (MenuItemConfig menuItem : schema.getNavigation().getMenu()) {
          // Kontrola required role
          if (menuItem.getRequiredRole() == null || roles.contains(menuItem.getRequiredRole())) {
            UiCapabilitiesDto.MenuItem item = UiCapabilitiesDto.MenuItem.builder()
                .id(menuItem.getId())
                .label(menuItem.getLabel())
                .path(menuItem.getPath())
                .icon(menuItem.getIcon())
                .order(menuItem.getOrder())
                .build();

            result.add(item);
          }
        }
      }
    }

    // Deduplikace a řazení
    return result.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
        .collect(Collectors.toList());
  }

  /**
   * Získá features podle rolí z metamodelu
   */
  private List<String> getFeatures(List<String> roles) {
    List<String> result = new ArrayList<>();

    // Procházet všechny schemas a jejich features
    for (EntitySchema schema : metamodelRegistry.getAllSchemas().values()) {
      if (schema.getFeatures() != null) {
        for (FeatureConfig feature : schema.getFeatures()) {
          // Kontrola required role
          if (feature.getRequiredRole() == null || roles.contains(feature.getRequiredRole())) {
            result.add(feature.getId());
          }
        }
      }
    }

    return result.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Získá data scope (priority: all_tenants > own_tenant > own_data)
   * 
   * Note: DataScope není součástí metamodelu FÁZE 1, používá se legacy PermissionService
   */
  private String getDataScope(List<String> roles) {
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
      // Hash z metamodelu schémat
      int schemaHash = metamodelRegistry.getAllSchemas().hashCode();
      String input = "metamodel:" + schemaHash + ":" + lastMetamodelChange;
      
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
   * Získá seznam rolí z Authentication
   */
  private List<String> getRoles(Authentication auth) {
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(a -> a.replace("ROLE_", ""))
        .collect(Collectors.toList());
  }
}
