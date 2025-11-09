# AI-001: AI Metamodel Designer

**EPIC:** EPIC-016 - AI & Metamodel Collaboration  
**Status:** ⏳ **PLANNED**  
**Priority:** 🔥 **HIGH**  
**Effort:** 3 dny (~1,200 LOC)  
**Dependencies:** Metamodel Studio, MCP Server, RBAC

---

## 📋 User Stories

### US-001.1: Draft Entity from Text
> "Jako **Solution Architect** chci z textového zadání nechat AI navrhnout entitní model jako draft, abych ho jen zrevidoval místo ručního kreslení od nuly."

**Value:** Reduce entity design time from 2 hours → 30 minutes

### US-001.2: Intelligent Field Suggestions
> "Jako **Architect** chci, aby AI navrhla nejen entity ale i pole s typy, validacemi a vazbami, abych měl 80% práce hotové automaticky."

**Value:** AI handles boilerplate, architect focuses on business rules

### US-001.3: Diff Preview
> "Jako **Power User** chci vidět diff mezi současným metamodelem a AI návrhem, abych chápal dopady změn před schválením."

**Value:** Prevent accidental breaking changes

### US-001.4: Validation Before Apply
> "Jako **Architect** chci kliknout 'Validate' na AI návrh a dostat report o konfliktech, kolizích a breaking changes, než to schválím."

**Value:** Catch errors early, before production deployment

---

## 🎯 Functional Requirements

### Input
- **Text Prompt** (natural language)
  - Example: "Chci model pro řízení reklamací zákazníků"
  - Example: "Add support tracking entity with SLA fields"
  
- **Context** (automatically included)
  - Current metamodel entities (names, fields, relationships)
  - Tenant naming conventions
  - Existing entity types and enums

### AI Output (Draft Metamodel)

**1. Entities**
- Entity names (PascalCase, singular)
- Table names (snake_case, auto-generated)
- Entity descriptions

**2. Fields per Entity**
```yaml
fields:
  - name: id
    type: uuid
    required: true
    primaryKey: true
    
  - name: customerId
    type: reference
    targetEntity: Customer
    required: true
    onDelete: RESTRICT
    
  - name: subject
    type: string
    maxLength: 200
    required: true
    indexed: false
    
  - name: priority
    type: enum
    values: [LOW, MEDIUM, HIGH, CRITICAL]
    default: MEDIUM
    
  - name: status
    type: enum
    values: [NEW, IN_PROGRESS, RESOLVED, CLOSED]
    default: NEW
    
  - name: description
    type: text
    required: false
    
  - name: createdAt
    type: timestamp
    required: true
    audit: true
    
  - name: createdBy
    type: reference
    targetEntity: User
    required: true
    audit: true
```

**3. Relationships**
```yaml
relationships:
  - type: manyToOne
    field: customerId
    targetEntity: Customer
    foreignKey: customer_id
    cascadeDelete: false
    
  - type: oneToMany
    field: comments
    targetEntity: ComplaintComment
    mappedBy: complaintId
```

**4. Validations**
```yaml
validations:
  - field: subject
    rule: required
    message: "Subject is required"
    
  - field: email
    rule: pattern
    pattern: ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
    message: "Invalid email format"
    
  - field: resolveBy
    rule: custom
    validator: "resolveBy must be after createdAt"
```

**5. Metadata**
```yaml
metadata:
  audit:
    enabled: true
    fields: [createdBy, createdAt, updatedBy, updatedAt]
    
  softDelete:
    enabled: true
    field: deletedAt
    
  security:
    piiFields: [customerEmail, customerPhone]
    sensitiveFields: []
    
  workflow:
    hooks:
      - event: onBeforeCreate
        handler: validateSLADeadline
      - event: onAfterUpdate
        handler: notifyCustomer
```

### System Actions

**1. Draft Display**
- Show draft as **diff** in Metamodel Studio
- Color-coded changes:
  - 🟢 **Green**: New entities/fields
  - 🟡 **Yellow**: Modified entities/fields
  - 🔴 **Red**: Potential conflicts/breaking changes

