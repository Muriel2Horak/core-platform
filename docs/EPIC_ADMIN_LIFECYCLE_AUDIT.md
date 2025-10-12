# 🔍 EPIC: Admin & Lifecycle - Audit & Gap Report

**Datum:** 12. října 2025  
**Branch base:** `feature/admin-epic`  
**Autor:** GitHub Copilot  
**Status:** S10-0 – Audit pouze, bez změn kódu

---

## 📋 Executive Summary

Tento dokument provádí revizi existující implementace a identifikuje chybějící prvky pro EPIC Admin & Lifecycle. Zahrnuje:
- **Metamodel Studio** (GUI + BE API, diff/preview/approval, modelgen hook)
- **Admin Config GUI** (env/tenant parametry, feature flags, secrets maskování, audit)
- **Archivace/Obnova** (backup/restore jobs, S3/MinIO, checksum, retention, DR runbook)
- **Workflow** (definice, editor, per-entity lifecycle, akce v UI, Policy/Rules engine, persistence, eventy)
- **Admin Console** (joby/modelgen/pre-agg/backup, health, streaming lag/DLQ, CB)
- **RBAC** (Keycloak roles pro admin oblasti)
- **Testy** (unit/IT/E2E), CI gates, dokumentace

---

## 🎯 Audit Matrix

