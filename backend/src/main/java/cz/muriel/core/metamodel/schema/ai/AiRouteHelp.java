package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;

/**
 * AI help configuration for UI routes
 * 
 * Controls what help information can be exposed for specific routes.
 * 
 * @since 2025-10-14
 */
@Data
public class AiRouteHelp {
  
  /**
   * Override visibility mode for this specific route
   * If null, uses entity/global default
   */
  private AiVisibilityMode visibility;
  
  /**
   * Are examples allowed for this route?
   * If true, AI can generate example data (still respects visibility mode)
   */
  private Boolean examplesAllowed = false;
}
