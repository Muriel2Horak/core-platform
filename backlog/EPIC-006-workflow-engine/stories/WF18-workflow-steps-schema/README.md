# WF18: Workflow Steps Schema - Metamodel Integration

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** MEDIUM  
**Effort:** 600 LOC, 3 dny  
**Dependencies:** META-001 (Entity Definition), W10 (Workflow Steps UI), WF17 (Runtime)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Integrovat **workflow steps schema** do metamodel (EntityDefinition):
- Extend EntityDefinition.workflowSteps (JSONB array)
- Schema validation (step types, config struktura)
- Storage v workflow_versions table
- Backend API: POST /validate-steps, POST /dry-run-steps
- Migrate WorkflowStepsEditor (W10) z mock backend na real persistence

**Use Case:**

```json
{
  "entityType": "Order",
  "workflowSteps": [
    {
      "id": "validate-order",
      "type": "REST_SYNC",
      "label": "Validate Order with ERP",
      "config": {
        "endpoint": "POST /erp/validate",
        "requestBody": {
          "orderId": "${entity.id}",
          "amount": "${entity.totalAmount}"
        }
      }
    },
    {
      "id": "approval",
      "type": "APPROVAL",
      "label": "Manager Approval",
      "config": {
        "approvalType": "SINGLE",
        "approvers": [{"role": "MANAGER"}],
        "dueHours": 24
      }
    }
  ]
}
```

---

## 📋 Požadavky

### Funkční Požadavky

1. **Schema Extension**
   - EntityDefinition.workflowSteps: JSONB array
   - Step schema: `{id, type, label, config, retry, inputMapping, outputMapping}`
   - Type validation (APPROVAL, REST_SYNC, KAFKA_COMMAND, EXTERNAL_TASK, TIMER)

2. **Validation API**
   - `POST /api/workflow/steps/validate`
   - Check step type exists (executor registered)
   - Validate config schema per type
   - Return validation errors

3. **Dry-Run API**
   - `POST /api/workflow/steps/dry-run`
   - Simulate step execution (no side effects)
   - Return mock output

4. **Storage**
   - Store in workflow_versions.schema_definition (JSONB)
   - Version control (immutable after publish)

---

## 🗄️ Database Schema

```sql
-- Extend workflow_versions (already exists from W9)
-- ALTER TABLE workflow_versions ADD COLUMN IF NOT EXISTS schema_definition JSONB;

-- Step validation errors (audit)
CREATE TABLE workflow_step_validation_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    step_id VARCHAR(100) NOT NULL,
    error_code VARCHAR(50) NOT NULL, -- UNKNOWN_TYPE, INVALID_CONFIG
    error_message TEXT NOT NULL,
    validated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 🔧 Implementace

### 1. Schema Model

**File:** `backend/src/main/java/cz/muriel/core/workflow/model/WorkflowStepSchema.java`

```java
@Data
public class WorkflowStepSchema {
    private String id;
    private String type; // APPROVAL, REST_SYNC, etc.
    private String label;
    private Map<String, Object> config;
    private RetryConfig retry;
    private Map<String, String> inputMapping;
    private Map<String, String> outputMapping;
    private String condition; // Optional: "${entity.priority} == 'HIGH'"
}
```

---

### 2. Validation Service

**File:** `backend/src/main/java/cz/muriel/core/workflow/service/WorkflowStepValidationService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepValidationService {
    
    private final WorkflowExecutorRegistry executorRegistry;
    private final ObjectMapper objectMapper;
    
    public List<ValidationError> validateSteps(List<WorkflowStepSchema> steps) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (WorkflowStepSchema step : steps) {
            // 1. Check step type
            if (!executorRegistry.hasExecutor(step.getType())) {
                errors.add(new ValidationError(
                    step.getId(),
                    "UNKNOWN_TYPE",
                    "Unknown step type: " + step.getType()
                ));
                continue;
            }
            
            // 2. Validate config schema per type
            List<ValidationError> configErrors = validateConfig(step);
            errors.addAll(configErrors);
            
            // 3. Validate input/output mappings
            if (step.getInputMapping() != null) {
                validateTemplates(step.getId(), step.getInputMapping().values(), errors);
            }
            if (step.getOutputMapping() != null) {
                validateTemplates(step.getId(), step.getOutputMapping().values(), errors);
            }
        }
        
        return errors;
    }
    
    private List<ValidationError> validateConfig(WorkflowStepSchema step) {
        // Type-specific validation
        return switch (step.getType()) {
            case "APPROVAL" -> validateApprovalConfig(step);
            case "REST_SYNC" -> validateRestSyncConfig(step);
            case "KAFKA_COMMAND" -> validateKafkaConfig(step);
            default -> List.of();
        };
    }
    
    private List<ValidationError> validateApprovalConfig(WorkflowStepSchema step) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, Object> config = step.getConfig();
        
        if (!config.containsKey("approvalType")) {
            errors.add(new ValidationError(step.getId(), "MISSING_FIELD", "approvalType required"));
        }
        
        if (!config.containsKey("approvers")) {
            errors.add(new ValidationError(step.getId(), "MISSING_FIELD", "approvers required"));
        }
        
        return errors;
    }
    
    private void validateTemplates(String stepId, Collection<String> templates, List<ValidationError> errors) {
        for (String template : templates) {
            if (template.contains("${") && !template.contains("}")) {
                errors.add(new ValidationError(stepId, "INVALID_TEMPLATE", "Unclosed template: " + template));
            }
        }
    }
}