| Oblast | Požadavek | Stav | Soubor(y) | Chybí | Návrh PR | Odhad (h) |
|--------|-----------|------|-----------|-------|----------|-----------|
| **METAMODEL STUDIO** |
| Studio | GUI pro správu metamodelu | ❌ | - | Celá FE komponenta | S10 | 8h |
| Studio | BE API - GET/POST /api/admin/studio/entities | ⚠️ | `MetamodelAdminController.java` | Pouze /metamodel/reload, chybí /studio endpoints | S10 | 4h |
| Studio | Entity editor (form + JSON Monaco) | ❌ | - | Celý editor | S10 | 6h |
| Studio | Validace (JSON schema + naming-lint) | ⚠️ | `tools/naming-lint/` | Integrace do GUI | S10 | 2h |
| Studio | Diff view vůči current | ✅ | `MetamodelAdminController.java:48-71` | - | - | - |
| Studio | Preview modelgen (dry-run) | ❌ | - | Dry-run režim | S10 | 4h |
| Studio | POST /api/admin/studio/validate | ❌ | - | Endpoint | S10 | 2h |
| Studio | POST /api/admin/studio/preview | ❌ | - | Endpoint + dry-run | S10 | 3h |
| Studio | POST /api/admin/studio/proposals | ❌ | - | CR workflow | S10 | 5h |
| Studio | DB: metamodel_versions | ❌ | - | Migrace | S10 | 1h |
| Studio | DB: metamodel_change_requests | ❌ | - | Migrace | S10 | 2h |
| Studio | RBAC: CORE_ADMIN_STUDIO | ❌ | - | Keycloak role | S10 | 0.5h |
| Studio | CI: modelgen-validate job | ❌ | - | GH Actions | S10 | 1h |
| Studio | E2E: edit→validate→preview→approve | ❌ | - | Test | S10 | 3h |
| Studio | Docs: ADMIN_STUDIO.md | ❌ | - | Dokumentace | S10 | 2h |
| **ADMIN CONFIG GUI** |
| Config | GUI pro env/tenant parametry | ❌ | - | Celá FE komponenta | S11 | 6h |
| Config | GET/PUT /api/admin/config/{scope} | ❌ | - | Endpoints | S11 | 3h |
| Config | Maskování secrets v API | ❌ | - | Interceptor/Filter | S11 | 2h |
| Config | Audit do config_audit table | ❌ | - | DB migrace + service | S11 | 3h |
| Config | Feature flags toggles | ❌ | - | GUI + BE | S11 | 4h |
| Config | Export/import JSON | ❌ | - | Endpoints | S11 | 2h |
| Config | RBAC: CORE_ADMIN_CONFIG | ❌ | - | Keycloak role | S11 | 0.5h |
| Config | E2E: edit, audit, export/import | ❌ | - | Test | S11 | 2h |
| Config | Docs: ENV_CONFIG.md | ❌ | - | Dokumentace | S11 | 2h |
| **ARCHIVACE & OBNOVA** |
| Backup | DB: backup_jobs, restore_jobs | ❌ | - | Migrace | S12 | 1h |
| Backup | POST /api/admin/backup (on-demand) | ❌ | - | Endpoint | S12 | 3h |
| Backup | GET /api/admin/backup/jobs | ❌ | - | Endpoint | S12 | 1h |
| Backup | POST /api/admin/restore (wizard) | ❌ | - | Endpoint | S12 | 4h |
| Backup | GET /api/admin/restore/jobs | ❌ | - | Endpoint | S12 | 1h |
| Backup | MinIO/S3 integrace (SSE-S3) | ⚠️ | `MinIOProperties.java`, `DocumentService.java` | Jen document upload, chybí backup logic | S12 | 5h |
| Backup | Checksum + retention | ❌ | - | Logic | S12 | 2h |
| Backup | FE: /admin/backup GUI | ❌ | - | Komponenta | S12 | 6h |
| Backup | RBAC: CORE_ADMIN_BACKUP | ❌ | - | Keycloak role | S12 | 0.5h |
| Backup | DR runbook script | ❌ | - | Script + docs | S12 | 3h |
| Backup | E2E: backup→dry-run restore→sandbox restore | ❌ | - | Test | S12 | 3h |
| Backup | Docs: BACKUP_RESTORE.md | ❌ | - | Dokumentace | S12 | 2h |
| **WORKFLOW** |
| Workflow | YAML/JSON definice per entita | ⚠️ | `backend/src/main/resources/metamodel/*.yaml` | Partial (states/transitions v User.yaml) | S13 | 3h |
| Workflow | DB: workflow_instances | ❌ | - | Migrace | S13 | 1h |
| Workflow | WorkflowService (applyAction, timers) | ⚠️ | `WorkflowService.java` | Timers + escalace chybí | S13 | 4h |
| Workflow | PolicyEngine guards (CEL/DSL) | ⚠️ | `PolicyEngine.java`, `WorkflowService.java:158-179` | Jen simple hasRole, chybí CEL | S13 | 5h |
| Workflow | Kafka: core.workflow.events | ❌ | - | Topic + producer | S13 | 2h |
| Workflow | FE: EntityView SDK - Actions panel | ❌ | - | SDK extension | S13 | 4h |
| Workflow | FE: Presence lock + STALE_ON disable | ⚠️ | `usePresence.ts` | Lock funguje, chybí workflow integrace | S13 | 2h |
| Workflow | FE: /admin/workflows editor | ❌ | - | Celá komponenta | S13 | 8h |
| Workflow | Editor GUI (Monaco + form) | ❌ | - | Editor | S13 | 5h |
| Workflow | Validace + preview (dot-run) | ❌ | - | Validátor | S13 | 3h |
| Workflow | Diff + publish | ❌ | - | Versioning | S13 | 2h |
| Workflow | RBAC: CORE_ADMIN_WORKFLOW | ❌ | - | Keycloak role | S13 | 0.5h |
| Workflow | Unit: akce mění stav, guards | ⚠️ | - | Partial (workflow ručně testováno) | S13 | 3h |
| Workflow | IT: timers se spouští | ❌ | - | Test | S13 | 2h |
| Workflow | E2E: akce→stav change, lock→disable | ❌ | - | Test | S13 | 3h |
| Workflow | Docs: WORKFLOW.md | ❌ | - | Dokumentace | S13 | 3h |
| **ADMIN CONSOLE** |
| Console | FE: /admin/console taby | ❌ | - | Komponenta | S14 | 6h |
| Console | Tab: Jobs (modelgen/pre-agg/backup) | ❌ | - | Tab | S14 | 3h |
| Console | Tab: Health | ❌ | - | Tab | S14 | 2h |
| Console | Tab: Streaming (lag/DLQ) | ⚠️ | `StreamingDashboardPage.tsx` | Existuje na /core-admin/streaming | S14 | 1h |
| Console | Tab: Grafana | ❌ | - | Tab | S14 | 1h |
| Console | Tab: Circuit Breakers | ❌ | - | Tab | S14 | 2h |
| Console | GET /api/admin/jobs | ❌ | - | Endpoint | S14 | 3h |
| Console | GET /api/admin/health | ⚠️ | `/actuator/health` | Actuator existuje, chybí custom agregace | S14 | 2h |
| Console | RBAC: CORE_ADMIN_CONSOLE | ❌ | - | Keycloak role | S14 | 0.5h |
| Console | Dashboardy zobrazují stavy | ❌ | - | Implementace | S14 | 4h |
| Console | Odkazy na logy | ❌ | - | Loki integrace | S14 | 2h |
| Console | Akce: retry/cancel | ❌ | - | API + UI | S14 | 3h |
| Console | Docs: ADMIN_CONSOLE.md | ❌ | - | Dokumentace | S14 | 2h |
| **RBAC** |
| RBAC | Keycloak realm: CORE_ADMIN_STUDIO | ❌ | - | Role definition | S10-S14 | 0.5h |
| RBAC | Keycloak realm: CORE_ADMIN_CONFIG | ❌ | - | Role definition | S10-S14 | 0.5h |
| RBAC | Keycloak realm: CORE_ADMIN_BACKUP | ❌ | - | Role definition | S10-S14 | 0.5h |
| RBAC | Keycloak realm: CORE_ADMIN_WORKFLOW | ❌ | - | Role definition | S10-S14 | 0.5h |
| RBAC | Keycloak realm: CORE_ADMIN_CONSOLE | ❌ | - | Role definition | S10-S14 | 0.5h |
| RBAC | Realm export aktualizace | ❌ | `docker/keycloak/realm-admin.template.json` | Nové role chybí | S10-S14 | 1h |
| RBAC | Backend: @PreAuthorize annotations | ⚠️ | Partial - CORE_ROLE_ADMIN v 48 místech | Nové admin role chybí | S10-S14 | 2h |
| RBAC | Frontend: role-based menu | ⚠️ | `SidebarNav.tsx` | Partial | S10-S14 | 1h |
| **TESTY & CI** |
| Tests | Unit: Studio validate/preview | ❌ | - | Test | S15 | 2h |
| Tests | Unit: Config masks secrets | ❌ | - | Test | S15 | 1h |
| Tests | Unit: Backup checksum | ❌ | - | Test | S15 | 1h |
| Tests | Unit: Workflow guards | ❌ | - | Test | S15 | 2h |
| Tests | IT: Studio modelgen dry-run | ❌ | - | Test | S15 | 3h |
| Tests | IT: Backup→restore roundtrip | ❌ | - | Test | S15 | 4h |
| Tests | IT: Workflow timer trigger | ❌ | - | Test | S15 | 2h |
| Tests | E2E: Studio edit→approve | ❌ | - | Test | S15 | 3h |
| Tests | E2E: Config edit→audit | ❌ | - | Test | S15 | 2h |
| Tests | E2E: Backup→restore | ❌ | - | Test | S15 | 3h |
| Tests | E2E: Workflow action→lock | ❌ | - | Test | S15 | 2h |
| CI | grep TODO killer | ❌ | - | GH Action step | S15 | 0.5h |
| CI | FE/BE lint gates | ⚠️ | `.github/workflows/streaming-tests.yml` | Částečně | S15 | 1h |
| CI | E2E mandatory | ❌ | - | GH Action | S15 | 2h |
| Security | CodeQL/OWASP/ZAP/Trivy | ⚠️ | `.github/workflows/streaming-tests.yml:25-37` | OWASP je, chybí ZAP/Trivy | S15 | 3h |
| Security | Secrets scan | ❌ | - | GH Action | S15 | 1h |
| Coverage | ≥70% nový kód | ⚠️ | `.github/workflows/streaming-tests.yml:62-67` | JaCoCo je, chybí per-PR check | S15 | 1h |
| **DOKUMENTACE** |
| Docs | ADMIN_STUDIO.md | ❌ | - | Dokument | S10 | 2h |
| Docs | ENV_CONFIG.md | ❌ | - | Dokument | S11 | 2h |
| Docs | BACKUP_RESTORE.md | ❌ | - | Dokument | S12 | 2h |
| Docs | WORKFLOW.md | ❌ | - | Dokument | S13 | 3h |
| Docs | ADMIN_CONSOLE.md | ❌ | - | Dokument | S14 | 2h |
| Docs | Observability (Micrometer/Grafana) | ⚠️ | `STREAMING_README.md`, `REPORTING_OPERATIONS_RUNBOOK.md` | Partial - streaming má, admin oblasti chybí | S15 | 2h |
| Docs | Audit trail spec | ❌ | - | Dokument | S11 | 1h |
| Docs | Release notes | ❌ | - | Dokument | S15 | 1h |

