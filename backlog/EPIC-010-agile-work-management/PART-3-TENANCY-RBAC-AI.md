# EPIC-010: Agile Work Management - Part 3/3

**Tenancy, RBAC, Licensing & AI/MCP** (Themes 6-7, Stories AWM14-AWM17)

---

## Theme 6: Tenancy, RBAC & Licensing

### AWM14: Modul jako Tenant Feature

**As a** platform admin  
**I want** AWM jako tenant-level feature flag  
**So that** můžu povolit/zakázat AWM per tenant

**Acceptance Criteria:**

✅ **Tenant configuration:**
- Admin realm UI: "Enable Agile Work Management" toggle (per tenant)
- Uloženo v `tenant_features` table (tenant_id, feature_key='AWM', enabled=true/false)
- Default: disabled (opt-in)

✅ **Feature enforcement:**
- Pokud AWM disabled pro tenant → HTTP 403 Forbidden na všechny AWM endpoints
- Frontend: AWM menu skrytý pokud feature disabled
- Middleware check na backendu (check feature flag before processing request)

✅ **Migration path:**
- Existing tenants: AWM initially disabled
- New tenants: Admin může povolit během onboardingu
- Bulk enable API (admin-only): enable AWM pro multiple tenants najednou

✅ **Audit logging:**
- Log pokud admin enable/disable AWM (Loki event: `awm_feature_toggled`)
- Visible in admin audit trail

**BE Impact:**
- Tenant features table (`tenant_id`, `feature_key`, `enabled`, `config_json`)
- Feature flag middleware (check on every AWM API call)
- Admin API: POST `/api/v1/admin/tenants/{id}/features/awm` (enable/disable)

**FE Impact:**
- Admin UI: Feature toggle switch (tenant settings page)
- AWM menu conditional rendering (check feature flag from user context)
- Error page pokud user access AWM when disabled (friendly message)

**Security/Tenancy:**
- Feature flags tenant-scoped (každý tenant má vlastní config)
- Only `CORE_PLATFORM_ADMIN` (admin realm) can toggle features

**Test Coverage:**
- Unit: Feature flag middleware logic
- Integration: Enable AWM → verify endpoints accessible, Disable → verify 403
- E2E: Admin toggle feature → tenant user see/don't see AWM menu

**LOC:** ~300 (BE: ~150, FE: ~100, tests: ~50)  
**Effort:** 6h  
**Priority:** MUST HAVE (Phase 3)

---

### AWM15: Licensing Readiness

**As a** platform owner  
**I want** AWM připravený na licencování  
**So that** můžu v budoucnu nabízet AWM jako paid feature

**Acceptance Criteria:**

✅ **License check hooks:**
- License validation API endpoint (GET `/api/v1/licenses/awm/status`)
- Response: `{enabled: true, expires_at: '2026-12-31', user_limit: 50}`
- Pokud license invalid/expired → show warning banner (FE), log event (BE)

✅ **User limit enforcement (optional):**
- Pokud license má `user_limit` → count active AWM users
- Pokud limit překročen → prevent new user assignments, show admin warning
- Grace period (30 days) before hard block

✅ **License UI:**
- Admin realm: License overview page (`/admin/licenses`)
- Show AWM license status, expiration, user count
- "Contact sales" link pokud license expired

✅ **Fallback behavior:**
- Pokud licensing system NOT implemented → default behavior: AWM enabled (open)
- Allow smooth transition to paid licensing v budoucnu

**BE Impact:**
- License service interface (contract for future licensing system)
- Mock implementation (always returns `enabled: true` pro development)
- Pluggable architecture (easy swap mock → real licensing EPIC)

**FE Impact:**
- License status banner (warning pokud expiring soon)
- Admin license page (table view, renewal link)

**Security/Tenancy:**
- License je tenant-scoped (každý tenant má vlastní license)
- Only `CORE_PLATFORM_ADMIN` vidí license details

**Test Coverage:**
- Unit: License validation logic
- Integration: Mock license service returns expected responses
- E2E: Verify warning banner pokud license expiring

**LOC:** ~250 (BE: ~150, FE: ~80, tests: ~20)  
**Effort:** 5h  
**Priority:** NICE TO HAVE (Phase 3)  
**Note:** Připravenost na budoucí licensing EPIC, ne implementace licencování

---

## Theme 7: AI/MCP Hooks (Design-time Assist)

