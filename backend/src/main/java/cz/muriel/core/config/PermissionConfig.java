package cz.muriel.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 🔐 Permission Configuration Načítá permissions z YAML a poskytuje je jako
 * Spring Bean
 */
@Configuration @ConfigurationProperties(prefix = "") @Data
public class PermissionConfig {

  private Map<String, RolePermissions> roles;

  @Data
  public static class RolePermissions {
    private String displayName;
    private String description;
    private List<String> apiPermissions;
    private List<MenuItem> menuItems;
    private List<String> features;
    private String dataScope;
  }

  @Data
  public static class MenuItem {
    private String id;
    private String label;
    private String path;
    private String icon;
    private List<SubMenuItem> submenu;
  }

  @Data
  public static class SubMenuItem {
    private String label;
    private String path;
  }
}