**2. Validation Checks**
Run automatically before allowing "Apply":

- **Name Conflicts:**
  - ❌ Entity "Customer" already exists
  - ❌ Field "email" conflicts with different type in base class
  
- **Type Mismatches:**
  - ❌ FK to non-existent entity "Invoice"
  - ❌ Enum value "CANCELLED" not in allowed list
  
- **Circular Dependencies:**
  - ❌ Entity A → B → C → A (circular FK reference)
  
- **Breaking Changes:**
  - ⚠️ Removed required field "accountNumber" from "Customer"
  - ⚠️ Changed field type "amount" from decimal → integer (data loss)

**3. Review Mode**
Architect can:
- ✏️ Edit entity/field inline
- 💬 Add comments/notes per entity
- ✅ Accept/reject individual entities
- 📝 Request AI refinement ("add SLA fields")
- 💾 Save as "Proposed Metamodel v2.1-draft"

### Security Requirements

**AI Constraints:**
- AI can suggest PII fields but MUST annotate them with `@PII`
- AI cannot suggest fields with direct DB access (raw SQL)
- Draft proposals go through MCP tool "propose_metamodel_draft" (no direct DB writes)

**RBAC:**
- Only **ARCHITECT** or **METAMODEL_ADMIN** role can trigger AI designer
- Only **ADMIN** can apply breaking changes

**Audit:**
- Log: who triggered AI, when, prompt text (sanitized)
- Log: AI draft version, entities proposed
- Log: who approved/rejected draft

---

## ✅ Acceptance Criteria

### Must Have
- [ ] UI: Text input "Describe your data model need" in Metamodel Studio
- [ ] AI proposes minimum 1 entity with 5+ fields
- [ ] AI proposes relationships (1:N or N:M) if applicable
- [ ] Draft displayed as expandable diff tree
- [ ] "Validate" button runs conflict checker
- [ ] Validation report shows errors/warnings/suggestions
- [ ] Architect can edit draft inline before apply
- [ ] "Apply Draft" button requires ARCHITECT role
- [ ] Applied changes create new metamodel version (e.g., v2.0 → v2.1)
- [ ] Audit log entry created: who, when, what changed

### Should Have
- [ ] AI suggests audit fields (createdBy, createdAt) automatically
- [ ] AI detects PII fields by name/context and annotates them
- [ ] Rollback option: revert to previous version (v2.0)
- [ ] Draft saved as "work-in-progress" (can resume later)
- [ ] Multi-entity proposals (e.g., Complaint + ComplaintComment + SLAEvent)

### Nice to Have
- [ ] AI explains reasoning ("I added 'priority' field because complaints typically need prioritization")
- [ ] Alternative suggestions ("You could also use separate Status entity instead of enum")
- [ ] Import from external schema (JSON Schema, OpenAPI, SQL DDL)

---

## 🛠️ Implementation Tasks

### Task 1: MCP Tool - Propose Metamodel Draft
**Effort:** 0.5 dne  
**Owner:** Backend Developer

**Goal:** Create MCP server endpoint that accepts text prompt and returns AI-generated metamodel draft.

**Files:**
- `backend/src/main/java/cz/muriel/core/mcp/tools/ProposeMetamodelDraftTool.java`
- `backend/src/main/java/cz/muriel/core/ai/MetamodelDraftGenerator.java`

