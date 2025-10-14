package cz.muriel.core.metamodel.schema.ai;

import lombok.Data;
import java.util.List;

/**
 * AI policies for data visibility and PII protection
 * 
 * Controls what data can be exposed to AI agents.
 * Default mode is META_ONLY for maximum security.
 * 
 * @since 2025-10-14
 */
@Data
public class AiPolicies {
  
  /**
   * Default visibility mode for AI context
   * META_ONLY - only metadata, no values
   * REDACTED - metadata + redacted values
   * FULL - metadata + full values (requires explicit permission)
   */
  private AiVisibilityMode defaultVisibility = AiVisibilityMode.META_ONLY;
  
  /**
   * Fields that should be redacted (patterns supported)
   * Examples: ["email", "phone", "nationalId", "secret*"]
   */
  private List<String> redactFields = List.of("email", "phone", "nationalId", "secret*");
  
  /**
   * Maximum number of fields to include in context
   */
  private Integer maxFields = 30;
  
  /**
   * Maximum number of records to include in context
   */
  private Integer maxRecords = 20;
  
  /**
   * Maximum tokens for AI context (soft limit)
   */
  private Integer maxTokens = 8000;
}
