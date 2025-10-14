package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Transition/Action configuration for workflow
 * 
 * Extended with AI-friendly metadata (since 2025-10-14)
 */
@Data
public class TransitionConfig {
  /**
   * Stable action identifier
   */
  private String code;
  
  /**
   * Source state
   */
  private String from;
  
  /**
   * Target state
   */
  private String to;
  
  /**
   * Human-readable label
   */
  private String label;
  
  /**
   * Guard condition for enabling this action
   */
  private Map<String, Object> guard;
  
  /**
   * SLA in minutes
   */
  private Integer slaMinutes;
  
  // ✨ AI-friendly extensions (since 2025-10-14)
  
  /**
   * Optional help text
   */
  private String help;
  
  /**
   * Icon name for UI (e.g., "check", "cancel", "edit")
   */
  private String icon;
  
  /**
   * Is this a dangerous action? (requires confirmation)
   */
  private Boolean dangerous;
  
  /**
   * Routes where this action is available
   */
  private List<String> routes;
  
  /**
   * Preconditions for this action (human-readable)
   */
  private List<String> preconditions;
  
  /**
   * Postconditions after this action (human-readable)
   */
  private List<String> postconditions;
  
  /**
   * Side effects of this action (human-readable)
   */
  private List<String> sideEffects;
  
  /**
   * Possible errors from this action
   */
  private List<String> errors;
  
  /**
   * Command type (for streaming)
   */
  private String commandType;
  
  /**
   * Streaming priority (CRITICAL|HIGH|NORMAL|BULK)
   */
  private String streamingPriority;
  
  /**
   * How-to steps (3-7 steps for AI guidance)
   */
  private List<String> howto;
}
