# EPIC-015: AI & ML Platform Integration

**Status:** 🟡 **30% IN PROGRESS** (MCP server prototyp, Copilot integrace ✅)  
**Priority:** 🔥 **HIGH** (strategic differentiator)  
**Effort:** ~30 dní implementace (15d metamodel + 10d AI tools + 5d ML platform)  
**Dependencies:** Metamodel Studio, RBAC, Audit Trail, MCP Server, MLflow

> **Strategic Vision:** Kompletní AI/ML ekosystém - od intelligent metamodel designu přes developer productivity tools až po ML model serving a predictions.

**Merged EPICs:**
- ✅ EPIC-009: AI Integration & MCP Server (40% done - MCP prototyp, Copilot)
- ✅ EPIC-010: ML Platform (0% done - model serving, training pipelines)

---

## 🎯 Vision

**Unified AI/ML Platform** integrující:

### 1. AI-Powered Metamodel Management
- AI navrhuje datové modely z textových zadání (draft, ne autopilot)
- Kontroluje kvalitu, bezpečnost a konzistenci změn
- Generuje UI specifikace z metamodelu automaticky
- Najde duplicity, nekonzistence a tech debt
- Chrání PII a citlivá data podle metamodel anotací
- Odpovídá na otázky o datovém modelu v přirozeném jazyce

### 2. Developer Productivity (AI Tools)
- **MCP Server**: Context provider pro AI modely (Claude, GPT)
- **GitHub Copilot**: Project-specific instructions (~500 LOC rules)
- **Test Generation**: Auto-generate tests from acceptance criteria
- **Code Review Bot**: AI-powered PR reviews

### 3. ML Platform & Intelligent Features
- **Model Serving**: TorchServe, TensorFlow Serving, MLflow registry
- **Prediction APIs**: Real-time (<50ms latency) + batch processing
- **Training Pipelines**: Automated training with Airflow + Optuna
- **AI Features**: Workflow suggestions, anomaly detection, NLP search

**Klíčový princip:** AI navrhuje, člověk schvaluje. Žádné autopilot změny.

---

## 🔒 Security & Governance First

### Bezpečnostní Pravidla (Non-Negotiable)

1. **Zero Direct Access**
   - AI NIKDY nemá přímý přístup k PostgreSQL, Loki, MinIO, Kafka
   - Pouze přes BFF/MCP tools s RBAC enforcement

2. **Metamodel-Driven Authorization**
   - Každý AI dotaz prochází metamodel filtrem
   - PII/Sensitive fields jsou maskovány nebo odepřeny
   - Tenant isolation: AI vidí POUZE data svého tenanta

3. **Propose/Approve Workflow**
   - Všechny AI návrhy jsou DRAFT
   - Vyžadují ruční schválení Architect/Admin role
   - Verzované s rollback možností

4. **Comprehensive Audit**
   - Každý AI call logován: kdo, kdy, nad čím, výsledek
   - UI pro review AI aktivit (per tenant/global)
   - Retention policy dle compliance

5. **Per-Tenant AI Toggle**
   - Tenant Admin může AI vypnout/zapnout
   - Granularita: per-feature (designer ON, navigator OFF)
   - Default: AI disabled pro nové tenanty

---

## 📋 Stories Overview

