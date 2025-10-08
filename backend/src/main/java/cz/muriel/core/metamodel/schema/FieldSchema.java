package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Field definition in metamodel
 */
@Data
public class FieldSchema {
  private String name;
  private String type; // uuid, string, email, text, long, timestamp, ref
  private Boolean pk;
  private Boolean required;
  private Boolean generated;
  private Boolean unique;
  private Integer maxLength;

  // For reference types
  private String refEntity;
  private String refField;
}
