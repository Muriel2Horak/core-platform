package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Sort configuration
 */
@Data
public class SortConfig {
  private String defaultField;
  private List<String> fields;
}
