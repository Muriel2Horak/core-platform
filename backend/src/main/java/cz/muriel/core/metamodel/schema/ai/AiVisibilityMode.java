package cz.muriel.core.metamodel.schema.ai;

/**
 * AI visibility modes for context assembly
 * 
 * Controls what level of data is exposed to AI agents.
 * 
 * @since 2025-10-14
 */
public enum AiVisibilityMode {
  /**
   * META_ONLY - Only metadata (schema, structure, labels)
   * No actual data values, safest mode
   */
  META_ONLY,
  
  /**
   * REDACTED - Metadata + redacted/masked values
   * For demo/testing purposes
   */
  REDACTED,
  
  /**
   * FULL - Full data access
   * Requires explicit permission and audit logging
   */
  FULL
}
