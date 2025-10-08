package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * UI Configuration for entity
 */
@Data
public class UiConfig {
  private ListConfig list;
  private DetailConfig detail;
}
