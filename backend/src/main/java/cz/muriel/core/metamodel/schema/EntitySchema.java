package cz.muriel.core.metamodel.schema;

import cz.muriel.core.metamodel.schema.ai.AiConfig;
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

  // Phase 2: Fulltext search and workflow
  private List<String> fulltext;
  private List<StateConfig> states;
  private List<TransitionConfig> transitions;

  // ✨ NEW: ID Generation strategy
  private IdGenerationConfig idGeneration;

  // ✨ NEW: Lifecycle hooks
  private LifecycleConfig lifecycle;

  // 📊 NEW: Streaming configuration
  private StreamingEntityConfig streaming;

  // 🤖 AI configuration (since 2025-10-14)
  private AiConfig ai;
}