| Category | ID | Story | Status | LOC | Effort | Priority | Dependencies |
|----------|----|----|--------|-----|--------|----------|--------------|
| **Metamodel AI** | [AI-001](./stories/AI-001-ai-metamodel-designer/README.md) | AI Metamodel Designer | ⏳ PENDING | ~1,200 | 3d | 🔥 HIGH | Metamodel Studio, MCP |
| **Metamodel AI** | [AI-002](./stories/AI-002-ai-metamodel-reviewer/README.md) | AI Metamodel Reviewer | ⏳ PENDING | ~800 | 2d | 🔥 HIGH | AI-001, Validator |
| **Metamodel AI** | [AI-003](./stories/AI-003-ai-ui-spec-generator/README.md) | AI UI Spec Generator | ⏳ PENDING | ~900 | 2d | 🟡 MEDIUM | Metamodel, UI Components |
| **Metamodel AI** | [AI-004](./stories/AI-004-ai-refactoring-assistant/README.md) | AI Refactoring Assistant | ⏳ PENDING | ~700 | 2d | 🟡 MEDIUM | AI-001, AI-002 |
| **Metamodel AI** | [AI-005](./stories/AI-005-ai-security-compliance/README.md) | AI Security & Compliance | ⏳ PENDING | ~1,100 | 3d | 🔥 HIGH | Metamodel, RBAC |
| **Metamodel AI** | [AI-006](./stories/AI-006-ai-metamodel-navigator/README.md) | AI Metamodel Navigator | ⏳ PENDING | ~1,000 | 3d | 🟢 LOW | RAG, Metamodel Docs |
| **AI Tools** | [AI-007](./stories/AI-007-mcp-server/README.md) | MCP Server Implementation | 🟡 IN PROGRESS | ~1,200 | 2d | 🔥 HIGH | Node.js, MCP SDK |
| **AI Tools** | [AI-008](./stories/AI-008-copilot-integration/README.md) | GitHub Copilot Integration | ✅ DONE | ~500 | - | 🔥 HIGH | Copilot Instructions |
| **AI Tools** | [AI-009](./stories/AI-009-test-generation/README.md) | AI Test Generation | ⏳ PENDING | ~600 | 2d | 🟡 MEDIUM | MCP, Prompt Engineering |
| **AI Tools** | [AI-010](./stories/AI-010-code-review-bot/README.md) | AI Code Review Bot | 🔮 PLANNED | ~800 | 3d | 🟢 LOW | GitHub Actions, LLM |
| **ML Platform** | [ML-001](./stories/ML-001-model-serving/README.md) | ML Model Serving | ⏳ PENDING | ~800 | 2d | 🔥 HIGH | TorchServe, MLflow |
| **ML Platform** | [ML-002](./stories/ML-002-prediction-api/README.md) | Prediction API Gateway | ⏳ PENDING | ~600 | 2d | 🔥 HIGH | REST, Caching |
| **ML Platform** | [ML-003](./stories/ML-003-training-pipeline/README.md) | Training Pipeline | ⏳ PENDING | ~700 | 2d | 🟡 MEDIUM | Airflow, Optuna |
| **ML Platform** | [ML-004](./stories/ML-004-ai-features/README.md) | AI-Powered Features | ⏳ PENDING | ~500 | 1d | 🟡 MEDIUM | ML-001, ML-002 |
| **TOTAL** | | **14 stories** | **1 DONE / 1 IN PROGRESS / 12 PENDING** | **~11,500** | **30d** | **6 HIGH / 5 MED / 3 LOW** | **3 categories** |

### Progress by Category
- **Metamodel AI (6 stories):** 0% done, 15 days effort - Core intelligent assistant features
- **AI Tools (4 stories):** 40% done (Copilot ✅, MCP 🟡), 7 days effort - Developer productivity
- **ML Platform (4 stories):** 0% done, 7 days effort - Model serving & predictions

---

## 📖 Detailed Stories

### AI-001: AI Metamodel Designer

**Status:** ⏳ **PLANNED**  
**Effort:** 3 dny (~1,200 LOC)  
**Priority:** 🔥 **HIGH**

#### User Stories

**US-001.1: Draft Entity from Text**
> "Jako **Solution Architect** chci z textového zadání nechat AI navrhnout entitní model jako draft, abych ho jen zrevidoval místo ručního kreslení od nuly."

**US-001.2: Intelligent Field Suggestions**
> "Jako **Architect** chci, aby AI navrhla nejen entity ale i pole s typy, validacemi a vazbami, abych měl 80% práce hotové automaticky."

**US-001.3: Diff Preview**
> "Jako **Power User** chci vidět diff mezi současným metamodelem a AI návrhem, abych chápal dopady změn před schválením."

**US-001.4: Validation Before Apply**
> "Jako **Architect** chci kliknout 'Validate' na AI návrh a dostat report o konfliktech, kolizích a breaking changes, než to schválím."

#### Functional Requirements

**Input:**
- Textový popis záměru (např. "Chci model pro řízení reklamací zákazníků")
- Kontext: jaké entity už existují, jaké konvence platí

**AI Output (Draft Metamodel):**
1. **Entities:**
   - Navržené entity (Complaint, Customer, SLAEvent...)
   - S naming conventions (PascalCase, singular)
   
