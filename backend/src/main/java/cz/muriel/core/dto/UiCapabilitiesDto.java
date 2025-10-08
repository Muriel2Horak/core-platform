package cz.muriel.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 📱 UI Capabilities DTO
 * 
 * Response pro GET /api/me/ui-capabilities Obsahuje pouze UI-specific
 * capabilities (menu, features). API permissions jsou vyhodnocovány na BE.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UiCapabilitiesDto {

  /**
   * Menu items podle rolí uživatele
   */
  private List<MenuItem> menu;

  /**
   * Feature flags
   */
  private List<String> features;

  /**
   * Verze permissions (pro ETag/cache invalidation)
   */
  private String permVersion;

  /**
   * Data scope uživatele
   */
  private String dataScope;

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class MenuItem {
    private String id;
    private String label;
    private String path;
    private String icon;
    private List<SubMenuItem> submenu;
    private int order;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class SubMenuItem {
    private String label;
    private String path;
  }
}
