package cz.muriel.core.controller.admin;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S10: Metamodel Studio Admin API
 * 
 * Phase S10-A: Health check + RBAC Phase S10-B: Read-only viewer (export
 * entities) Phase S10-C: Editors (validate) Phase S10-D: Diff/Propose/Approve
 * Phase S10-E: Workflow steps editor
 * 
 * Note: MetamodelAdminController already has: - GET /api/admin/metamodel/reload
 * (hot reload + diff) - POST /api/admin/metamodel/apply-safe-changes - GET
 * /api/admin/metamodel/status
 */
@Slf4j @RestController @RequestMapping("/api/admin/studio") @RequiredArgsConstructor @PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")
public class StudioAdminController {

  private final MetamodelRegistry metamodelRegistry;

  // S10-D: In-memory storage for proposals (production should use DB)
  private final Map<String, Map<String, Object>> proposals = new LinkedHashMap<>();
  private int proposalCounter = 1;

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🎨 Studio Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "S10-B", "message",
        "Studio Admin API ready - metamodel export available"));
  }

  /**
   * S10-B: Export all entities from metamodel
   * 
   * GET /api/admin/studio/entities
   * 
   * Returns all entity schemas with fields, relations, validations
   */
  @GetMapping("/entities")
  public ResponseEntity<Map<String, Object>> exportEntities() {
    log.info("📦 Exporting all entities from metamodel");

    try {
      Map<String, EntitySchema> schemas = metamodelRegistry.getAllSchemas();

      // Convert to simplified DTO for frontend
      List<Map<String, Object>> entities = schemas.entrySet().stream().map(entry -> {
        EntitySchema schema = entry.getValue();
        Map<String, Object> entityDto = new LinkedHashMap<>();
        entityDto.put("name", entry.getKey());
        entityDto.put("entity", schema.getEntity());
        entityDto.put("table", schema.getTable());
        entityDto.put("idField", schema.getIdField());
        entityDto.put("versionField", schema.getVersionField());
        entityDto.put("tenantField", schema.getTenantField());
        entityDto.put("fields", schema.getFields());
        entityDto.put("accessPolicy", schema.getAccessPolicy());
        entityDto.put("ui", schema.getUi());
        entityDto.put("navigation", schema.getNavigation());
        entityDto.put("features", schema.getFeatures());
        entityDto.put("fulltext", schema.getFulltext());
        entityDto.put("states", schema.getStates());
        entityDto.put("transitions", schema.getTransitions());
        entityDto.put("idGeneration", schema.getIdGeneration());
        entityDto.put("lifecycle", schema.getLifecycle());
        entityDto.put("streaming", schema.getStreaming());
        return entityDto;
      }).collect(Collectors.toList());

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "success");
      response.put("entitiesCount", entities.size());
      response.put("entities", entities);

      log.info("✅ Exported {} entities", entities.size());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Failed to export entities: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError().body(
          Map.of("status", "error", "message", "Failed to export entities: " + e.getMessage()));
    }
  }

  /**
   * S10-B: Get single entity detail
   * 
   * GET /api/admin/studio/entities/{entity}
   */
  @GetMapping("/entities/{entity}")
  public ResponseEntity<Map<String, Object>> getEntity(@PathVariable String entity) {
    log.info("📋 Getting entity detail: {}", entity);

    Optional<EntitySchema> schema = metamodelRegistry.getSchema(entity);
    if (schema.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntitySchema s = schema.get();
    Map<String, Object> entityDto = new LinkedHashMap<>();
    entityDto.put("name", entity);
    entityDto.put("entity", s.getEntity());
    entityDto.put("table", s.getTable());
    entityDto.put("idField", s.getIdField());
    entityDto.put("versionField", s.getVersionField());
    entityDto.put("tenantField", s.getTenantField());
    entityDto.put("fields", s.getFields());
    entityDto.put("accessPolicy", s.getAccessPolicy());
    entityDto.put("ui", s.getUi());
    entityDto.put("navigation", s.getNavigation());
    entityDto.put("features", s.getFeatures());
    entityDto.put("fulltext", s.getFulltext());
    entityDto.put("states", s.getStates());
    entityDto.put("transitions", s.getTransitions());
    entityDto.put("idGeneration", s.getIdGeneration());
    entityDto.put("lifecycle", s.getLifecycle());
    entityDto.put("streaming", s.getStreaming());

    return ResponseEntity.ok(entityDto);
  }

  /**
   * S10-C: Validate entity draft
   * 
   * POST /api/admin/studio/validate
   * 
   * Validates entity schema without persisting
   */
  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validateEntity(
      @RequestBody Map<String, Object> draftData) {
    log.info("✓ Validating entity draft");

    List<Map<String, Object>> errors = new ArrayList<>();

    try {
      // Basic validation rules
      String entityName = (String) draftData.get("entity");
      String tableName = (String) draftData.get("table");

      if (entityName == null || entityName.isBlank()) {
        errors.add(
            Map.of("field", "entity", "message", "Entity name is required", "severity", "error"));
      } else if (!entityName.matches("^[A-Z][a-zA-Z0-9]*$")) {
        errors.add(Map.of("field", "entity", "message",
            "Entity name must start with capital letter and contain only alphanumeric characters",
            "severity", "error"));
      }

      if (tableName == null || tableName.isBlank()) {
        errors.add(
            Map.of("field", "table", "message", "Table name is required", "severity", "error"));
      } else if (!tableName.matches("^[a-z][a-z0-9_]*$")) {
        errors.add(Map.of("field", "table", "message",
            "Table name must be lowercase with underscores", "severity", "error"));
      }

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> fields = (List<Map<String, Object>>) draftData.get("fields");

      if (fields == null || fields.isEmpty()) {
        errors.add(Map.of("field", "fields", "message", "At least one field is required",
            "severity", "error"));
      } else {
        // Validate fields
        for (int i = 0; i < fields.size(); i++) {
          Map<String, Object> field = fields.get(i);
          String fieldName = (String) field.get("name");
          String fieldType = (String) field.get("type");

          if (fieldName == null || fieldName.isBlank()) {
            errors.add(Map.of("field", "fields[" + i + "].name", "message",
                "Field name is required", "severity", "error"));
          }

          if (fieldType == null || fieldType.isBlank()) {
            errors.add(Map.of("field", "fields[" + i + "].type", "message",
                "Field type is required", "severity", "error"));
          }
        }
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", errors.isEmpty() ? "valid" : "invalid");
      response.put("errors", errors);

      if (!errors.isEmpty()) {
        log.warn("⚠️ Validation failed with {} errors", errors.size());
      } else {
        log.info("✅ Validation passed");
      }

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Validation error: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * S10-D: Preview diff between current and draft
   * 
   * POST /api/admin/studio/preview
   */
  @PostMapping("/preview")
  public ResponseEntity<Map<String, Object>> previewDiff(@RequestBody Map<String, Object> request) {
    log.info("🔍 Previewing diff");

    try {
      String entityName = (String) request.get("entity");
      @SuppressWarnings("unchecked")
      Map<String, Object> draftData = (Map<String, Object>) request.get("draft");

      // Get current entity
      Optional<EntitySchema> currentSchema = metamodelRegistry.getSchema(entityName);

      Map<String, Object> currentData = new LinkedHashMap<>();
      if (currentSchema.isPresent()) {
        EntitySchema s = currentSchema.get();
        currentData.put("entity", s.getEntity());
        currentData.put("table", s.getTable());
        currentData.put("fields", s.getFields());
      }

      // Calculate diff
      List<Map<String, Object>> changes = calculateDiff(currentData, draftData);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("status", "success");
      response.put("current", currentData);
      response.put("draft", draftData);
      response.put("changes", changes);

      log.info("✅ Preview generated with {} changes", changes.size());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Preview failed: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * S10-D: Create proposal (change request)
   * 
   * POST /api/admin/studio/proposals
   */
  @PostMapping("/proposals")
  public ResponseEntity<Map<String, Object>> createProposal(
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("📝 Creating proposal");

    try {
      String proposalId = "PROP-" + (proposalCounter++);
      String author = auth != null ? auth.getName() : "system";
      String description = (String) request.getOrDefault("description", "Metamodel change");

      @SuppressWarnings("unchecked")
      Map<String, Object> draftData = (Map<String, Object>) request.get("draft");

      Map<String, Object> proposal = new LinkedHashMap<>();
      proposal.put("id", proposalId);
      proposal.put("author", author);
      proposal.put("description", description);
      proposal.put("draft", draftData);
      proposal.put("status", "pending");
      proposal.put("createdAt", LocalDateTime.now().toString());

      proposals.put(proposalId, proposal);

      log.info("✅ Proposal {} created by {}", proposalId, author);
      return ResponseEntity.ok(proposal);

    } catch (Exception e) {
      log.error("❌ Failed to create proposal: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * S10-D: List proposals
   * 
   * GET /api/admin/studio/proposals
   */
  @GetMapping("/proposals")
  public ResponseEntity<List<Map<String, Object>>> listProposals(
      @RequestParam(required = false) String status) {
    log.info("📋 Listing proposals (status={})", status);

    List<Map<String, Object>> filtered = proposals.values().stream()
        .filter(p -> status == null || status.equals(p.get("status"))).collect(Collectors.toList());

    return ResponseEntity.ok(filtered);
  }

  /**
   * S10-D: Approve proposal
   * 
   * POST /api/admin/studio/proposals/{id}/approve
   */
  @PostMapping("/proposals/{id}/approve")
  public ResponseEntity<Map<String, Object>> approveProposal(@PathVariable String id,
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("✅ Approving proposal: {}", id);

    try {
      Map<String, Object> proposal = proposals.get(id);
      if (proposal == null) {
        return ResponseEntity.notFound().build();
      }

      String approver = auth != null ? auth.getName() : "system";
      String comment = (String) request.getOrDefault("comment", "Approved");

      proposal.put("status", "approved");
      proposal.put("approver", approver);
      proposal.put("comment", comment);
      proposal.put("approvedAt", LocalDateTime.now().toString());

      // S10-D: Bump specVersion (simulated)
      @SuppressWarnings("unchecked")
      Map<String, Object> draft = (Map<String, Object>) proposal.get("draft");
      Integer currentVersion = (Integer) draft.getOrDefault("specVersion", 1);
      draft.put("specVersion", currentVersion + 1);

      log.info("✅ Proposal {} approved by {} (specVersion → {})", id, approver, currentVersion + 1);
      return ResponseEntity.ok(proposal);

    } catch (Exception e) {
      log.error("❌ Failed to approve proposal: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * S10-D: Reject proposal
   * 
   * POST /api/admin/studio/proposals/{id}/reject
   */
  @PostMapping("/proposals/{id}/reject")
  public ResponseEntity<Map<String, Object>> rejectProposal(@PathVariable String id,
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("❌ Rejecting proposal: {}", id);

    try {
      Map<String, Object> proposal = proposals.get(id);
      if (proposal == null) {
        return ResponseEntity.notFound().build();
      }

      String reviewer = auth != null ? auth.getName() : "system";
      String comment = (String) request.getOrDefault("comment", "Rejected");

      proposal.put("status", "rejected");
      proposal.put("reviewer", reviewer);
      proposal.put("comment", comment);
      proposal.put("rejectedAt", LocalDateTime.now().toString());

      log.info("❌ Proposal {} rejected by {}", id, reviewer);
      return ResponseEntity.ok(proposal);

    } catch (Exception e) {
      log.error("❌ Failed to reject proposal: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * S10-D: Get proposal diff
   * 
   * GET /api/admin/studio/proposals/{id}/diff
   */
  @GetMapping("/proposals/{id}/diff")
  public ResponseEntity<Map<String, Object>> getProposalDiff(@PathVariable String id) {
    log.info("🔍 Getting diff for proposal: {}", id);

    try {
      Map<String, Object> proposal = proposals.get(id);
      if (proposal == null) {
        return ResponseEntity.notFound().build();
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> draft = (Map<String, Object>) proposal.get("draft");
      String entityName = (String) draft.get("entity");

      // Get current entity
      Optional<EntitySchema> currentSchema = metamodelRegistry.getSchema(entityName);
      Map<String, Object> currentData = new LinkedHashMap<>();
      if (currentSchema.isPresent()) {
        EntitySchema s = currentSchema.get();
        currentData.put("entity", s.getEntity());
        currentData.put("table", s.getTable());
        currentData.put("fields", s.getFields());
      }

      List<Map<String, Object>> changes = calculateDiff(currentData, draft);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("proposalId", id);
      response.put("current", currentData);
      response.put("draft", draft);
      response.put("changes", changes);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Failed to get diff: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }

  /**
   * Calculate diff between current and draft
   */
  private List<Map<String, Object>> calculateDiff(Map<String, Object> current,
      Map<String, Object> draft) {
    List<Map<String, Object>> changes = new ArrayList<>();

    // Compare entity name
    if (!Objects.equals(current.get("entity"), draft.get("entity"))) {
      changes.add(Map.of("type", "MODIFY", "field", "entity", "oldValue",
          current.getOrDefault("entity", ""), "newValue", draft.getOrDefault("entity", "")));
    }

    // Compare table name
    if (!Objects.equals(current.get("table"), draft.get("table"))) {
      changes.add(Map.of("type", "MODIFY", "field", "table", "oldValue",
          current.getOrDefault("table", ""), "newValue", draft.getOrDefault("table", "")));
    }

    // Compare fields (simplified)
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentFields = (List<Map<String, Object>>) current
        .getOrDefault("fields", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> draftFields = (List<Map<String, Object>>) draft.getOrDefault("fields",
        List.of());

    Set<String> currentFieldNames = currentFields.stream().map(f -> (String) f.get("name"))
        .collect(Collectors.toSet());
    Set<String> draftFieldNames = draftFields.stream().map(f -> (String) f.get("name"))
        .collect(Collectors.toSet());

    // Added fields
    draftFieldNames.stream().filter(name -> !currentFieldNames.contains(name)).forEach(
        name -> changes.add(Map.of("type", "ADD", "field", "fields." + name, "newValue", draftFields
            .stream().filter(f -> name.equals(f.get("name"))).findFirst().orElse(Map.of()))));

    // Removed fields
    currentFieldNames.stream().filter(name -> !draftFieldNames.contains(name))
        .forEach(name -> changes
            .add(Map.of("type", "REMOVE", "field", "fields." + name, "oldValue", currentFields
                .stream().filter(f -> name.equals(f.get("name"))).findFirst().orElse(Map.of()))));

    return changes;
  }

  /**
   * S10-E: Validate workflow steps schema
   * 
   * POST /api/admin/studio/workflow-steps/validate
   * 
   * Validates: - Step IDs are unique - Action codes are not empty - InputMap keys
   * are valid - onSuccess/onError references exist - Retry policy values are
   * reasonable
   * 
   * @param request { "steps": [...] }
   * @return Validation result
   */
  @PostMapping("/workflow-steps/validate")
  public ResponseEntity<Map<String, Object>> validateWorkflowSteps(
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("📋 Validating workflow steps for user: {}", auth.getName());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> steps = (List<Map<String, Object>>) request.getOrDefault("steps",
        List.of());

    List<Map<String, Object>> errors = new ArrayList<>();
    Set<String> stepIds = new HashSet<>();

    for (int i = 0; i < steps.size(); i++) {
      Map<String, Object> step = steps.get(i);
      String stepId = (String) step.get("id");
      String actionCode = (String) step.get("actionCode");

      // Validate step ID uniqueness
      if (stepId == null || stepId.isBlank()) {
        errors.add(Map.of("stepId", "step-" + i, "field", "id", "message", "Step ID is required"));
      } else if (stepIds.contains(stepId)) {
        errors.add(
            Map.of("stepId", stepId, "field", "id", "message", "Duplicate step ID: " + stepId));
      } else {
        stepIds.add(stepId);
      }

      // Validate action code
      if (actionCode == null || actionCode.isBlank()) {
        errors.add(
            Map.of("stepId", stepId, "field", "actionCode", "message", "Action code is required"));
      }

      // Validate inputMap keys (should not be empty)
      @SuppressWarnings("unchecked")
      Map<String, String> inputMap = (Map<String, String>) step.getOrDefault("inputMap", Map.of());
      for (String key : inputMap.keySet()) {
        if (key.isBlank()) {
          errors.add(Map.of("stepId", stepId, "field", "inputMap", "message",
              "InputMap key cannot be empty"));
        }
      }

      // Validate retry policy
      @SuppressWarnings("unchecked")
      Map<String, Object> retry = (Map<String, Object>) step.get("retry");
      if (retry != null) {
        Integer maxAttempts = (Integer) retry.get("maxAttempts");
        Integer initialDelayMs = (Integer) retry.get("initialDelayMs");
        Integer maxDelayMs = (Integer) retry.get("maxDelayMs");

        if (maxAttempts != null && (maxAttempts < 1 || maxAttempts > 10)) {
          errors.add(Map.of("stepId", stepId, "field", "retry.maxAttempts", "message",
              "Max attempts must be between 1 and 10"));
        }
        if (initialDelayMs != null && initialDelayMs < 0) {
          errors.add(Map.of("stepId", stepId, "field", "retry.initialDelayMs", "message",
              "Initial delay must be >= 0"));
        }
        if (maxDelayMs != null && maxDelayMs < 0) {
          errors.add(Map.of("stepId", stepId, "field", "retry.maxDelayMs", "message",
              "Max delay must be >= 0"));
        }
      }
    }

    // Validate onSuccess/onError references
    for (Map<String, Object> step : steps) {
      String stepId = (String) step.get("id");
      String onSuccess = (String) step.get("onSuccess");
      String onError = (String) step.get("onError");
      String compensate = (String) step.get("compensate");

      if (onSuccess != null && !onSuccess.isBlank() && !stepIds.contains(onSuccess)) {
        errors.add(Map.of("stepId", stepId, "field", "onSuccess", "message",
            "Referenced step not found: " + onSuccess));
      }
      if (onError != null && !onError.isBlank() && !stepIds.contains(onError)) {
        errors.add(Map.of("stepId", stepId, "field", "onError", "message",
            "Referenced step not found: " + onError));
      }
      if (compensate != null && !compensate.isBlank() && !stepIds.contains(compensate)) {
        errors.add(Map.of("stepId", stepId, "field", "compensate", "message",
            "Referenced step not found: " + compensate));
      }
    }

    boolean valid = errors.isEmpty();

    log.info("Workflow steps validation: {} errors found", errors.size());

    return ResponseEntity.ok(Map.of("valid", valid, "errors", errors));
  }

  /**
   * S10-E: Dry-run workflow steps with test context
   * 
   * POST /api/admin/studio/workflow-steps/dry-run
   * 
   * Simulates step execution with mock context. Validates: - InputMap expressions
   * resolve correctly - Context variables are available - Flow control
   * (onSuccess/onError) is correct
   * 
   * @param request { "steps": [...], "context": { "key": "value" } }
   * @return Dry-run result
   */
  @PostMapping("/workflow-steps/dry-run")
  public ResponseEntity<Map<String, Object>> dryRunWorkflowSteps(
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("🧪 Dry-running workflow steps for user: {}", auth.getName());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> steps = (List<Map<String, Object>>) request.getOrDefault("steps",
        List.of());
    @SuppressWarnings("unchecked")
    Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", Map.of());

    List<Map<String, Object>> results = new ArrayList<>();
    boolean success = true;

    for (Map<String, Object> step : steps) {
      String stepId = (String) step.get("id");
      String type = (String) step.get("type");
      @SuppressWarnings("unchecked")
      Map<String, String> inputMap = (Map<String, String>) step.getOrDefault("inputMap", Map.of());

      try {
        // Resolve inputMap expressions (simplified - just check if keys exist in
        // context)
        Map<String, Object> resolvedInputs = new HashMap<>();
        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
          String key = entry.getKey();
          String expr = entry.getValue();

          // Simple variable resolution: "${varName}" -> context.get("varName")
          if (expr.startsWith("${") && expr.endsWith("}")) {
            String varName = expr.substring(2, expr.length() - 1);
            if (!context.containsKey(varName)) {
              throw new IllegalArgumentException("Context variable not found: " + varName);
            }
            resolvedInputs.put(key, context.get(varName));
          } else {
            resolvedInputs.put(key, expr);
          }
        }

        // Mock output (in real implementation, would call executor)
        Map<String, Object> output = Map.of("status", "MOCKED", "type", type, "inputs",
            resolvedInputs);

        results.add(Map.of("stepId", stepId, "status", "SUCCESS", "output", output));
      } catch (Exception e) {
        success = false;
        results.add(Map.of("stepId", stepId, "status", "ERROR", "error", e.getMessage()));
      }
    }

    log.info("Dry-run completed: {} steps, success={}", steps.size(), success);

    return ResponseEntity.ok(Map.of("success", success, "steps", results));
  }
}