2. **Fields:**
   - Datové typy (string, int, date, uuid, reference...)
   - Povinnost (required/optional)
   - Délky, ranges, enumy
   - Default values
   
3. **Relationships:**
   - 1:N, N:M, FK references
   - Cascade delete rules
   - Bidirectional navigation
   
4. **Validations:**
   - Required fields
   - Pattern constraints (email, phone...)
   - Range constraints (min/max)
   - Custom validators
   
5. **Metadata:**
   - Audit fields (createdBy, createdAt...)
   - Workflow hooks (onBeforeCreate, onAfterUpdate...)
   - Security annotations (PII, sensitive)

**System Actions:**
1. Display draft as **diff** in Metamodel Studio
2. Highlight:
   - 🟢 New entities/fields
   - 🟡 Modified entities/fields
   - 🔴 Potential conflicts
3. **Validation checks:**
   - Name conflicts (entity/field already exists)
   - Type mismatches (FK to non-existent entity)
   - Circular dependencies
   - Breaking changes (removed required fields)
4. **Review mode:**
   - Architect can edit, comment, accept/reject per entity/field
   - Save as "Proposed Metamodel v2.1-draft"

**Security:**
- AI může navrhnout PII fields, ale MUSÍ je označit
- Návrhy jdou pouze přes schválenou MCP tool ("propose_metamodel_draft")
- Žádné přímé DB writes

#### Acceptance Criteria

- [ ] UI: Text input "Describe your data model need"
- [ ] AI navrhne minimálně 3 entity + fields + relationships
- [ ] Draft viditelný jako diff v Metamodel Studio
- [ ] "Validate" button spustí konflikt checker
- [ ] Validation report zobrazí errors/warnings/suggestions
- [ ] Architect může upravit návrh před schválením
- [ ] "Apply Draft" vyžaduje ARCHITECT role
- [ ] Applied changes verzované (Metamodel v2.1)
- [ ] Audit log: kdo schválil, kdy, co se změnilo
- [ ] Rollback možnost (revert to v2.0)

#### Implementation Tasks

**T1: MCP Tool - Propose Metamodel Draft** (0.5d)
- `POST /api/mcp/metamodel/propose-draft`
- Input: text prompt + current metamodel context
- Output: JSON draft (entities, fields, relationships)
- Security: RBAC check (ARCHITECT role required)

**T2: Metamodel Diff Engine** (1d)
- Compare current vs draft
- Generate diff: added/modified/removed
- Conflict detection (name clashes, type changes)
- Breaking change analysis

**T3: Validation Service** (1d)
- Check entity/field naming conventions
- Validate FK references (target entity exists)
- Detect circular dependencies
- Flag PII/sensitive fields

**T4: Metamodel Studio UI - Draft Review** (0.5d)
- Display diff as expandable tree
- Color-coded changes (green/yellow/red)
- Inline editing of draft
- Comment threads per entity/field
- "Apply Draft" / "Reject" buttons

**Implementation Details:**
```
POST /api/mcp/metamodel/propose-draft
Request:
{
  "prompt": "Model pro řízení reklamací zákazníků",
  "tenantId": "acme-corp",
  "currentMetamodelVersion": "2.0"
}

Response (AI Draft):
{
  "draftId": "draft-uuid",
  "version": "2.1-draft",
  "entities": [
    {
      "name": "Complaint",
      "fields": [
        {"name": "id", "type": "uuid", "required": true, "primaryKey": true},
        {"name": "customerId", "type": "reference", "targetEntity": "Customer", "required": true},
        {"name": "subject", "type": "string", "maxLength": 200, "required": true},
        {"name": "description", "type": "text"},
        {"name": "priority", "type": "enum", "values": ["LOW", "MEDIUM", "HIGH", "CRITICAL"]},
        {"name": "status", "type": "enum", "values": ["NEW", "IN_PROGRESS", "RESOLVED", "CLOSED"]},
        {"name": "createdAt", "type": "timestamp", "audit": true},
        {"name": "createdBy", "type": "reference", "targetEntity": "User", "audit": true}
      ],
      "relationships": [
        {"type": "manyToOne", "field": "customerId", "targetEntity": "Customer", "cascadeDelete": false}
      ]
    },
    {
      "name": "ComplaintComment",
      "fields": [
        {"name": "id", "type": "uuid", "required": true, "primaryKey": true},
        {"name": "complaintId", "type": "reference", "targetEntity": "Complaint", "required": true},
        {"name": "comment", "type": "text", "required": true},
        {"name": "createdAt", "type": "timestamp", "audit": true},
        {"name": "createdBy", "type": "reference", "targetEntity": "User", "audit": true}
      ]
    }
  ],
  "validationReport": {
    "errors": [],
    "warnings": [
      "Entity 'Customer' referenced but not defined in draft (assume exists)"
    ],
    "suggestions": [
      "Consider adding SLA fields to Complaint (e.g., resolveBy: timestamp)"
    ]
  }
}
```