---

## 📊 Gap Summary

### Existující implementace (můžeme využít):

1. **Metamodel Hot Reload API** ✅
   - `MetamodelAdminController.java` s 3 endpoints:
     - GET `/api/admin/metamodel/reload` – diff detection
     - POST `/api/admin/metamodel/apply-safe-changes`
     - GET `/api/admin/metamodel/status`
   - Docs: `METAMODEL_PHASE_2_3_COMPLETE.md`

2. **Workflow State Management** ⚠️ (partial)
   - DB tabulky: `entity_state`, `state_transition`, `entity_state_log` (V1__init.sql:323-396)
   - `WorkflowService.java` + `WorkflowController.java`
   - Endpoints: GET `/api/entities/{entityType}/{entityId}/state`, `/transitions`
   - **Chybí:** Timers, CEL guards, Kafka events, FE editor

3. **Streaming Dashboard** ✅
   - `StreamingDashboardPage.tsx` – real-time metriky, Grafana embeds
   - `StreamingAdminController.java` – DLQ management
   - 3 Grafana dashboardy: overview, entities, ops
   - **Note:** Může být refaktorován do Admin Console jako tab

4. **MinIO Storage** ⚠️ (partial)
   - `MinIOProperties.java`, `DocumentService.java`
   - Upload/download funguje pro dokumenty
   - **Chybí:** Backup/restore logic

