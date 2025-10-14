# 🎯 Workflow + Studio EPIC (W5–W12) - Progress Tracker

**Started**: 2025-10-14  
**Status**: 🟢 In Progress (W5 Complete)

---

## 📊 Overall Progress

| Phase | Status | Tests | DoD | Tag |
|-------|--------|-------|-----|-----|
| **W5** | ✅ Complete | ✅ Pass | ✅ Met | `studio-workflow-W5` |
| **W6** | ✅ Complete | ✅ Pass | ✅ Met | `studio-workflow-W6` |
| **W7** | 🔵 Next | ⏳ Pending | ⏳ Pending | - |
| **W8** | ⏳ Queue | ⏳ Pending | ⏳ Pending | - |
| **W9** | ⏳ Queue | ⏳ Pending | ⏳ Pending | - |
| **W10** | ⏳ Queue | ⏳ Pending | ⏳ Pending | - |
| **W11** | ⏳ Queue | ⏳ Pending | ⏳ Pending | - |
| **W12** | ⏳ Queue | ⏳ Pending | ⏳ Pending | - |

---

## ✅ W5: Workflow Runtime (DB + API + Events) - COMPLETE

**Delivered**: 2025-10-14  
**Commits**: 4 (implementation, IT, E2E, docs)  
**Tag**: `studio-workflow-W5`

### Scope Delivered
- ✅ DB migration V3: `workflow_instances`, `workflow_events`, `workflow_timers` (+ indexes, retention views)
- ✅ API endpoints:
  - `GET /api/workflows/{entity}/{id}/graph` - Visual graph with current state
  - `GET /api/workflows/{entity}/{id}/state` - Allowed/blocked transitions + "why not"
  - `GET /api/workflows/{entity}/{id}/history` - Timeline with durations
  - `GET /api/workflows/{entity}/{id}/forecast` - Next steps + timers/SLA
- ✅ Kafka events: `ENTER_STATE`, `EXIT_STATE`, `ACTION_APPLIED`, `ERROR`
- ✅ Metrics (Micrometer): durations, error-rate, transition counts, SLA breaches

### Tests Delivered
- ✅ **Unit**: `WorkflowRuntimeServiceTest` (graph, state, history, forecast)
- ✅ **Integration**: 
  - `WorkflowApiIT` (Testcontainers PostgreSQL)
  - `WorkflowEventsKafkaIT` (Testcontainers Kafka, JSON validation)
- ✅ **E2E**: `pre/05_workflow_runtime_smoke.spec.ts` (timeline, forecast, tooltips, SLA)

### DoD Verification
- ✅ FE can load history + forecast from API
- ✅ Events/metrics visible in Prometheus/Grafana
- ✅ OpenAPI documented (W5_RUNTIME_GUIDE.md)
- ✅ 3 UX goals: **WHERE AM I / WHAT HAPPENED / WHAT'S NEXT**

### Key Files
```
backend/src/main/resources/db/migration/V3__workflow_runtime.sql
backend/src/main/java/cz/muriel/core/workflow/
  - WorkflowRuntimeService.java
  - WorkflowRuntimeController.java
  - WorkflowEventPublisher.java
  - WorkflowMetricsService.java
  - WorkflowModels.java (enhanced)
backend/src/test/java/cz/muriel/core/workflow/
  - WorkflowRuntimeServiceTest.java
  - WorkflowApiIT.java
  - WorkflowEventsKafkaIT.java
e2e/specs/pre/05_workflow_runtime_smoke.spec.ts
docs/workflow/W5_RUNTIME_GUIDE.md
```

---

## ✅ W6: Frontend UX (Graph, Timeline, Actions) - COMPLETE

**Delivered**: 2025-01-14  
**Commits**: 5 (components, unit tests, IT, E2E, docs)  
**Tag**: `studio-workflow-W6`

### Scope Delivered
- ✅ **WorkflowGraph.tsx**: React Flow visualization
  - Current state highlighting (blue border/background)
  - Allowed edges (green, animated) vs blocked edges (gray, static)
  - "Why not" tooltips for disabled transitions
  - Layout toggle: ELK (hierarchical) / Dagre (compact)
  - Legend for visual indicators