---

### AI-002: AI Metamodel Reviewer

**Status:** ⏳ **PLANNED**  
**Effort:** 2 dny (~800 LOC)  
**Priority:** 🔥 **HIGH**

#### User Stories

**US-002.1: AI Review on Metamodel Change**
> "Jako **Architect** chci, aby AI při schvalování změn metamodelu udělala bezpečnostní a návrhový review, abych snížil riziko blbostí v produkci."

**US-002.2: Compliance Guardrails**
> "Jako **DPO** chci, aby AI automaticky flagovala PII pole bez příslušných security anotací, abychom dodrželi GDPR."

**US-002.3: Best Practice Suggestions**
> "Jako **Tech Lead** chci dostat AI doporučení na zlepšení metamodelu (indexy, naming, normalizace), ne jen error hlášky."

#### Functional Requirements

**Trigger:**
- Před schválením metamodel draft (AI-001 output)
- Manuální "Run AI Review" button v Metamodel Studio
- Automaticky při CI/CD (optional)

**AI Review Checks:**

1. **Security & Compliance:**
   - PII fields bez `@PII` annotation
   - Sensitive data bez `@Encrypted` nebo `@Masked`
   - Public API exposure nebezpečných fields

2. **Data Quality:**
   - Chybějící required fields (id, tenantId, audit...)
   - Duplicitní entities (Customer vs Client vs Person)
   - Inconsistent naming (user_name vs userName)

3. **Performance:**
   - Chybějící indexy na FK nebo často filtrovaných polích
   - Text fields bez length limitu (risk of bloat)
   - N+1 query risks (missing eager loading hints)

4. **Design Patterns:**
   - Audit fields (createdBy, updatedAt) missing
   - Soft delete pattern not applied
   - Versioning fields missing where appropriate

**Output:**
- **Report Categories:**
  - 🔴 **CRITICAL** (must fix before production)
  - 🟡 **WARNING** (should fix, risky)
  - 🟢 **SUGGESTION** (nice to have, best practice)

- **Human-Readable Messages:**
  - ❌ "Field 'socialSecurityNumber' appears to be PII but lacks @PII annotation"
  - ⚠️ "Entity 'Order' has FK 'customerId' but no index - this will slow down lookups"
  - 💡 "Consider adding 'updatedAt' field to 'Product' for audit trail"

#### Acceptance Criteria

- [ ] "Run AI Review" button in Metamodel Studio
- [ ] Review runs automatically before "Apply Draft"
- [ ] Report shows 3 severity levels (Critical/Warning/Suggestion)
- [ ] Each issue has:
  - Description (human-readable)
  - Affected entity/field
  - Recommended fix
  - "Accept Risk" / "Fix Now" buttons
- [ ] Critical issues BLOCK draft application (unless override with ADMIN role)
- [ ] Review results saved with draft (versioned)
- [ ] Architect can mark issues as "Acknowledged" or "Ignored"

#### Implementation Tasks

**T1: AI Review MCP Tool** (0.5d)
- `POST /api/mcp/metamodel/review-draft`
- Input: draft metamodel JSON
- Output: review report (issues array)

**T2: Review Rule Engine** (1d)
- Security rules (PII detection, encryption checks)
- Performance rules (index analysis, FK checks)
- Design pattern rules (audit fields, naming conventions)
- Pluggable architecture (new rules without code changes)

**T3: Review UI Component** (0.5d)
- Display review report in Studio
- Group issues by severity
- Inline "Fix" actions (add @PII, add index, rename field)
- "Accept Risk" with mandatory comment

