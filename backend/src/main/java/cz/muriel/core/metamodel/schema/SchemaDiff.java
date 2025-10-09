package cz.muriel.core.metamodel.schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents detected differences between Metamodel YAML and current DB schema
 */
@Data
public class SchemaDiff {

  private String entityType;
  private String tableName;
  private List<ColumnChange> columnChanges = new ArrayList<>();
  private List<IndexChange> indexChanges = new ArrayList<>();
  private List<ConstraintChange> constraintChanges = new ArrayList<>();
  private List<TriggerChange> triggerChanges = new ArrayList<>();

  public boolean isEmpty() {
    return columnChanges.isEmpty() && indexChanges.isEmpty() && constraintChanges.isEmpty()
        && triggerChanges.isEmpty();
  }

  public boolean hasRiskyChanges() {
    return columnChanges.stream().anyMatch(ColumnChange::isRisky)
        || constraintChanges.stream().anyMatch(ConstraintChange::isRisky);
  }

  @Data
  public static class ColumnChange {
    private ChangeType type;
    private String columnName;
    private String oldType;
    private String newType;
    private Boolean oldNullable;
    private Boolean newNullable;
    private String oldDefault;
    private String newDefault;
    private boolean risky;
    private String riskDescription;
    private String sql;

    public enum ChangeType {
      ADD, ALTER_TYPE, ALTER_NULLABLE, ALTER_DEFAULT, DROP, RENAME
    }
  }

  @Data
  public static class IndexChange {
    private ChangeType type;
    private String indexName;
    private String columnName;
    private String sql;

    public enum ChangeType {
      CREATE, DROP
    }
  }

  @Data
  public static class ConstraintChange {
    private ChangeType type;
    private String constraintName;
    private String constraintType; // UNIQUE, CHECK, FOREIGN_KEY
    private String sql;
    private boolean risky;

    public enum ChangeType {
      ADD, DROP, MODIFY
    }
  }

  @Data
  public static class TriggerChange {
    private ChangeType type;
    private String triggerName;
    private String sql;

    public enum ChangeType {
      CREATE, DROP, RECREATE
    }
  }
}
