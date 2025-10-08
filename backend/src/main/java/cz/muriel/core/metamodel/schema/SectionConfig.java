package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Section in detail view
 */
@Data
public class SectionConfig {
  private String name;
  private List<String> fields;
  private Boolean readonly;
}
