package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.Map;

/**
 * ABAC Access Policy definition
 */
@Data
public class AccessPolicy {
  private PolicyRule read;
  private PolicyRule create;
  private PolicyRule update;
  private PolicyRule delete;

  // Column-level policies
  private Map<String, ColumnPolicy> columns;
}
