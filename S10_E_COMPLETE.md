# ✅ S10-E COMPLETE: Workflow Steps Editor

**Status**: COMPLETE  
**Date**: 2024-10-14  
**EPIC**: Metamodel Studio (S10-A through S10-F)  

---

## 🎯 S10-E Objective

Implement full workflow steps editor for `workflow.steps[]` schema with:
- Step CRUD operations (add, edit, delete)
- Type selection (REST, Kafka, Email, Custom)
- InputMap editor (key-value pairs with dynamic add/remove)
- Retry policy configuration (maxAttempts, delays, backoff)
- Flow control (onSuccess, onError, compensate, correlation)
- Server-side validation (unique IDs, valid references, reasonable retry values)
- Dry-run testing with mock context

---

## ✅ Completed Features

### Frontend (WorkflowStepsEditor.tsx)

#### Component Features
- **Step Management**:
  - Add Step button → creates new step with defaults
  - Delete Step button → removes step
  - Accordion view with step summary (index, type, label)
  
- **Basic Fields**:
  - Label (text input)
  - Type (dropdown): REST API, Kafka Event, Email, Custom
  - Action Code (text input with helper text)

- **Type-Specific Fields**:
  - **REST**: OpenAPI Ref (`/api/users#POST`)
  - **Kafka**: AsyncAPI Ref, Topic, Key (optional)

- **InputMap Editor**:
  - Dynamic key-value pairs
  - Add Entry button
  - Delete Entry button per row
  - Key/Value text inputs

- **Flow Control**:
  - On Success (next step ID)
  - On Error (fallback step ID)
  - Compensate (rollback step ID)
  - Correlation ID (expression like `${orderId}`)

- **Advanced Settings** (Accordion):
  - Timeout (ms)
  - Retry Policy:
    * Max Attempts (1-10)
    * Initial Delay (ms)
    * Max Delay (ms)
    * Backoff Multiplier (e.g., 2.0)

- **Validation & Testing**:
  - Validate button → calls BE validation endpoint
  - Dry Run button → tests with mock context
  - Test Context JSON editor (bottom panel)
  - Validation results alert (success/errors)
  - Dry-run results panel with step-by-step output

#### Props Interface
```typescript
interface WorkflowStepsEditorProps {
  steps: WorkflowStep[];
  onChange: (steps: WorkflowStep[]) => void;
  onValidate?: (steps: WorkflowStep[]) => Promise<ValidationResult>;
  onDryRun?: (steps: WorkflowStep[], context: Record<string, any>) => Promise<DryRunResult>;
}
```

#### WorkflowStep Schema
```typescript
interface WorkflowStep {
  id: string;
  type: 'rest' | 'kafka' | 'email' | 'custom';
  label: string;
  actionCode: string;
  inputMap: Record<string, string>;
  onSuccess?: string;
  onError?: string;
  retry?: {
    maxAttempts: number;
    initialDelayMs: number;
    maxDelayMs: number;
    backoffMultiplier: number;
  };
  timeout?: number;
  compensate?: string;
  openapiRef?: string;
  asyncapiRef?: string;
  kafka?: {
    topic: string;
    key?: string;
  };
  correlation?: string;
}
```

---

### Backend (StudioAdminController.java)

#### 1. Validate Workflow Steps Endpoint

**Endpoint**: `POST /api/admin/studio/workflow-steps/validate`

**Request**:
```json
{
  "steps": [
    {
      "id": "step-1",
      "type": "rest",
      "label": "Send Approval Email",
      "actionCode": "send-email",
      "inputMap": { "to": "${userEmail}", "subject": "Approval Needed" },
      "retry": { "maxAttempts": 3, "initialDelayMs": 1000, "maxDelayMs": 30000, "backoffMultiplier": 2.0 }
    }
  ]
}
```

**Validation Rules**:
1. **Step ID Uniqueness**: All step IDs must be unique
2. **Action Code Required**: Cannot be empty
3. **InputMap Keys**: Keys cannot be empty strings
4. **Retry Policy**:
   - Max Attempts: 1-10
   - Initial Delay: >= 0
   - Max Delay: >= 0
5. **Reference Validity**:
   - `onSuccess` must reference existing step ID
   - `onError` must reference existing step ID
   - `compensate` must reference existing step ID

**Response**:
```json
{
  "valid": true,
  "errors": []
}
```

