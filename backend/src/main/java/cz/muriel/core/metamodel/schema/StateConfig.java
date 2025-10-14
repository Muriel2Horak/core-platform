package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * State configuration for workflow
 */
@Data
public class StateConfig {
  /**
   * Stable state identifier (never changes)
   */
  private String code;
  
  /**
   * Human-readable label (localized)
   */
  private String label;
  
  /**
   * Optional description/help text
   */
  private String description;
  
  /**
   * Optional help text safe for AI (since 2025-10-14)
   */
  private String help;
}
