package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Feature flag configuration
 */
@Data
public class FeatureConfig {
  private String id;
  private String requiredRole;
  private String requiredGroup;
}