5. **Naming Lint** ✅
   - `tools/naming-lint/` – metamodel, API, Kafka, DB
   - CI: `.github/workflows/naming-lint.yml`
   - **Potřeba:** Integrace do Studio GUI

6. **RBAC** ⚠️ (partial)
   - Existující role: `CORE_ROLE_ADMIN`, `CORE_ROLE_USER_MANAGER`, `CORE_ROLE_TENANT_ADMIN`
   - 48+ `@PreAuthorize` anotací v BE
   - **Chybí:** 5 nových admin rolí (STUDIO, CONFIG, BACKUP, WORKFLOW, CONSOLE)

7. **Presence System** ✅
   - `usePresence.ts`, WebSocket backend
   - Lock indicator, stale mode
   - **Potřeba:** Integrace do workflow akcí

8. **Testing Infrastructure** ⚠️ (partial)
   - JaCoCo coverage: `.github/workflows/streaming-tests.yml:62-67`
   - IT tests: Testcontainers
   - **Chybí:** E2E pro admin oblasti, TODO killer

---

### Chybějící komponenty (musí být implementovány):

#### S10 – Metamodel Studio
- **FE:** `/admin/studio` komponenta (editor, diff view, validation, preview)
- **BE:** 4 nové endpoints (validate, preview, proposals, approve)
- **DB:** 2 migrace (metamodel_versions, metamodel_change_requests)
- **CI:** modelgen-validate job
- **Docs:** ADMIN_STUDIO.md

#### S11 – Admin Config GUI
- **FE:** `/admin/config` komponenta (env×tenant tabulka, secrets masking, export/import)
- **BE:** GET/PUT `/api/admin/config/{scope}`, maskování interceptor
- **DB:** config_audit table
- **Docs:** ENV_CONFIG.md