---

### AI-003: AI UI Spec Generator

**Status:** ⏳ **PLANNED**  
**Effort:** 2 dny (~900 LOC)  
**Priority:** 🟡 **MEDIUM**

#### User Stories

**US-003.1: Auto-Generate Form Layout**
> "Jako **FE Developer** chci dostat z metamodelu AI-navrženou UI specifikaci, abych minimalizoval ruční drátování fieldů."

**US-003.2: Smart Component Mapping**
> "Jako **UX Designer** chci, aby AI navrhla správné komponenty (datepicker pro date, autocomplete pro FK), ne jen generic textboxes."

**US-003.3: List View Configuration**
> "Jako **Product Owner** chci AI návrh defaultních sloupců a filtrů pro list views, abych nemusel specifikovat každý detail."

#### Functional Requirements

**Input:** Metamodel entity definition

**AI Output (UI Spec):**

1. **Form Layout:**
   ```json
   {
     "entity": "Complaint",
     "formLayout": {
       "sections": [
         {
           "title": "Basic Information",
           "fields": [
             {"name": "subject", "component": "TextInput", "width": "full", "order": 1},
             {"name": "priority", "component": "Select", "width": "half", "order": 2},
             {"name": "status", "component": "Select", "width": "half", "order": 3}
           ]
         },
         {
           "title": "Details",
           "fields": [
             {"name": "description", "component": "TextArea", "width": "full", "rows": 5}
           ]
         },
         {
           "title": "Customer",
           "fields": [
             {"name": "customerId", "component": "Autocomplete", "targetEntity": "Customer", "displayField": "name"}
           ]
         }
       ]
     }
   }
   ```

2. **Field → Component Mapping:**
   - `type: "date"` → `<DatePicker>`
   - `type: "reference"` → `<Autocomplete targetEntity="X">`
   - `type: "enum"` → `<Select>` or `<RadioGroup>`
   - `type: "boolean"` → `<Switch>` or `<Checkbox>`
   - `type: "text"` (long) → `<TextArea>`
   - `@PII` annotation → `<MaskedInput>` + role-based visibility

3. **List View Spec:**
   ```json
   {
     "entity": "Complaint",
     "listView": {
       "columns": [
         {"field": "id", "label": "ID", "width": 100, "sortable": true},
         {"field": "subject", "label": "Subject", "width": 300, "sortable": true},
         {"field": "priority", "label": "Priority", "width": 120, "filterable": true, "component": "PillBadge"},
         {"field": "status", "label": "Status", "width": 150, "filterable": true},
         {"field": "createdAt", "label": "Created", "width": 180, "sortable": true, "format": "datetime"}
       ],
       "defaultSort": {"field": "createdAt", "direction": "desc"},
       "filters": [
         {"field": "status", "type": "multi-select"},
         {"field": "priority", "type": "multi-select"},
         {"field": "createdAt", "type": "date-range"}
       ]
     }
   }
   ```

#### Acceptance Criteria

- [ ] MCP tool: `POST /api/mcp/ui-spec/generate`
- [ ] Input: entity name (e.g., "Complaint")
- [ ] Output: UI spec JSON (form + list view)
- [ ] Form layout respektuje field types
- [ ] PII fields mají `readOnly: true` pro non-privileged roles
- [ ] UI spec editovatelný v Metamodel Studio
- [ ] Generický frontend může UI spec renderovat (žádný hardcoded JSX)
- [ ] Preview mode: "Show Generated Form" button

#### Implementation Tasks

**T1: Component Mapper** (0.5d)
- Rules: field type → UI component
- PII handling: masking, role visibility
- Reference fields: autocomplete config

**T2: Layout Optimizer** (1d)
- Section grouping (related fields together)
- Responsive layout (half/full width)
- Field ordering (important fields first)

**T3: List View Generator** (0.5d)
- Column selection (most important fields)
- Default filters (enum/reference fields)
- Sorting hints

---

### AI-004: AI Refactoring Assistant

**Status:** ⏳ **PLANNED**  
**Effort:** 2 dny (~700 LOC)  
**Priority:** 🟡 **MEDIUM**

#### User Stories

