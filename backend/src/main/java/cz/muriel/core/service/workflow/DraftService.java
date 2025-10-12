package cz.muriel.core.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.entities.MetamodelCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * W5: Workflow Draft Service (Metamodel-based persistence)
 * 
 * Manages workflow drafts using MetamodelCrudService
 */
@Slf4j @Service @RequiredArgsConstructor
public class DraftService {

  private final MetamodelCrudService crudService;
  private final ObjectMapper objectMapper;

  /**
   * Get draft by entity name
   */
  @Transactional(readOnly = true)
  public Optional<Map<String, Object>> getDraft(String entity, Authentication auth) {
    log.info("📂 Loading draft workflow for entity: {}", entity);

    Map<String, String> filters = Map.of("entity", entity);
    List<Map<String, Object>> drafts = crudService.list("WorkflowDraft", filters, null, 0, 1, auth);

    if (drafts.isEmpty()) {
      log.info("📭 No draft found for entity: {}", entity);
      return Optional.empty();
    }

    Map<String, Object> draft = drafts.get(0);

    // Deserialize JSON data field to Map
    String dataJson = (String) draft.get("data");
    Map<String, Object> data = deserializeData(dataJson);

    // Return draft with deserialized data
    Map<String, Object> result = new HashMap<>(draft);
    result.put("data", data);

    log.info("✅ Draft loaded for entity: {}", entity);
    return Optional.of(result);
  }

  /**
   * Save or update draft
   */
  @Transactional
  public Map<String, Object> saveDraft(String entity, Map<String, Object> draftData,
      Authentication auth) {
    log.info("💾 Saving draft workflow for entity: {}", entity);

    // Check if draft already exists
    Map<String, String> filters = Map.of("entity", entity);
    List<Map<String, Object>> existing = crudService.list("WorkflowDraft", filters, null, 0, 1,
        auth);

    String dataJson = serializeData(draftData);

    if (existing.isEmpty()) {
      // Create new draft
      Map<String, Object> newDraft = new HashMap<>();
      newDraft.put("entity", entity);
      newDraft.put("data", dataJson);
      newDraft.put("createdBy", extractUsername(auth));
      newDraft.put("updatedBy", extractUsername(auth));

      Map<String, Object> created = crudService.create("WorkflowDraft", newDraft, auth);
      log.info("✅ Draft created for entity: {}", entity);
      return created;
    } else {
      // Update existing draft
      Map<String, Object> draft = existing.get(0);
      String draftId = (String) draft.get("id");
      Object versionObj = draft.get("version");
      long version = versionObj instanceof Number ? ((Number) versionObj).longValue() : 0L;

      Map<String, Object> updates = new HashMap<>();
      updates.put("data", dataJson);
      updates.put("updatedBy", extractUsername(auth));

      Map<String, Object> updated = crudService.update("WorkflowDraft", draftId, version, updates,
          auth);
      log.info("✅ Draft updated for entity: {}", entity);
      return updated;
    }
  }

  /**
   * Delete draft
   */
  @Transactional
  public void deleteDraft(String entity, Authentication auth) {
    log.info("🗑️ Deleting draft workflow for entity: {}", entity);

    Map<String, String> filters = Map.of("entity", entity);
    List<Map<String, Object>> drafts = crudService.list("WorkflowDraft", filters, null, 0, 1, auth);

    if (!drafts.isEmpty()) {
      String draftId = (String) drafts.get(0).get("id");
      crudService.delete("WorkflowDraft", draftId, auth);
      log.info("✅ Draft deleted for entity: {}", entity);
    } else {
      log.info("📭 No draft found to delete for entity: {}", entity);
    }
  }

  private String serializeData(Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize draft data", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> deserializeData(String json) {
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize draft data", e);
      return Map.of("nodes", List.of(), "edges", List.of());
    }
  }

  private String extractUsername(Authentication auth) {
    if (auth != null && auth.getName() != null) {
      return auth.getName();
    }
    return "system";
  }
}
