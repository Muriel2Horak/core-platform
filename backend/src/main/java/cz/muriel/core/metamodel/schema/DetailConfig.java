package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Detail view configuration
 */
@Data
public class DetailConfig {
  private List<SectionConfig> sections;
}