### AWM16: MCP/AI Workflow & Config Suggestions

**As a** admin  
**I want** AI návrhy workflow a board konfigurace  
**So that** můžu rychle setupnout AWM podle best practices

**Acceptance Criteria:**

✅ **MCP tool: `generate_awm_workflow`**
- Input: team type (kanban, scrum, bugfix, custom description)
- Output: Workflow definition JSON (states, transitions, guards)
- Example:
  ```json
  {
    "team_type": "scrum",
    "description": "Software development team, 2-week sprints"
  }
  → generates Scrum workflow (To Do → In Progress → In Test → Done)
  ```

✅ **MCP tool: `generate_board_config`**
- Input: workflow definition, team preferences (swimlanes, WIP limits)
- Output: Board configuration JSON (columns, swimlanes, WIP)

✅ **MCP tool: `suggest_work_item_fields`**
- Input: use case description (např. "bug tracking pro mobile app")
- Output: Suggested custom fields (steps_to_reproduce, device_model, OS_version, severity)

✅ **FE Integration:**
- "AI Assist" button v workflow/board editor
- Modal: describe your team → AI generates config → preview → accept/edit

**BE Impact:**
- MCP server endpoints (EPIC-009 AI/MCP Integration)
- Tool implementations (`generate_awm_workflow`, `generate_board_config`, `suggest_work_item_fields`)
- Prompt engineering (best practices embedded in prompts)

**FE Impact:**
- AI assist modal (input form + preview)
- Config preview component (diff view: current vs suggested)
- Apply button (merge AI suggestion into config)

**Security/Tenancy:**
- AI suggestions tenant-scoped (no cross-tenant data leakage)
- Only `AWM_ADMIN` can use AI assist

**Test Coverage:**
- Unit: MCP tool invocation, response parsing
- Integration: Call MCP endpoint → verify valid workflow JSON returned
- E2E: Click "AI Assist" → describe team → verify workflow generated

**LOC:** ~400 (MCP tools: ~200, FE: ~150, tests: ~50)  
**Effort:** 10h  
**Priority:** NICE TO HAVE (Phase 3)  
**Dependency:** EPIC-009 AI/MCP Integration

---

### AWM17: AI Field & Validation Generation

**As a** admin  
**I want** AI návrhy custom fields a validací  
**So that** nemusím manually designing entity schema

**Acceptance Criteria:**

✅ **MCP tool: `generate_entity_schema`**
- Input: entity description (např. "user story for e-commerce platform")
- Output: Entity schema JSON (fields, types, validations)
- Example:
  ```json
  {
    "description": "User story for e-commerce checkout flow"
  }
  → generates fields:
    - acceptance_criteria (rich-text)
    - user_persona (enum: guest, registered, premium)
    - business_value (enum: high, medium, low)
    - technical_complexity (integer 1-10)
  ```

✅ **MCP tool: `suggest_validation_rules`**
- Input: field definition
- Output: Validation rules (regex, required, min/max, custom logic)

✅ **Auto-populate description:**
- AI can generate work item description from summary
- User writes summary: "Add payment gateway integration"
- AI suggests description: "As a customer, I want to pay via credit card so that I can complete my purchase securely."

✅ **FE Integration:**
- "Generate Description" button v work item form
- "Suggest Fields" button v entity schema editor

**BE Impact:**
- MCP tool implementations (`generate_entity_schema`, `suggest_validation_rules`, `auto_description`)
- Prompt templates (user story format, acceptance criteria template)

**FE Impact:**
- AI assist buttons (entity editor, work item form)
- Description generation modal (summary input → AI description output)

**Security/Tenancy:**
- AI suggestions tenant-scoped
- No PII sent to AI (anonymize tenant-specific data in prompts)

**Test Coverage:**
- Unit: Prompt generation, response parsing
- Integration: Call MCP tool → verify valid schema returned
- E2E: Generate description → verify populated in form

**LOC:** ~350 (MCP tools: ~200, FE: ~100, tests: ~50)  
**Effort:** 8h  
**Priority:** NICE TO HAVE (Phase 3)  
**Dependency:** EPIC-009 AI/MCP Integration

---

## 📦 Story Overview (Part 3 - Themes 6-7)

