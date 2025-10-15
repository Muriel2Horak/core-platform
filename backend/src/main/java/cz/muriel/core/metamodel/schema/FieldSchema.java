package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Field definition in metamodel
 * 
 * Supported types: - Primitives: uuid, string, email, text, long, timestamp,
 * boolean - References: ref (1:1, 1:N) - Collections: collection (primitives or
 * references) - Relationships: manyToMany, oneToMany, manyToOne
 */
@Data
public class FieldSchema {
  private String name;
  private String type; // uuid, string, email, text, long, timestamp, ref, collection, manyToMany,
                       // oneToMany, manyToOne
  private Boolean pk;
  private Boolean required;
  private Boolean generated;
  private Boolean unique;
  private Integer maxLength;
  private Object defaultValue; // Default value for the field

  // For reference types (ref, manyToOne)
  private String refEntity;
  private String refField;

  // For collection types
  private String itemType; // For collection: string, number, uuid, reference
  private String targetEntity; // For collection with itemType=reference

  // For M:N relationships (manyToMany)
  private String joinTable; // Junction table name
  private String joinColumn; // Column in junction table for this entity
  private String inverseJoinColumn; // Column in junction table for target entity

  // For bidirectional relationships
  private Boolean bidirectional; // Is this a bidirectional relationship?
  private String inverseName; // Name of the inverse field (for auto-generation)
  private String mappedBy; // For inverse side of bidirectional (like JPA mappedBy)

  // For cascade operations
  private List<String> cascade; // ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH

  // ✨ AI annotations (since 2025-10-14)
  /**
   * Is this field containing PII (Personally Identifiable Information)? If true,
   * field will be masked/redacted in AI context
   */
  private Boolean pii;

  /**
   * Is help/description safe to expose to AI without data? If true, field
   * description/label can be used for AI assistance
   */
  private Boolean helpSafe;

  /**
   * Mask pattern for redacted mode (e.g., "u***@d***.cz") Optional, used when
   * visibility = REDACTED
   */
  private String mask;
}
