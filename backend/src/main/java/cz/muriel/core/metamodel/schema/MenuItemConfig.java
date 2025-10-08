package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Menu item configuration
 */
@Data
public class MenuItemConfig {
  private String id;
  private String label;
  private String icon;
  private String path;
  private String requiredRole;
  private Integer order;
}
