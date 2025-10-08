package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Navigation menu configuration
 */
@Data
public class NavigationConfig {
  private List<MenuItemConfig> menu;
}