| Story | Name | Theme | LOC | Effort | Phase | Priority |
|-------|------|-------|-----|--------|-------|----------|
| AWM14 | Tenant Feature Flag | 6 | ~300 | 6h | 3 | MUST |
| AWM15 | Licensing Readiness | 6 | ~250 | 5h | 3 | NICE |
| AWM16 | AI Workflow Suggestions | 7 | ~400 | 10h | 3 | NICE |
| AWM17 | AI Field Generation | 7 | ~350 | 8h | 3 | NICE |
| **SUBTOTAL** | **Themes 6-7** | | **~1,300** | **29h** | | |

---

## 🎯 EPIC-010 Complete Summary

### Total Breakdown

**All 3 Parts Combined:**

| Part | Themes | Stories | LOC | Effort | Phase |
|------|--------|---------|-----|--------|-------|
| Part 1 | 1-2 (Core Model & Workflow) | AWM1-AWM6 | ~2,500 | ~54h | 1 |
| Part 2 | 3-5 (Boards, DMS, Reporting) | AWM7-AWM13 | ~4,000 | ~90h | 2 |
| Part 3 | 6-7 (Tenancy, RBAC, AI/MCP) | AWM14-AWM17 | ~1,300 | ~29h | 3 |
| **TOTAL** | **7 Themes** | **17 Stories** | **~7,800** | **~173h** | |

### Phase Distribution

**Phase 1 - Foundation (MUST HAVE):**
- AWM1-AWM6: Core entity model, workflow engine, šablony
- Effort: ~54h
- Priority: All MUST or SHOULD HAVE

**Phase 2 - User Experience (MUST/SHOULD HAVE):**
- AWM7-AWM13: Boards, backlog, sprints, DMS, comments, reports, monitoring
- Effort: ~90h
- Priority: AWM7-AWM8 MUST, rest SHOULD

**Phase 3 - Enterprise Readiness (NICE TO HAVE):**
- AWM14-AWM17: Tenant features, licensing hooks, AI assist
- Effort: ~29h
- Priority: AWM14 MUST, rest NICE TO HAVE

---

## 🔗 Complete Dependencies

**Core Platform (MUST):**
- ✅ EPIC-005: Metamodel Generator Studio (entity definitions, schema flexibility)
- ✅ EPIC-006: Workflow Engine (workflow execution, state transitions)
- ✅ EPIC-000: Security & RBAC (role-based permissions, tenant isolation)
- ✅ EPIC-007: Multi-tenant Infrastructure (tenant=subdomain=realm, hard boundary)

**Optional Integrations (NICE TO HAVE):**
- EPIC-008: DMS (AWM10 - document attachments, version history)
- EPIC-003: Monitoring & Observability (AWM13 - Grafana dashboards, Loki events)
- EPIC-009: AI/MCP Integration (AWM16-AWM17 - workflow suggestions, field generation)
- EPIC-011: n8n Workflow Automation (future: orchestration hooks, webhooks on work item events)

**No Architecture Changes:**
- ✅ Multi-tenant model preserved: `tenant = subdoména = realm`
- ✅ No licensing system implementation (AWM15 only prepares hooks)
- ✅ No external SaaS dependencies (all core-platform based)

---

## 🚀 Implementation Plan (Fázování)

### Fáze 1: Metamodel + Workflow (4-6 týdnů)

**Stories:** AWM1-AWM6  
**Deliverables:**
- Work Item entity v metamodelu (typy, fields, validace)
- Hierarchie a linky (Epic → Story → Subtask)
- 3 workflow šablony (Kanban, Scrum, Bugflow)
- Workflow engine integrace (guard rules, eventy)
- Workflow viz v detailu (timeline, SLA warnings)

**Milestone:** Základní work item lze vytvořit, provázat, přesunout workflow

---

### Fáze 2: UI Modul (6-8 týdnů)

**Stories:** AWM7-AWM11  
**Deliverables:**
- Backlog view (filtering, drag-drop prioritizace)
- Board (Kanban/Scrum, swimlanes, WIP limits)
- Sprint management (planning, burndown, completion)
- DMS tab (file upload, preview)
- Komentáře (@mentions, notifications)

**Milestone:** Team může kompletně pracovat s AWM (backlog → board → sprint)

---

### Fáze 3: Reporting & Enterprise (3-4 týdny)

**Stories:** AWM12-AWM15  
**Deliverables:**
- Dashboard reporty (lead time, velocity, bottlenecks)
- Observabilita (Loki events, Prometheus metrics, Grafana)
- Tenant feature flag (enable/disable AWM per tenant)
- Licensing readiness hooks