**US-004.1: Find Duplicates**
> "Jako **Architect** chci, aby AI navrhla konsolidaci entit a fieldů přes celý systém, abychom neměli bordel po 3 letech vývoje."

**US-004.2: Consistency Checker**
> "Jako **Tech Lead** chci vidět report, kde stejný koncept (adresa, telefon) je definován 5× různě, a dostat návrh na unifikaci."

**US-004.3: Shared Types Suggestion**
> "Jako **Architect** chci, aby AI navrhla shared types (Address, PhoneNumber) místo opakovaných field definic."

#### Functional Requirements

**Scan Scope:** Celý metamodel catalog (všechny entity)

**AI Analysis:**

1. **Duplicate Detection:**
   - Entities: "Customer" vs "Client" vs "Person" (semantic similarity)
   - Fields: "email" defined 20× with different constraints
   - Enums: "Status" enum duplicated across entities

2. **Consistency Check:**
   - Address fields: někde `street, city, zip`, jinde `address_line1, postal_code`
   - Phone fields: někde `string`, jinde `international format required`
   - Date fields: někde `date`, jinde `timestamp`, jinde `string`

3. **Shared Type Candidates:**
   - Address (street, city, postalCode, country) → použito 10×
   - PhoneNumber (country_code, number, verified) → použito 8×
   - MonetaryAmount (value, currency) → použito 15×

**Output:**
- **Refactoring Proposals:**
  ```json
  [
    {
      "type": "MERGE_ENTITIES",
      "suggestion": "Merge 'Client' and 'Customer' into single 'Customer' entity",
      "affected": ["Client", "Customer"],
      "impact": "12 references need update",
      "breaking": true
    },
    {
      "type": "CREATE_SHARED_TYPE",
      "suggestion": "Extract 'Address' as shared type",
      "occurrences": 10,
      "entities": ["Customer", "Supplier", "Employee", "Warehouse"...],
      "fields": ["street", "city", "postalCode", "country"]
    }
  ]
  ```

#### Acceptance Criteria