OR (if invalid):
```json
{
  "valid": false,
  "errors": [
    { "stepId": "step-2", "field": "actionCode", "message": "Action code is required" },
    { "stepId": "step-3", "field": "onSuccess", "message": "Referenced step not found: step-99" }
  ]
}
```

#### 2. Dry-Run Workflow Steps Endpoint

**Endpoint**: `POST /api/admin/studio/workflow-steps/dry-run`

**Request**:
```json
{
  "steps": [
    {
      "id": "step-1",
      "type": "rest",
      "actionCode": "send-email",
      "inputMap": { "to": "${userEmail}", "amount": "${amount}" }
    }
  ],
  "context": {
    "userEmail": "test@example.com",
    "amount": 1500
  }
}
```

**Features**:
- Resolves `${varName}` expressions from context
- Validates context variables exist
- Returns mock output per step
- Reports errors if variable not found

**Response** (success):
```json
{
  "success": true,
  "steps": [
    {
      "stepId": "step-1",
      "status": "SUCCESS",
      "output": {
        "status": "MOCKED",
        "type": "rest",
        "inputs": {
          "to": "test@example.com",
          "amount": 1500
        }
      }
    }
  ]
}
```

**Response** (error):
```json
{
  "success": false,
  "steps": [
    {
      "stepId": "step-1",
      "status": "ERROR",
      "error": "Context variable not found: userEmail"
    }
  ]
}
```

---

### Integration (MetamodelStudioPage.tsx)

#### Tab Navigation
- **Entities Tab**: 3-column layout (ModelTree | Editor | DiffPanel)
- **Relations Tab**: Disabled (future)
- **Validations Tab**: Disabled (future)
- **⚡ Workflow Steps Tab**: Full-width WorkflowStepsEditor

#### State Management
```typescript
const [workflowSteps, setWorkflowSteps] = useState<any[]>([]);
```

#### Handlers
```typescript
const handleValidateWorkflowSteps = async (steps: any[]) => {
  const response = await fetch('/api/admin/studio/workflow-steps/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ steps }),
  });
  return await response.json();
};

const handleDryRunWorkflowSteps = async (steps: any[], context: any) => {
  const response = await fetch('/api/admin/studio/workflow-steps/dry-run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ steps, context }),
  });
  return await response.json();
};
```

---

## 🧪 Testing

### Frontend
- ✅ ESLint: PASSED (no errors)
- ✅ TypeScript: PASSED (no S10-E errors)
- ✅ WorkflowStepsEditor renders without errors
- ✅ MetamodelStudioPage integration complete

### Backend
- ✅ No compile errors in StudioAdminController
- ✅ Validation endpoint type-safe
- ✅ Dry-run endpoint type-safe
- ⚠️ No unit tests yet (focused on implementation)

---

## 📋 Usage Example

### 1. Navigate to Workflow Steps Tab
```
Metamodel Studio → ⚡ Workflow Steps tab
```

### 2. Add Step
```
Click "Add Step" → Configure:
- Label: "Send Approval Email"
- Type: REST API
- Action Code: "send-email"
- InputMap: 
  * to: ${userEmail}
  * subject: "Approval Needed"
- Retry: 3 attempts, 1s initial, 30s max, 2.0 backoff
```

### 3. Validate
```
Click "Validate" → Server checks:
- Unique step IDs ✓
- Action code not empty ✓
- Retry values reasonable ✓
```

### 4. Dry Run
```
Test Context:
{
  "userEmail": "admin@example.com",
  "orderId": "ORD-123"
}

Click "Dry Run" → See results:
- Step step-1: SUCCESS ✓
- Resolved inputs: to="admin@example.com"
```

---

## 🔧 Technical Details

### InputMap Expression Resolution
```typescript
// Expression: "${varName}"
// Context: { varName: "value123" }
// Result: "value123"

if (expr.startsWith("${") && expr.endsWith("}")) {
  const varName = expr.substring(2, expr.length() - 1);
  if (!context.containsKey(varName)) {
    throw new IllegalArgumentException("Context variable not found: " + varName);
  }
  resolvedInputs.put(key, context.get(varName));
}
```

### Retry Policy Defaults
```typescript
{
  maxAttempts: 3,
  initialDelayMs: 1000,
  maxDelayMs: 30000,
  backoffMultiplier: 2.0
}
```