@Data
@AllArgsConstructor
class ValidationError {
    private String stepId;
    private String code;
    private String message;
}
```

---

### 3. REST API Controller

**File:** `backend/src/main/java/cz/muriel/core/workflow/api/WorkflowStepsController.java`

```java
@RestController
@RequestMapping("/api/workflow/steps")
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepsController {
    
    private final WorkflowStepValidationService validationService;
    private final WorkflowStepDryRunService dryRunService;
    
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateSteps(@RequestBody List<WorkflowStepSchema> steps) {
        log.info("Validating {} workflow steps", steps.size());
        
        List<ValidationError> errors = validationService.validateSteps(steps);
        
        if (errors.isEmpty()) {
            return ResponseEntity.ok(new ValidationResponse(true, List.of()));
        } else {
            return ResponseEntity.badRequest().body(new ValidationResponse(false, errors));
        }
    }
    
    @PostMapping("/dry-run")
    public ResponseEntity<Map<String, Object>> dryRun(
            @RequestBody WorkflowStepSchema step,
            @RequestParam String entityType,
            @RequestParam String entityId
    ) {
        log.info("Dry-run step: stepId={}, type={}", step.getId(), step.getType());
        
        Map<String, Object> output = dryRunService.dryRun(step, entityType, entityId);
        
        return ResponseEntity.ok(output);
    }
}

@Data
@AllArgsConstructor
class ValidationResponse {
    private boolean valid;
    private List<ValidationError> errors;
}
```

---

### 4. Dry-Run Service

**File:** `backend/src/main/java/cz/muriel/core/workflow/service/WorkflowStepDryRunService.java`

```java
@Service
@RequiredArgsConstructor
public class WorkflowStepDryRunService {
    
    private final ContextManager contextManager;
    
    public Map<String, Object> dryRun(WorkflowStepSchema step, String entityType, String entityId) {
        // Initialize mock context
        Map<String, Object> context = contextManager.initializeContext(entityType, entityId, null);
        
        // Return mock output based on step type
        return switch (step.getType()) {
            case "APPROVAL" -> Map.of(
                "approved", true,
                "approver", "mock-user",
                "approvedAt", Instant.now().toString()
            );
            case "REST_SYNC" -> Map.of(
                "statusCode", 200,
                "response", Map.of("id", "mock-123")
            );
            case "KAFKA_COMMAND" -> Map.of(
                "correlationId", UUID.randomUUID().toString(),
                "status", "SENT"
            );
            default -> Map.of("status", "OK");
        };
    }
}
```

---

### 5. Migration: WorkflowStepsEditor Backend

**File:** `backend/src/main/java/cz/muriel/core/workflow/api/WorkflowDefinitionController.java`

```java
@RestController
@RequestMapping("/api/workflow-definitions")
@RequiredArgsConstructor
public class WorkflowDefinitionController {
    
    private final WorkflowVersionRepository versionRepository;
    
    /**
     * Save workflow steps to EntityDefinition
     */
    @PostMapping("/{entityType}/steps")
    public ResponseEntity<Void> saveSteps(
            @PathVariable String entityType,
            @RequestBody List<WorkflowStepSchema> steps
    ) {
        // 1. Validate steps
        List<ValidationError> errors = validationService.validateSteps(steps);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // 2. Save to workflow_versions
        WorkflowVersion version = new WorkflowVersion();
        version.setEntityType(entityType);
        version.setName("Default Workflow");
        version.setVersion(1);
        
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setSteps(steps.stream()
            .map(this::convertToWorkflowStep)
            .collect(Collectors.toList())
        );
        
        version.setSchemaDefinition(definition);
        versionRepository.save(version);
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{entityType}/steps")
    public ResponseEntity<List<WorkflowStepSchema>> getSteps(@PathVariable String entityType) {
        Optional<WorkflowVersion> version = versionRepository
            .findLatestByEntityType(entityType);
        
        if (version.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<WorkflowStepSchema> steps = version.get().getSchemaDefinition().getSteps()
            .stream()
            .map(this::convertToSchema)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(steps);
    }
}
```

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] EntityDefinition.workflowSteps JSONB field
   - [ ] Validation API (POST /validate)
   - [ ] Dry-run API (POST /dry-run)
   - [ ] Storage v workflow_versions
   - [ ] WorkflowStepsEditor persistence

2. **Validation:**
   - [ ] Unknown step type → error
   - [ ] Missing required config → error
   - [ ] Invalid template syntax → error

3. **Testy:**
   - [ ] Test validation (valid + invalid steps)
   - [ ] Test dry-run
   - [ ] Integration test: UI → API → DB

---

**Related Stories:**
- META-001: Entity Definition
- W10: Workflow Steps UI
- WF17: Workflow Instance Runtime
