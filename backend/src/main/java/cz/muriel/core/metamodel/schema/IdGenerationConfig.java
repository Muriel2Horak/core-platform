package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Configuration for ID generation strategy
 */
@Data
public class IdGenerationConfig {

  /**
   * Generation strategy: - auto: Database auto-increment or UUID random -
   * deterministic: UUID generated from source fields (for portability) -
   * assigned: ID must be manually assigned
   */
  private String strategy; // auto, deterministic, assigned

  /**
   * For deterministic strategy: source fields to hash Example:
   * ["keycloak_role_id", "tenant_id"]
   */
  private List<String> sourceFields;

  /**
   * Hash algorithm for deterministic UUID Default: sha256
   */
  private String algorithm; // sha256, md5

  /**
   * Prefix for composite key (used in hash) Example: "role:" results in
   * "role:{tenant}:{keycloak_id}"
   */
  private String prefix;
}
