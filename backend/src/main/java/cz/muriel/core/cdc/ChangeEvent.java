package cz.muriel.core.cdc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * CDC Change Event z Postgres triggeru
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ChangeEvent {

  @JsonProperty("operation")
  private String operation; // INSERT, UPDATE, DELETE

  @JsonProperty("table_name")
  private String tableName;

  @JsonProperty("schema_name")
  private String schemaName;

  @JsonProperty("timestamp")
  private Instant timestamp;

  @JsonProperty("old_data")
  private JsonNode oldData;

  @JsonProperty("new_data")
  private JsonNode newData;

  @JsonProperty("changed_fields")
  private JsonNode changedFields;

  /**
   * Získá hodnotu z new_data nebo old_data
   */
  public String getFieldValue(String fieldName) {
    if (newData != null && newData.has(fieldName)) {
      JsonNode node = newData.get(fieldName);
      return node.isNull() ? null : node.asText();
    }
    if (oldData != null && oldData.has(fieldName)) {
      JsonNode node = oldData.get(fieldName);
      return node.isNull() ? null : node.asText();
    }
    return null;
  }

  /**
   * Zkontroluje, zda se pole změnilo
   */
  public boolean hasFieldChanged(String fieldName) {
    return changedFields != null && changedFields.has(fieldName);
  }

  public boolean isInsert() {
    return "INSERT".equalsIgnoreCase(operation);
  }

  public boolean isUpdate() {
    return "UPDATE".equalsIgnoreCase(operation);
  }

  public boolean isDelete() {
    return "DELETE".equalsIgnoreCase(operation);
  }
}
