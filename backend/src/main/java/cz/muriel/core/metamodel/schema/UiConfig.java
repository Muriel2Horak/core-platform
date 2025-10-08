package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * UI Configuration for entity
 */
@Data
public class UiConfig {
  private ListConfig list;
  private DetailConfig detail;
}