**API Spec:**
```java
@MCPTool(name = "propose_metamodel_draft")
public class ProposeMetamodelDraftTool {
    
    @MCPToolMethod
    public MetamodelDraft proposeDraft(
        @MCPParam("prompt") String prompt,
        @MCPParam("tenantId") String tenantId,
        @MCPParam("currentVersion") String currentVersion
    ) {
        // 1. Validate: user has ARCHITECT role
        if (!hasRole(ARCHITECT)) {
            throw new UnauthorizedException("ARCHITECT role required");
        }
        
        // 2. Load current metamodel (tenant-scoped)
        Metamodel current = metamodelService.getByVersion(tenantId, currentVersion);
        
        // 3. Build context for LLM
        String context = buildContext(current); // existing entities, naming conventions
        
        // 4. Call LLM
        String llmPrompt = String.format("""
            You are a database schema designer. Based on the following request:
            "%s"
            
            Current metamodel context:
            %s
            
            Generate a YAML metamodel draft with entities, fields, relationships, validations.
            Follow naming conventions: PascalCase for entities, camelCase for fields.
            Include audit fields: createdBy, createdAt, updatedBy, updatedAt.
            Mark PII fields with security.pii annotation.
            """, prompt, context);
        
        String llmResponse = llmService.complete(llmPrompt);
        
        // 5. Parse LLM YAML → MetamodelDraft object
        MetamodelDraft draft = yamlParser.parse(llmResponse);
        
        // 6. Validate draft (basic checks)
        ValidationReport report = validator.validate(draft, current);
        
        // 7. Save draft (not applied yet)
        draft.setStatus(DraftStatus.PROPOSED);
        draft.setProposedBy(currentUser());
        draft.setProposedAt(Instant.now());
        draftRepository.save(draft);
        
        // 8. Audit log
        auditService.log(AuditEvent.builder()
            .action("AI_METAMODEL_DRAFT_PROPOSED")
            .userId(currentUser().getId())
            .tenantId(tenantId)
            .metadata(Map.of("draftId", draft.getId(), "prompt", prompt))
            .build());
        
        return draft;
    }
    
    private String buildContext(Metamodel current) {
        // Extract entity names, field types, relationships
        return String.format("""
            Existing entities: %s
            Naming convention: PascalCase entities, camelCase fields
            Standard audit fields: createdBy, createdAt, updatedBy, updatedAt
            Standard FK pattern: entityNameId (e.g., customerId)
            """, current.getEntities().stream().map(Entity::getName).collect(Collectors.joining(", ")));
    }
}
```

**Acceptance:**
- [ ] Endpoint: `POST /api/mcp/metamodel/propose-draft`
- [ ] Input validation: prompt not empty, tenantId valid
- [ ] RBAC check: ARCHITECT role required
- [ ] LLM call returns valid YAML
- [ ] Draft saved in DB with status=PROPOSED
- [ ] Audit log created

---

### Task 2: Metamodel Diff Engine
**Effort:** 1 den  
**Owner:** Backend Developer

**Goal:** Compare current metamodel vs AI draft and generate structured diff.

**Files:**
- `backend/src/main/java/cz/muriel/core/metamodel/diff/MetamodelDiffEngine.java`
- `backend/src/main/java/cz/muriel/core/metamodel/diff/DiffReport.java`

