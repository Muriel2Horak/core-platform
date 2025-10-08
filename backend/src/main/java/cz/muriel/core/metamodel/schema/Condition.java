package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Condition for ABAC expressions Supports dot-notation for 1-level deep
 * relations
 */
@Data
public class Condition {
  private String left; // e.g. "${entity.tenant_id}" or "${user.tenant_id}"
  private String right; // e.g. "${user.tenant_id}" or literal value
}
