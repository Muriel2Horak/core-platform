package cz.muriel.core.metamodel.schema;

import lombok.Data;
import java.util.List;

/**
 * Root schema for entity metamodel definition
 */
@Data
public class EntitySchema {
  private String entity;
  private String table;
  private String idField;
  private String versionField;
  private String tenantField;

  private List<FieldSchema> fields;
  private AccessPolicy accessPolicy;
  private UiConfig ui;
  private NavigationConfig navigation;
  private List<FeatureConfig> features;
}