**Logic:**
```java
public class MetamodelDiffEngine {
    
    public DiffReport generateDiff(Metamodel current, MetamodelDraft draft) {
        DiffReport report = new DiffReport();
        
        // 1. Entity-level diff
        Set<String> currentEntities = current.getEntities().stream()
            .map(Entity::getName).collect(Collectors.toSet());
        Set<String> draftEntities = draft.getEntities().stream()
            .map(Entity::getName).collect(Collectors.toSet());
        
        // New entities (in draft, not in current)
        Set<String> added = Sets.difference(draftEntities, currentEntities);
        report.addedEntities(added.stream()
            .map(name -> draft.getEntity(name))
            .collect(Collectors.toList()));
        
        // Removed entities (in current, not in draft)
        Set<String> removed = Sets.difference(currentEntities, draftEntities);
        report.removedEntities(removed.stream()
            .map(name -> current.getEntity(name))
            .collect(Collectors.toList()));
        
        // Modified entities (in both)
        Set<String> common = Sets.intersection(currentEntities, draftEntities);
        for (String entityName : common) {
            Entity currentEntity = current.getEntity(entityName);
            Entity draftEntity = draft.getEntity(entityName);
            
            EntityDiff entityDiff = compareEntities(currentEntity, draftEntity);
            if (entityDiff.hasChanges()) {
                report.addModifiedEntity(entityDiff);
            }
        }
        
        return report;
    }
    
    private EntityDiff compareEntities(Entity current, Entity draft) {
        EntityDiff diff = new EntityDiff(current.getName());
        
        // Field-level diff
        Set<String> currentFields = current.getFields().stream()
            .map(Field::getName).collect(Collectors.toSet());
        Set<String> draftFields = draft.getFields().stream()
            .map(Field::getName).collect(Collectors.toSet());
        
        // Added fields
        Sets.difference(draftFields, currentFields).forEach(fieldName -> {
            diff.addAddedField(draft.getField(fieldName));
        });
        
        // Removed fields
        Sets.difference(currentFields, draftFields).forEach(fieldName -> {
            Field field = current.getField(fieldName);
            if (field.isRequired()) {
                diff.addBreakingChange("Removed required field: " + fieldName);
            }
            diff.addRemovedField(field);
        });
        
        // Modified fields
        Sets.intersection(currentFields, draftFields).forEach(fieldName -> {
            Field currentField = current.getField(fieldName);
            Field draftField = draft.getField(fieldName);
            
            FieldDiff fieldDiff = compareFields(currentField, draftField);
            if (fieldDiff.hasChanges()) {
                diff.addModifiedField(fieldDiff);
            }
        });
        
        return diff;
    }
    
    private FieldDiff compareFields(Field current, Field draft) {
        FieldDiff diff = new FieldDiff(current.getName());
        
        // Type change (breaking if incompatible)
        if (!current.getType().equals(draft.getType())) {
            diff.addChange("type", current.getType(), draft.getType());
            if (isIncompatibleTypeChange(current.getType(), draft.getType())) {
                diff.markAsBreaking("Type change from " + current.getType() + " to " + draft.getType());
            }
        }
        
        // Required change
        if (current.isRequired() != draft.isRequired()) {
            diff.addChange("required", current.isRequired(), draft.isRequired());
            if (!current.isRequired() && draft.isRequired()) {
                diff.markAsBreaking("Field made required (existing nulls will fail)");
            }
        }
        
        // MaxLength change
        if (current.getMaxLength() != null && draft.getMaxLength() != null) {
            if (!current.getMaxLength().equals(draft.getMaxLength())) {
                diff.addChange("maxLength", current.getMaxLength(), draft.getMaxLength());
                if (draft.getMaxLength() < current.getMaxLength()) {
                    diff.markAsWarning("MaxLength reduced (data may be truncated)");
                }
            }
        }
        
        return diff;
    }
    
    private boolean isIncompatibleTypeChange(FieldType from, FieldType to) {
        // decimal → integer: precision loss
        if (from == FieldType.DECIMAL && to == FieldType.INTEGER) return true;
        // timestamp → date: time loss
        if (from == FieldType.TIMESTAMP && to == FieldType.DATE) return true;
        // string → enum: values may not match
        if (from == FieldType.STRING && to == FieldType.ENUM) return true;
        return false;
    }
}
```

**Acceptance:**
- [ ] Detects added/removed/modified entities
- [ ] Detects added/removed/modified fields per entity
- [ ] Flags breaking changes (removed required field, incompatible type change)
- [ ] Flags warnings (reduced maxLength, changed default)
- [ ] Returns structured DiffReport object

---

### Task 3: Validation Service
**Effort:** 1 den  
**Owner:** Backend Developer

**Goal:** Validate metamodel draft for conflicts, errors, best practices.

**Files:**
- `backend/src/main/java/cz/muriel/core/metamodel/validation/MetamodelValidator.java`
- `backend/src/main/java/cz/muriel/core/metamodel/validation/ValidationReport.java`

**Validation Rules:**