- ✅ **TimelinePanel.tsx**: MUI Timeline with history
  - Duration formatting (ms → human readable: 5m 30s, 2h 15m)
  - SLA badges (OK/WARN/BREACH) with icons
  - Actor tracking + relative timestamps (date-fns)
- ✅ **ActionsBar.tsx**: Context-aware action buttons
  - Allowed actions from current state
  - Read-only mode on workflow lock (Kafka signal)
  - Stale→Fresh refresh (30s timeout before apply)
  - Disabled actions with "why not" tooltips
- ✅ **Layout Hooks**:
  - `useElkLayout.ts` - ELK.js hierarchical layout
  - `useDagreLayout.ts` - Dagre compact layout

### Tests Delivered
- ✅ **Unit (Vitest + RTL)**: 24 tests total
  - `WorkflowGraph.test.tsx` (7 tests): nodes, edges, highlighting, tooltips, layout toggle
  - `TimelinePanel.test.tsx` (8 tests): durations, SLA badges, actors, empty state
  - `ActionsBar.test.tsx` (9 tests): buttons, lock detection, stale refresh, tooltips
- ✅ **Integration**: `PresenceLockIT.java` (4 tests)
  - Kafka lock signal → Actions disabled
  - Kafka unlock signal → Actions enabled
  - Multiple users → First-come-first-served
  - Lock expiration → Auto-unlock after 5 minutes
- ✅ **E2E**: `pre/06_workflow_ux.spec.ts` (8 tests)
  - Graph rendering + state highlighting
  - Layout toggle (elk ↔ dagre)
  - Edge styling (colors, animations)
  - Timeline (durations, SLA badges)
  - Actions (enabled/disabled, lock/unlock)
  - Stale data refresh

### DoD Verification
- ✅ Obrazovka plní 3 UX cíle: **WHERE AM I / WHAT HAPPENED / WHAT'S NEXT**
- ✅ Auto-layout funguje (elkjs i dagre přepínač)
- ✅ Tooltips zobrazují "why not" pro disabled edges
- ✅ Lock/unlock flow funguje přes Kafka (Testcontainers validation)

### Key Files
```
frontend/src/components/Workflow/
  - WorkflowGraph.tsx (212 lines)
  - TimelinePanel.tsx (135 lines)
  - ActionsBar.tsx (145 lines)
  - index.ts (updated exports)
frontend/src/hooks/
  - useElkLayout.ts
  - useDagreLayout.ts
  - index.ts
frontend/src/components/Workflow/__tests__/
  - WorkflowGraph.test.tsx
  - TimelinePanel.test.tsx
  - ActionsBar.test.tsx
backend/src/test/java/com/platform/workflow/
  - PresenceLockIT.java (Testcontainers PostgreSQL + Kafka)
e2e/pre/06_workflow_ux.spec.ts (Playwright)
W6_FRONTEND_UX.md (comprehensive documentation)
```

---

## 🔵 W7: Workflow Executors (automatické kroky) - NEXT

**Target**: TBD  
**Status**: 🔵 Starting

### Planned Scope
- [ ] Executor interface + registry (auto-kroky jak SendEmail, NotifySlack)
- [ ] Async execution s CompletableFuture
- [ ] Rollback/compensation logic při chybě
- [ ] Executor monitoring (Micrometer metrics)
- [ ] Error handling s retry policy

### Planned Tests
- [ ] **Unit (FE)**: 
  - `WorkflowGraph.test.tsx` (highlight/tooltipy)
  - `TimelinePanel.test.tsx` (durations/SLA)
  - `ActionsBar.test.tsx` (stale→fresh)
- [ ] **Integration**: `PresenceLockIT` (Kafka entityChanged/locked → FE read-only)
- [ ] **E2E**: `pre/06_workflow_ux.spec.ts` (mapa + layout, tooltipy, timeline, read-only flip)

### DoD Targets
- [ ] Obrazovka plní 3 UX cíle: kde jsem / co proběhlo / co bude
- [ ] Auto-layout funguje (elkjs i dagre)
- [ ] Tooltips zobrazují "why not" pro disabled edges
- [ ] Lock/unlock flow funguje přes Kafka