**Milestone:** AWM ready for multi-tenant production deployment

---

### Fáze 4: AI Assist (2-3 týdny, volitelné)

**Stories:** AWM16-AWM17  
**Deliverables:**
- MCP tools (workflow generation, board config, field suggestions)
- FE AI assist buttons (workflow editor, entity schema)
- Auto-populate description from summary

**Milestone:** Admin může rychle setupnout AWM pomocí AI

---

## 📋 Testing Strategy

### Unit Tests (~20% LOC)

**Coverage:**
- Metamodel validace (AWM1, AWM3)
- Workflow guard logic (AWM5)
- Priority order calculation (AWM7)
- WIP limit checks (AWM8)
- Burndown calculation (AWM9)
- Lead time / Cycle time (AWM12)

**Target:** ~1,500 LOC tests

---

### Integration Tests (~15% LOC)

**Coverage:**
- Work item CRUD via API (AWM1-AWM2)
- Workflow transition API (AWM5)
- Board state API (AWM8)
- Sprint completion logic (AWM9)
- DMS upload/download (AWM10)
- Reports API (AWM12)

**Target:** ~1,200 LOC tests

---

### E2E Tests (~10% LOC)

**Scenarios:**
- **Tenant Lifecycle:**
  - Admin enable AWM → tenant user create work item → verify tenant isolation
- **Full Workflow:**
  - Create Epic → add Stories → drag to sprint → start sprint → move on board → complete → verify report
- **RBAC:**
  - AWM_VIEWER cannot edit → AWM_USER can edit → AWM_ADMIN can configure
- **Performance:**
  - Load 100 work items → verify backlog renders <2s
  - Drag-drop transition → verify <500ms response

**Target:** ~800 LOC tests (Playwright)

---

## 🎓 UX Expectations vs Jira Baseline

**What AWM Must Match:**
- ✅ Backlog view (filter, prioritize, bulk actions) - **AWM7**
- ✅ Board (drag-drop, swimlanes, WIP) - **AWM8**
- ✅ Sprints (planning, burndown, completion) - **AWM9**
- ✅ Workflow (states, transitions, guards) - **AWM4-AWM6**
- ✅ Comments (@mentions, timeline) - **AWM11**
- ✅ Attachments (files, preview) - **AWM10**
- ✅ Reports (lead time, velocity, bottlenecks) - **AWM12**

**What AWM Does Better:**
- ⭐ **Metamodel flexibility**: Custom fields BEZ DB migrace
- ⭐ **Multi-tenant native**: Tvrdá tenant izolace built-in
- ⭐ **RBAC granularity**: Permissions až na field level
- ⭐ **DMS integrace**: Dokumenty s verzováním (EPIC-008)
- ⭐ **Observabilita**: Loki/Prometheus/Grafana out-of-box
- ⭐ **AI assist**: Workflow suggestions, field generation

**What AWM Skips (deliberately):**
- ❌ JQL (Jira Query Language) - replaced by structured filters (AWM7)
- ❌ 100+ Jira apps ecosystem - fokus on core, extension via n8n (EPIC-011)
- ❌ Complex Jira admin UI - simplified tenant-based config

---

## ✅ Checklist: Ready for Implementation

**Architecture:**
- ✅ Žádná speciální magie mimo core (vše = metamodel + workflow + views)
- ✅ Multi-tenant preserved (tenant=subdomain=realm)
- ✅ RBAC defined (AWM_ADMIN, AWM_USER, AWM_VIEWER)
- ✅ Dependencies clear (EPIC-005, 006, 000, 007 required)

**Scope:**
- ✅ 17 stories well-defined (AC, BE/FE impact, LOC estimates)
- ✅ Phasing realistic (4 fáze, 13-21 týdnů total)
- ✅ Testing strategy (unit, integration, E2E)

**Brand:**
- ✅ No "Jira" trademark usage
- ✅ Module name: "Agile Work Management" (AWM)

**Future-proof:**
- ✅ Licensing readiness (AWM15 hooks)
- ✅ AI/MCP extensibility (AWM16-AWM17)
- ✅ n8n orchestration hooks (future EPIC integration)

---

**EPIC-010: Agile Work Management Module je kompletně nadefinován a připraven k implementaci!** 🚀