```java
public class MetamodelValidator {
    
    public ValidationReport validate(MetamodelDraft draft, Metamodel current) {
        ValidationReport report = new ValidationReport();
        
        // 1. Entity naming conventions
        draft.getEntities().forEach(entity -> {
            if (!isPascalCase(entity.getName())) {
                report.addError("Entity name must be PascalCase: " + entity.getName());
            }
            if (isPlural(entity.getName())) {
                report.addWarning("Entity name should be singular: " + entity.getName());
            }
        });
        
        // 2. Field naming conventions
        draft.getEntities().forEach(entity -> {
            entity.getFields().forEach(field -> {
                if (!isCamelCase(field.getName())) {
                    report.addError("Field name must be camelCase: " + entity.getName() + "." + field.getName());
                }
            });
        });
        
        // 3. Primary key checks
        draft.getEntities().forEach(entity -> {
            long pkCount = entity.getFields().stream().filter(Field::isPrimaryKey).count();
            if (pkCount == 0) {
                report.addError("Entity missing primary key: " + entity.getName());
            }
            if (pkCount > 1) {
                report.addWarning("Entity has composite primary key (not recommended): " + entity.getName());
            }
        });
        
        // 4. FK reference validation
        draft.getEntities().forEach(entity -> {
            entity.getFields().stream()
                .filter(field -> field.getType() == FieldType.REFERENCE)
                .forEach(field -> {
                    String targetEntity = field.getTargetEntity();
                    boolean exists = draft.hasEntity(targetEntity) || current.hasEntity(targetEntity);
                    if (!exists) {
                        report.addError("FK references non-existent entity: " + entity.getName() + "." + field.getName() + " → " + targetEntity);
                    }
                });
        });
        
        // 5. Circular dependency detection
        Map<String, Set<String>> graph = buildDependencyGraph(draft);
        if (hasCycle(graph)) {
            report.addError("Circular FK dependencies detected");
        }
        
        // 6. Audit field recommendations
        draft.getEntities().forEach(entity -> {
            if (!hasAuditFields(entity)) {
                report.addSuggestion("Consider adding audit fields (createdBy, createdAt): " + entity.getName());
            }
        });
        
        // 7. PII detection
        draft.getEntities().forEach(entity -> {
            entity.getFields().forEach(field -> {
                if (isProbablyPII(field.getName()) && !field.hasPIIAnnotation()) {
                    report.addWarning("Field appears to be PII but lacks @PII annotation: " + entity.getName() + "." + field.getName());
                }
            });
        });
        
        return report;
    }
    
    private boolean isProbablyPII(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("email") 
            || lower.contains("phone") 
            || lower.contains("ssn") 
            || lower.contains("social")
            || lower.contains("passport")
            || lower.contains("birthdate")
            || lower.contains("dateofbirth");
    }
    
    private boolean hasAuditFields(Entity entity) {
        return entity.hasField("createdBy") && entity.hasField("createdAt");
    }
    
    private Map<String, Set<String>> buildDependencyGraph(MetamodelDraft draft) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        draft.getEntities().forEach(entity -> {
            Set<String> dependencies = entity.getFields().stream()
                .filter(field -> field.getType() == FieldType.REFERENCE)
                .map(Field::getTargetEntity)
                .collect(Collectors.toSet());
            graph.put(entity.getName(), dependencies);
        });
        
        return graph;
    }
    
    private boolean hasCycle(Map<String, Set<String>> graph) {
        // DFS cycle detection
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        
        for (String node : graph.keySet()) {
            if (hasCycleDFS(node, graph, visited, stack)) {
                return true;
            }
        }
        return false;
    }
}
```

**Acceptance:**
- [ ] Validates entity/field naming (PascalCase/camelCase)
- [ ] Checks PK exists per entity
- [ ] Validates FK references (target entity must exist)
- [ ] Detects circular dependencies
- [ ] Suggests audit fields if missing
- [ ] Flags probable PII fields without @PII annotation
- [ ] Returns ValidationReport with errors/warnings/suggestions

---

### Task 4: Metamodel Studio UI - Draft Review
**Effort:** 0.5 dne  
**Owner:** Frontend Developer

**Goal:** Display AI-generated draft as interactive diff with approve/reject workflow.

**Files:**
- `frontend/src/components/metamodel/MetamodelDraftReview.tsx`
- `frontend/src/components/metamodel/DiffViewer.tsx`
- `frontend/src/components/metamodel/ValidationReportPanel.tsx`

**UI Components:**

