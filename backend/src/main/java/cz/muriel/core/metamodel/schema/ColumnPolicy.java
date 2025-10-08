package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Column-level access policy
 */
@Data
public class ColumnPolicy {
  private PolicyRule read;
  private PolicyRule write;
}