---

## ⏳ W7–W12: Queued

### W7: Workflow Executors (automatické kroky)
- Minimální sada: APPROVAL, SERVICE_REST_SYNC, KAFKA_COMMAND, EXTERNAL_TASK, TIMER/DELAY
- Retry/backoff/CircuitBreaker konfigurace
- Metriky per typ kroku

### W8: Konektory (Jira/Confluence/Trello + Generic REST)
- Generic REST z OpenAPI (codegen + templating)
- Konektory: Jira, Confluence, Trello
- Secrets v Safe, RBAC, audit

### W9: Rozšíření metamodelu (deklarativní kroky)
- Schéma workflow.steps[] (type, inputMap, onSuccess/onError, retry)
- Validátor + dry-run
- Studio editor (form + JSON/Monaco)

### W10: API kontrakty (OpenAPI/AsyncAPI) + CI lint/codegen
- Generuj OpenAPI/AsyncAPI z metamodelu → /contracts
- Spectral lint v CI
- Codegen klientů pro BE/FE

### W11: MCP integrace (ChatGPT tooling)
- metamodel-mcp tools: validate_spec, generate_ui_spec_preview, propose_change
- workflow-mcp tools: get_graph, get_state, simulate_transition
- Rate-limit, redakce citlivých dat

### W12: Observabilita + E2E scaffold
- Grafana panely: duration per state, step error-rate, CB stav, SLA breach
- Alerty: SLA překročení, DLQ růst
- E2E sanity: scaffold → execute → timeline → cleanup

---

## 📈 Metrics & CI Gates

### Current Status
- **BE Coverage**: ✅ Target 80/70 (W5 met)
- **FE Coverage**: ⏳ Target 80/70 (W6 pending)
- **Spectral Lint**: ⏳ W10
- **Contract Tests**: ⏳ W10

### Make Targets
```bash
make test:unit      # Unit tests only
make test:it        # Integration tests (Testcontainers)
make test:e2e       # E2E tests (Playwright)
make test:all       # All tests (local + CI)
```

---

## 🔐 Cross-Cutting (Across All Phases)

### RBAC
- [ ] Keycloak roles: `CORE_ADMIN_STUDIO`, `CORE_ADMIN_WORKFLOW`
- [ ] Export to `security/keycloak-realm-export.json`

### Security
- [ ] Rate-limit admin API
- [ ] Audit kdo/kdy/co
- [ ] PII redakce ve workflow_events

### Documentation
- [x] W5: `W5_RUNTIME_GUIDE.md`
- [ ] W6: `W6_FRONTEND_UX.md`
- [ ] W7–W12: TBD
- [ ] Final: `WORKFLOW_DESIGNER.md`, `ADMIN_STUDIO.md`, `MCP_SETUP.md`

---

## ✅ Acceptance Criteria (Final "Hotovo")

### User Experience
- [ ] Uživatel v detailu entity vidí stav, možné přechody (včetně důvodů, proč nejdou), timeline s duracemi a forecast
- [ ] Admin ve Studiu mění entitu/validace/relace přes Propose/Approve
- [ ] Po publish se UI-spec projeví bez redeploye

### Orchestration
- [ ] Orchestrátor provádí min. 3 typy kroků (REST/Kafka/Timer) s CB/Retry/Idempotence
- [ ] Publikuje eventy a metriky

### Quality
- [ ] OpenAPI/AsyncAPI kontrakty existují (lint OK)
- [ ] E2E sanity je zelená
- [ ] V Grafaně jsou panely i alerty

---

## 📝 Commit Style

Every commit follows:
```
W{n}: <popis> (Scope|Tests|DoD)

SCOPE: ...
IMPLEMENTATION: ...
TESTS: ...
DoD: ...
NEXT: ...
```

Every phase ends with annotated tag:
```bash
git tag -a studio-workflow-W{n} -m "W{n}: <summary> - COMPLETE"
```

---

**Last Updated**: 2025-10-14  
**Next Milestone**: W6 Frontend UX  
**Overall Status**: 🟢 On Track