```tsx
// MetamodelDraftReview.tsx
export function MetamodelDraftReview({ draftId }: { draftId: string }) {
  const { data: draft } = useQuery(['metamodel-draft', draftId], 
    () => api.metamodel.getDraft(draftId));
  const { data: diffReport } = useQuery(['metamodel-diff', draftId],
    () => api.metamodel.getDiff(draftId));
  const { data: validationReport } = useQuery(['metamodel-validation', draftId],
    () => api.metamodel.validate(draftId));
  
  const [editedDraft, setEditedDraft] = useState(draft);
  
  const handleApply = async () => {
    if (validationReport.hasErrors) {
      toast.error("Cannot apply: validation errors exist");
      return;
    }
    
    await api.metamodel.applyDraft(draftId);
    toast.success("Metamodel updated to v" + draft.version);
  };
  
  return (
    <div className="metamodel-draft-review">
      <Header>
        <h1>AI Metamodel Draft: v{draft.version}</h1>
        <span>Proposed by {draft.proposedBy} at {draft.proposedAt}</span>
      </Header>
      
      <ValidationReportPanel report={validationReport} />
      
      <DiffViewer 
        current={draft.currentMetamodel}
        draft={editedDraft}
        diff={diffReport}
        onEdit={setEditedDraft}
      />
      
      <Actions>
        <Button variant="primary" onClick={handleApply} 
          disabled={validationReport.hasErrors}>
          Apply Draft
        </Button>
        <Button variant="secondary" onClick={() => api.metamodel.saveDraft(editedDraft)}>
          Save as Work-in-Progress
        </Button>
        <Button variant="danger" onClick={() => api.metamodel.rejectDraft(draftId)}>
          Reject Draft
        </Button>
      </Actions>
    </div>
  );
}

// DiffViewer.tsx
export function DiffViewer({ current, draft, diff, onEdit }) {
  return (
    <div className="diff-viewer">
      {/* Added Entities */}
      {diff.addedEntities.map(entity => (
        <EntityCard key={entity.name} variant="added">
          <EntityHeader>
            <span className="badge badge-success">+ NEW</span>
            <h3>{entity.name}</h3>
          </EntityHeader>
          <FieldList fields={entity.fields} onEdit={(field) => onEdit({...entity, fields: [...entity.fields, field]})} />
        </EntityCard>
      ))}
      
      {/* Modified Entities */}
      {diff.modifiedEntities.map(entityDiff => (
        <EntityCard key={entityDiff.name} variant="modified">
          <EntityHeader>
            <span className="badge badge-warning">~ MODIFIED</span>
            <h3>{entityDiff.name}</h3>
          </EntityHeader>
          
          {/* Added Fields */}
          {entityDiff.addedFields.map(field => (
            <FieldRow key={field.name} variant="added">
              <span className="badge badge-success">+</span>
              <FieldName>{field.name}</FieldName>
              <FieldType>{field.type}</FieldType>
              <InlineEdit field={field} onChange={(updated) => onEdit(updated)} />
            </FieldRow>
          ))}
          
          {/* Modified Fields */}
          {entityDiff.modifiedFields.map(fieldDiff => (
            <FieldRow key={fieldDiff.name} variant="modified">
              <span className="badge badge-warning">~</span>
              <FieldName>{fieldDiff.name}</FieldName>
              <FieldDiffDetail diff={fieldDiff} />
            </FieldRow>
          ))}
          
          {/* Removed Fields */}
          {entityDiff.removedFields.map(field => (
            <FieldRow key={field.name} variant="removed">
              <span className="badge badge-danger">-</span>
              <FieldName className="strikethrough">{field.name}</FieldName>
              {field.isRequired && <span className="badge badge-danger">BREAKING</span>}
            </FieldRow>
          ))}
        </EntityCard>
      ))}
      
      {/* Removed Entities */}
      {diff.removedEntities.map(entity => (
        <EntityCard key={entity.name} variant="removed">
          <EntityHeader>
            <span className="badge badge-danger">- REMOVED</span>
            <h3 className="strikethrough">{entity.name}</h3>
          </EntityHeader>
          <span className="badge badge-danger">BREAKING CHANGE</span>
        </EntityCard>
      ))}
    </div>
  );
}

// ValidationReportPanel.tsx
export function ValidationReportPanel({ report }) {
  const errorCount = report.errors.length;
  const warningCount = report.warnings.length;
  const suggestionCount = report.suggestions.length;
  
  return (
    <div className="validation-report">
      <Header>
        <h2>Validation Report</h2>
        <Badges>
          {errorCount > 0 && <Badge variant="danger">{errorCount} Errors</Badge>}
          {warningCount > 0 && <Badge variant="warning">{warningCount} Warnings</Badge>}
          {suggestionCount > 0 && <Badge variant="info">{suggestionCount} Suggestions</Badge>}
          {errorCount === 0 && warningCount === 0 && <Badge variant="success">✓ All checks passed</Badge>}
        </Badges>
      </Header>
      
      {/* Errors (blocking) */}
      {report.errors.length > 0 && (
        <Section variant="danger">
          <h3>🔴 Errors (must fix before apply)</h3>
          <IssueList issues={report.errors} />
        </Section>
      )}
      
      {/* Warnings (risky) */}
      {report.warnings.length > 0 && (
        <Section variant="warning">
          <h3>🟡 Warnings (should review)</h3>
          <IssueList issues={report.warnings} />
        </Section>
      )}
      
      {/* Suggestions (best practices) */}
      {report.suggestions.length > 0 && (
        <Section variant="info">
          <h3>💡 Suggestions (optional improvements)</h3>
          <IssueList issues={report.suggestions} />
        </Section>
      )}
    </div>
  );
}
```