- [ ] "Run Refactoring Analysis" button in Studio
- [ ] Report shows duplicates/inconsistencies/opportunities
- [ ] Each proposal has:
  - Description
  - Affected entities/fields
  - Impact assessment (# of changes)
  - Breaking change flag
- [ ] Actions: "Accept" / "Ignore" / "Remind Later"
- [ ] Accepted proposals → draft metamodel changes
- [ ] Ignored proposals → saved in "refactoring_ignored" table

#### Implementation Tasks

**T1: Semantic Similarity Engine** (1d)
- Entity name similarity (word2vec or LLM embeddings)
- Field name + type similarity
- Enum value matching

**T2: Shared Type Extractor** (0.5d)
- Find repeated field patterns
- Group by semantic meaning
- Suggest shared type structure

**T3: Refactoring Proposal UI** (0.5d)
- Display proposals as cards
- Accept/Ignore actions
- Preview impact (diff view)

---

### AI-005: AI Security & Compliance

**Status:** ⏳ **PLANNED**  
**Effort:** 3 dny (~1,100 LOC)  
**Priority:** 🔥 **HIGH**

#### User Stories

**US-005.1: PII Auto-Detection**
> "Jako **DPO** chci, aby AI i vývojáři měli jasně řízený přístup k PII podle metamodelu, ne podle pocitu."

**US-005.2: Security Annotation Suggestions**
> "Jako **Security Officer** chci, aby AI navrhla security anotace (@PII, @Encrypted, @Masked) na fields, které to vyžadují."

**US-005.3: Anonymization Strategy**
> "Jako **Compliance Officer** chci, aby AI dotazy nad produkčními daty automaticky anonymizovaly PII podle metamodel policy."

#### Functional Requirements

**Metamodel Annotations (New):**
```yaml
entity: Customer
  fields:
    - name: email
      type: string
      security:
        classification: PII
        masking: partial  # show only domain
        encryption: at_rest
        
    - name: socialSecurityNumber
      type: string
      security:
        classification: HIGHLY_SENSITIVE
        masking: full  # show ***-**-****
        encryption: at_rest
        access_roles: [DPO, ADMIN]
        
    - name: creditCardNumber
      type: string
      security:
        classification: PCI_DSS
        masking: last_four  # show **** **** **** 1234
        encryption: at_rest_and_transit
        access_roles: [PAYMENT_PROCESSOR]
```

**AI Capabilities:**

1. **Auto-Classify Fields:**
   - Scan field names: "email", "ssn", "creditCard", "password" → flag as PII/sensitive
   - Scan field values (optional): detect patterns (credit card format, email regex)

2. **Suggest Security Annotations:**
   - "Field 'dateOfBirth' should have `@PII` annotation"
   - "Field 'salary' should be `@Encrypted` at rest"

3. **Enforce Access Control:**
   - AI query: "Show me all customers"
   - System filters out PII fields UNLESS user has DPO role
   - Or returns masked values: `email: "j***@example.com"`

4. **Anonymization for AI:**
   - When AI tool requests data:
   - PII fields replaced with `<REDACTED>` or synthetic data
   - Only aggregates allowed (count, avg) not individual records

#### Acceptance Criteria

- [ ] Metamodel supports `security` field metadata
- [ ] AI can suggest security classifications
- [ ] MCP tools respect field security (mask PII)
- [ ] UI shows security badges (🔒 PII, 🔐 Encrypted)
- [ ] RBAC integration: only DPO role sees unmasked PII
- [ ] Audit log: who accessed PII fields, when
- [ ] Anonymization rules configurable per tenant

#### Implementation Tasks

**T1: Security Metadata Schema** (0.5d)
- Extend metamodel YAML with `security` block
- Validation: valid classifications, masking strategies

**T2: PII Detector** (1d)
- Field name matcher (regex + ML)
- Pattern detector (credit card, SSN formats)
- Suggest annotations

**T3: Masking Engine** (1d)
- Partial masking (email, phone)
- Full masking (SSN, credit card)
- Role-based unmasking

**T4: MCP Security Filter** (0.5d)
- Intercept MCP tool responses
- Apply masking before returning to AI
- Audit log integration

---

### AI-006: AI Metamodel Navigator

**Status:** ⏳ **PLANNED**  
**Effort:** 3 dny (~1,000 LOC)  
**Priority:** 🟢 **LOW** (nice-to-have)

#### User Stories

**US-006.1: Natural Language Query**
> "Jako **vývojář** chci položit otázku v přirozeném jazyce a dostat odpověď z aktuálního metamodelu, abych nemusel ručně hledat."

**US-006.2: Entity Comparison**
> "Jako **Business Analyst** chci se zeptat 'Jaký je rozdíl mezi Case a Ticket?' a dostat srozumitelnou odpověď."

**US-006.3: PII Discovery**
> "Jako **DPO** chci se zeptat 'Které entity obsahují osobní údaje?' a dostat seznam s detaily."

#### Functional Requirements

**UI:** Chat interface v Metamodel Studio

**Sample Queries:**
- "Jaké pole jsou v entitě Customer?"
- "Které entity mají FK na User?"
- "Kde se používá enum Status?"
- "Jaký je datový model pro objednávky?"
- "Které entity obsahují PII?"

**AI Backend:**
- **RAG (Retrieval-Augmented Generation):**
  - Vector DB: embeddings metamodel entities + docs
  - Query → retrieve relevant entities → generate answer
- **No Hallucination:**
  - AI odpovídá POUZE z metamodel + dokumentace
  - Pokud neví: "I don't have information about X"
- **Links to Source:**
  - Odpověď obsahuje link do Metamodel Studio
  - Např.: "Entity Order is defined here: [link]"

#### Acceptance Criteria

- [ ] Chat UI v Metamodel Studio
- [ ] AI odpovídá na dotazy o metamodelu
- [ ] Odpovědi obsahují odkazy na source (entity definitions)
- [ ] AI neprodukuje informace mimo metamodel
- [ ] Response time < 3s
- [ ] Audit: všechny dotazy logované

#### Implementation Tasks

**T1: RAG Setup** (1d)
- Embed metamodel entities (vector DB)
- Index documentation
- Query → retrieve → generate pipeline

**T2: Chat UI** (1d)
- React chat component
- Message history
- Code snippets rendering (YAML/JSON)

**T3: Answer Quality Filter** (1d)
- Validate AI answers against metamodel
- Reject hallucinated content
- Provide source links

---

## 📅 Implementation Timeline

**Phase 1: Foundation (5 dní)**
- AI-001: Metamodel Designer (3d)
- AI-002: Metamodel Reviewer (2d)

**Phase 2: Security & Generation (5 dní)**
- AI-005: Security & Compliance (3d)
- AI-003: UI Spec Generator (2d)

**Phase 3: Quality & Navigation (5 dní)**
- AI-004: Refactoring Assistant (2d)
- AI-006: Metamodel Navigator (3d)

**Total:** 15 dní (3 týdny @ 1 developer)

---

## 🔧 Technical Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Metamodel Studio UI                      │
│  (AI Designer, Reviewer, UI Generator, Chat Navigator)      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       AI/BFF Layer                          │
│  - Request validation (RBAC, tenant isolation)              │
│  - PII masking (based on metamodel security annotations)    │
│  - Audit logging (all AI calls)                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      MCP Server                             │
│  Tools:                                                     │
│  - propose_metamodel_draft                                  │
│  - review_metamodel_draft                                   │
│  - generate_ui_spec                                         │
│  - analyze_refactoring                                      │
│  - classify_pii_fields                                      │
│  - query_metamodel_rag                                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    AI Provider (LLM)                        │
│  - OpenAI GPT-4 / Claude / Local LLM                        │
│  - No direct DB/Loki/MinIO access                           │
│  - Only receives filtered/masked data from MCP              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend Services                          │
│  - Metamodel Service (CRUD, validation, versioning)         │
│  - RBAC Service (role checks)                               │
│  - Audit Service (log AI activities)                        │
│  - PostgreSQL (metamodel storage)                           │
└─────────────────────────────────────────────────────────────┘
```

### Security Flow

```
User (Architect) → Metamodel Studio UI
  → "Design entity from text: Complaint management"
  
Studio → POST /api/mcp/metamodel/propose-draft
  → BFF validates: user has ARCHITECT role
  → BFF checks: tenant AI enabled
  → BFF masks: no PII in prompt
  → BFF logs: audit entry created
  
BFF → MCP Tool: propose_metamodel_draft
  → MCP reads current metamodel (tenant-scoped)
  → MCP calls LLM with prompt + context
  → LLM generates draft entities JSON
  
MCP → BFF: draft metamodel
  → BFF validates: no conflicts, valid types
  → BFF sanitizes: removes any LLM hallucinations
  → BFF returns: draft + validation report
  
Studio displays draft as diff
  → Architect reviews, edits, approves
  → POST /api/metamodel/apply-draft
  → Backend creates new metamodel version
  → Audit: "Metamodel v2.1 created from AI draft by user@acme.com"
```

---

## 🎯 Success Metrics

**Adoption:**
- % of new entities created with AI assistance
- Time saved: manual design (2h) vs AI draft + review (30min)

**Quality:**
- % of AI drafts approved without major changes
- # of security issues caught by AI Reviewer
- Reduction in metamodel inconsistencies (duplicates, naming)

**Security:**
- 100% of AI calls logged in audit
- 0 PII leaks to LLM (validated via tests)
- % of fields correctly classified as PII by AI

**Developer Experience:**
- NPS score for AI Metamodel features
- Time to onboard new developer (with AI Navigator)

---

## 🚨 Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| AI hallucinates entities | 🔴 HIGH | Validation layer, human approval required |
| PII leak to LLM | 🔴 CRITICAL | Masking at BFF, audit all AI calls, no direct DB access |
| Over-reliance on AI | 🟡 MEDIUM | Training: AI assists, doesn't replace architects |
| LLM API costs | 🟡 MEDIUM | Rate limiting, caching, per-tenant quotas |
| LLM availability | 🟢 LOW | Fallback to manual mode, queue requests |

---

## 📚 Documentation

**For Architects:**
- "How to use AI Metamodel Designer"
- "Best practices for reviewing AI drafts"
- "Security annotations guide"

**For Developers:**
- "MCP Tools API reference"
- "Metamodel security model"
- "AI Navigator query examples"

**For Admins:**
- "Tenant AI configuration"
- "Audit log analysis"
- "AI quotas and rate limits"

---

**Last Updated:** 9. listopadu 2025  
**Owner:** AI/Platform Team  
**Stakeholders:** Architecture, Security, Product