#### S12 – Archivace & Obnova
- **FE:** `/admin/backup` komponenta (wizard, job list)
- **BE:** 4 endpoints (backup, restore, jobs lists)
- **DB:** backup_jobs, restore_jobs
- **Logic:** S3 upload, checksum, retention
- **Script:** DR runbook
- **Docs:** BACKUP_RESTORE.md

#### S13 – Workflow
- **FE:** `/admin/workflows` editor (Monaco, validation, preview)
- **FE:** EntityView SDK rozšíření (Actions panel)
- **BE:** Timer scheduler, CEL guards implementace
- **Kafka:** core.workflow.events topic
- **DB:** workflow_instances
- **Docs:** WORKFLOW.md

#### S14 – Admin Console
- **FE:** `/admin/console` dashboard (5 tabů)
- **BE:** GET `/api/admin/jobs`, agregace health dat
- **Integrace:** Streaming tab refactor
- **Docs:** ADMIN_CONSOLE.md

#### S15 – Hardening
- **Tests:** 11 E2E scenarios
- **CI:** TODO killer, E2E mandatory, ZAP/Trivy
- **Security:** Secrets scan
- **Docs:** Release notes

---

## 🎯 Odhady času (celkem)

| PR | Oblast | Hodiny |
|----|--------|--------|
| S10 | Metamodel Studio | 43.5h |
| S11 | Admin Config GUI | 24.5h |
| S12 | Archivace & Obnova | 31.5h |
| S13 | Workflow | 51.5h |
| S14 | Admin Console | 27.5h |
| S15 | Hardening & Tests | 33.5h |
| **TOTAL** | | **211.5h** |

**Poznámka:** Odhady zahrnují implementaci, testování a dokumentaci. Při využití existující infrastruktury (metamodel reload, workflow partial, MinIO) lze ušetřit ~20h.

---

## 🔗 Závislosti mezi PR

```
S10 (Studio) ──┐
               ├─→ S13 (Workflow - editor pattern)
S11 (Config) ──┘

S12 (Backup) ──→ S14 (Console - backup tab)

S13 (Workflow) ──→ S15 (E2E workflow tests)

S14 (Console) ──→ S15 (E2E console tests)

S10,S11,S12,S13,S14 ──→ S15 (Hardening všech)
```

**Doporučené pořadí:**
1. **S10** (Studio) – základ admin patterns
2. **S11** (Config) + **S12** (Backup) – paralelně
3. **S13** (Workflow) – po S10 (použije editor pattern)
4. **S14** (Console) – po S12,S13 (agreguje)
5. **S15** (Hardening) – na konec

---

## ✅ DoD pro S10-0

- [x] Dokument `EPIC_ADMIN_LIFECYCLE_AUDIT.md` vytvořen
- [x] Dokument `EPIC_ADMIN_LIFECYCLE_PLAN.md` vytvořen
- [ ] PR merged po review

---

## 📚 Reference

**Existující dokumentace:**
- `METAMODEL_PHASE_2_3_COMPLETE.md` – Hot reload API
- `STREAMING_README.md` – Monitoring patterns
- `REPORTING_OPERATIONS_RUNBOOK.md` – Operations best practices
- `WORKFLOW.md` (neexistuje, ale `WorkflowService.java` je)
- `S7_COMPLETE.md` – Kafka retry patterns
- `NAMING_GUIDE.md` – Naming conventions

**Keycloak:**
- Realm template: `docker/keycloak/realm-admin.template.json`
- Existující role: grep `CORE_ROLE_` v BE

**CI/CD:**
- `.github/workflows/streaming-tests.yml` – vzor pro gates
- `.github/workflows/naming-lint.yml` – lint gates

**Frontend patterns:**
- `StreamingDashboardPage.tsx` – dashboard template
- `usePresence.ts` – presence/lock pattern
- `TenantManagement.jsx` – admin CRUD template
