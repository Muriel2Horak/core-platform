package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * List view configuration
 */
@Data
public class ListConfig {
  private List<String> columns;
  private List<String> filters;
  private SortConfig sort;
}