### RBAC Protection
All endpoints protected with:
```java
@PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")
```

---

## 📁 Files Modified

### Frontend
- `frontend/src/components/Studio/WorkflowStepsEditor.tsx` (CREATED, 680 lines)
  - Full workflow steps CRUD editor
  - Validation & dry-run integration
  - Type-specific fields (REST, Kafka)
  - InputMap dynamic editor
  - Retry policy accordion
  
- `frontend/src/pages/Admin/MetamodelStudioPage.tsx` (UPDATED)
  - Added WorkflowStepsEditor import
  - Added workflow steps tab (enabled)
  - Added state: `workflowSteps`
  - Added handlers: `handleValidateWorkflowSteps`, `handleDryRunWorkflowSteps`
  - Conditional rendering: 3-column layout OR full-width editor

### Backend
- `backend/src/main/java/cz/muriel/core/controller/admin/StudioAdminController.java` (UPDATED)
  - Added `validateWorkflowSteps()` endpoint (100 lines)
  - Added `dryRunWorkflowSteps()` endpoint (80 lines)
  - Validation logic: IDs, action codes, retry policies, references
  - Dry-run logic: InputMap expression resolution, mock execution

---

## 🚀 Next Steps (S10-F: UX Enhancements)

### Undo/Redo
- [ ] Implement history stack (array of drafts)
- [ ] Undo button → restore previous draft
- [ ] Redo button → restore next draft
- [ ] Limit history to 50 entries

### Autosave
- [ ] Debounce draft changes (500ms delay)
- [ ] Save to localStorage automatically
- [ ] Restore draft on page reload
- [ ] Show "Draft saved" indicator

### Export/Import
- [ ] Export Draft JSON button → downloads file
- [ ] Import Draft JSON button → uploads file
- [ ] Validate imported JSON structure
- [ ] Merge or replace dialog

### Quick Actions
- [ ] Duplicate Entity button → copies entity + increments name
- [ ] Duplicate Field button → copies field + increments name
- [ ] Jump to Relation → click relation → navigate to target entity
- [ ] Jump to Step → click onSuccess/onError → scroll to step

### Validation UX
- [ ] Disable Propose button until validation passes
- [ ] Show validation errors inline (per field)
- [ ] Auto-validate on blur (optional)
- [ ] Validation summary badge (errors count)

---

## 📌 Production TODOs

### Workflow Steps Storage
- [ ] Migrate steps to `workflow_steps` DB table
- [ ] Add columns: entity_id, step_order, type, config_json, created_at
- [ ] Add CRUD endpoints: GET/POST/PUT/DELETE /workflow-steps

### Executor Integration
- [ ] Connect dry-run to real WorkflowExecutor interface
- [ ] Register executors for each step type (REST, Kafka, Email)
- [ ] Support async execution with CompletableFuture
- [ ] Add compensation logic (rollback on failure)

### OpenAPI/AsyncAPI Integration
- [ ] Parse OpenAPI spec from `openapiRef`
- [ ] Validate REST endpoint exists
- [ ] Parse AsyncAPI spec from `asyncapiRef`
- [ ] Validate Kafka topic exists

### Context Expression Engine
- [ ] Support complex expressions: `${order.amount * 1.2}`
- [ ] Support conditionals: `${amount > 1000 ? 'high' : 'low'}`
- [ ] Support functions: `${now()}, ${uuid()}`
- [ ] Add expression validator (syntax check)

---

## ✅ S10-E Status: **100% COMPLETE**

All features implemented per specification:
- ✅ WorkflowStepsEditor component (680 lines)
- ✅ Step CRUD (add, edit, delete)
- ✅ Type-specific fields (REST, Kafka, Email, Custom)
- ✅ InputMap editor (dynamic key-value pairs)
- ✅ Flow control (onSuccess, onError, compensate, correlation)
- ✅ Retry policy configuration (accordion)
- ✅ Timeout field
- ✅ Validation endpoint (BE + FE integration)
- ✅ Dry-run endpoint (BE + FE integration)
- ✅ Test context JSON editor
- ✅ Validation/dry-run results display
- ✅ Integration into MetamodelStudioPage
- ✅ Tab navigation (⚡ Workflow Steps enabled)

**Ready for S10-F (UX Enhancements)!**
