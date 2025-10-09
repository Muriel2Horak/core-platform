package cz.muriel.core.metamodel.schema;

import lombok.Data;

/**
 * Detailed information about a database column
 */
@Data
public class ColumnInfo {
  private String columnName;
  private String dataType;
  private Integer characterMaximumLength;
  private Integer numericPrecision;
  private Integer numericScale;
  private boolean isNullable;
  private String columnDefault;
  private boolean isPrimaryKey;
  private String foreignKeyTable;
  private String foreignKeyColumn;

  /**
   * Get full type representation (e.g., "VARCHAR(255)", "NUMERIC(10,2)")
   */
  public String getFullType() {
    if (characterMaximumLength != null && characterMaximumLength > 0) {
      return dataType.toUpperCase() + "(" + characterMaximumLength + ")";
    }
    if (numericPrecision != null && numericScale != null) {
      return dataType.toUpperCase() + "(" + numericPrecision + "," + numericScale + ")";
    }
    return dataType.toUpperCase();
  }

  /**
   * Normalize data type for comparison
   */
  public String getNormalizedType() {
    String type = dataType.toUpperCase();

    // Normalize common aliases
    return switch (type) {
    case "CHARACTER VARYING" -> "VARCHAR";
    case "CHARACTER" -> "CHAR";
    case "TIMESTAMP WITH TIME ZONE" -> "TIMESTAMPTZ";
    case "TIMESTAMP WITHOUT TIME ZONE" -> "TIMESTAMP";
    case "DOUBLE PRECISION" -> "DOUBLE PRECISION";
    case "INT", "INT4" -> "INTEGER";
    case "INT8" -> "BIGINT";
    case "INT2" -> "SMALLINT";
    case "BOOL" -> "BOOLEAN";
    default -> type;
    };
  }
}