**Acceptance:**
- [ ] Draft displayed as expandable tree (entities → fields)
- [ ] Color-coded: green (added), yellow (modified), red (removed)
- [ ] Validation report visible at top (errors/warnings/suggestions)
- [ ] Inline editing: click field to modify type/required/maxLength
- [ ] Comment threads: add note per entity/field
- [ ] "Apply Draft" disabled if validation errors exist
- [ ] "Save as WIP" allows resuming later
- [ ] "Reject Draft" with mandatory reason

---

## 🔒 Security Considerations

**RBAC:**
- Only `ARCHITECT` or `METAMODEL_ADMIN` can trigger AI designer
- Only `ADMIN` can override breaking change blocks
- Tenant isolation: AI sees only current tenant's metamodel

**PII Protection:**
- AI can suggest PII fields but MUST annotate them
- Prompt sanitization: no raw PII in LLM calls
- MCP tool masks any PII before sending to LLM

**Audit:**
```json
{
  "event": "AI_METAMODEL_DRAFT_PROPOSED",
  "userId": "architect@acme.com",
  "tenantId": "acme-corp",
  "timestamp": "2025-11-09T14:30:00Z",
  "metadata": {
    "draftId": "draft-uuid-123",
    "prompt": "Model pro řízení reklamací",
    "entitiesProposed": ["Complaint", "ComplaintComment", "SLAEvent"],
    "llmModel": "gpt-4",
    "llmTokensUsed": 1250
  }
}
```

---

## 📊 Success Metrics

**Adoption:**
- % of new entities created with AI assistance (target: 50%+)
- Time saved: manual (2h) vs AI draft + review (30min) (target: 75% reduction)

**Quality:**
- % of AI drafts approved without major changes (target: 70%+)
- Validation error rate per draft (target: <3 errors avg)

**User Satisfaction:**
- Architect NPS for AI Designer feature (target: 8+/10)

---

## 📚 Testing Checklist

**Unit Tests:**
- [ ] MetamodelDiffEngine: added/modified/removed detection
- [ ] MetamodelValidator: all validation rules
- [ ] PII detector: field name matching

**Integration Tests:**
- [ ] MCP tool: end-to-end draft proposal
- [ ] Draft → diff → validation pipeline
- [ ] Apply draft creates new metamodel version

**E2E Tests:**
- [ ] Architect enters prompt → draft appears
- [ ] Validation errors block apply
- [ ] Apply draft updates metamodel
- [ ] Rollback reverts to previous version

**Security Tests:**
- [ ] Non-architect cannot trigger AI designer
- [ ] PII fields auto-annotated
- [ ] Audit log created for all AI calls

---

**Last Updated:** 9. listopadu 2025  
**Owner:** AI/Platform Team
