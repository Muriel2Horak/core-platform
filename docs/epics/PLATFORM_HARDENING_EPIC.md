# Platform Hardening Epic - Tracking

**Epic Branch:** `feature/platform-hardening-epic`  
**Status:** 🚧 In Progress  
**Started:** 11. října 2025  
**Target Completion:** Q4 2025

---

## 🎯 Cíle Epic

Doručit jednotnou platformu s:
- ✅ Jednotné pojmenování (naming conventions)
- ✅ Realtime presence s Kafka "stale" režimem
- ✅ Event-driven Cube pre-aggregations
- ✅ Doplňkový audit mimo reporting
- ✅ Automatizace Metamodel ↔ Cube
- ✅ Revize a zapnutí streamingu
- ✅ Ucelená dokumentace
- ✅ Kompletní testy & security kontroly

---

## 📋 Fáze (S1-S8)

### S1: Naming - Pravidla + Refaktoring + Linty v CI

**Status:** ✅ Complete  
**PR:** [#TBD - S1: Naming Standards & Linting](link)  
**Estimate:** 8h  
**Actual:** 4h  
**Assignee:** @muriel

**Deliverables:**
- [x] `docs/NAMING_GUIDE.md` - kompletní pravidla (existuje ✅)
- [x] `tools/naming-lint/` - auto-validační nástroje (existuje ✅)
- [x] `.github/workflows/naming-lint.yml` - CI integrace (existuje ✅)
- [x] Refaktor REST API paths na kebab-case plurál
- [x] Swagger/OpenAPI anotace (@Tag, @Operation, @Parameter, @ApiResponses)
- [x] JSON DTOs verified (camelCase) ✅
- [x] Alias mapy pro zpětnou kompatibilitu
- [x] CHANGELOG entry

**DoD:**
- [x] Linty v CI běží a failují při porušení pravidel
- [x] Repo konzistentní dle NAMING_GUIDE.md
- [x] Všechny controllery mají kebab-case plurál paths
- [x] DTOs používají camelCase
- [x] Alias/deprecation mapy dokumentované
- [x] Build úspěšný (clean compile jar:jar)
- [x] Swagger dokumentace aktuální

**Issues Resolved:**
1. ✅ `/api/users-directory` → `/api/user-directories` (plurál)
2. ✅ Swagger anotace přidány pro lepší API dokumentaci
3. ✅ Všechny controller warnings vyřešeny nebo zdokumentovány jako OK

---

### S2: Online Viditelnost + Kafka "Stale"

**Status:** 📅 Planned  
**PR:** [#TBD - S2: Presence & Kafka Stale Mode](link)  
**Estimate:** 16h  
**Dependencies:** S1 ✅

**Deliverables:**
- [ ] WS endpoint `/ws/presence` (JSON protokol)
- [ ] Redis backplane pro presence tracking
- [ ] Kafka consumer `entity.lifecycle` (MUTATING/MUTATED)
- [ ] Backend stale detection + 423 Locked responses
- [ ] Frontend `usePresence` hook
- [ ] Explorer/Detail UI badges + read-only mode
- [ ] IT testy: lock TTL, STALE events, 423
- [ ] E2E: 2 prohlížeče (edit vs read-only)
- [ ] `docs/PRESENCE.md`
- [ ] CHANGELOG entry

**DoD:**
- [ ] Realtime presence funguje mezi klienty
- [ ] Read-only režim při cizím locku/stale
- [ ] Auto-refresh po STALE_OFF
- [ ] Testy zelené (IT + E2E)

---

### S3: Cube Výpočty přes Kafka

**Status:** 📅 Planned  
**PR:** [#TBD - S3: Event-Driven Pre-Aggregations](link)  
**Estimate:** 12h  
**Dependencies:** S2 ✅

**Deliverables:**
- [ ] Topic `core.reporting.preagg.refresh`
- [ ] Producer (BE): bulk job triggers
- [ ] Worker (consumer): Cube pre-agg build
- [ ] BFF guardrails: cache HIT + X-Data-Staleness
- [ ] Metriky: `preagg_build_duration_seconds`, `_failures_total`
- [ ] Alerty (Prometheus rules)
- [ ] `docs/PREAGG_REFRESH.md`
- [ ] CHANGELOG entry

**DoD:**
- [ ] Pre-aggs řízeny přes Kafka events
- [ ] Špičky frontované
- [ ] Metriky + alerty aktivní

---

### S4: Doplňkový Audit (Streaming, Backup, Grafana)

**Status:** 📅 Planned  
**PR:** [#TBD - S4: Platform Audit Report](link)  
**Estimate:** 6h  
**Dependencies:** S3 ✅

**Deliverables:**
- [ ] `AUDIT_REPORT_PLATFORM.md`
- [ ] `TODO_AUDIT_PLATFORM.md` (P0/P1/P2)
- [ ] Audit sections:
  - [ ] Streaming/Kafka (infra, DLQ, schémata, security)
  - [ ] Backup & Restore (RPO/RTO, skripty, drill)
  - [ ] Grafana Scenes (BFF, cache, CB)
  - [ ] Grafana↔Keycloak OIDC (SSO, CSP)
- [ ] CI job: audit-run (volitelně)
- [ ] CHANGELOG entry

**DoD:**
- [ ] Reporty existují s STATUS/EVIDENCE
- [ ] TODO má P0/P1/P2 + odhady (h)

---

### S5: Automatizace Metamodel ↔ Cube

**Status:** 📅 Planned  
**PR:** [#TBD - S5: Metamodel to Cube Generator](link)  
**Estimate:** 10h  
**Dependencies:** S4 ✅

**Deliverables:**
- [ ] `tools/modelgen/` (Node/TS)
- [ ] Generátor: `metamodel/*.json` → `cube/schema/*.js`
- [ ] Lint: timeDimension, preAggPolicy, cardinality
- [ ] CI: modelgen-validate, cube-smoke
- [ ] Watch task: auto-regen + Cube reload
- [ ] `docs/METAMODEL_TO_CUBE.md`
- [ ] CHANGELOG entry

**DoD:**
- [ ] Změna metamodelu → auto Cube schema
- [ ] specVersion bump → BFF cache invalidace
- [ ] Linty failují při porušení pravidel

---

### S6: Revize Streamingu + Zapnutí

**Status:** 📅 Planned  
**PR:** [#TBD - S6: Streaming Infrastructure](link)  
**Estimate:** 20h  
**Dependencies:** S5 ✅

**Deliverables:**
- [ ] Infra: Kafka (KRaft), Schema Registry, UI, metrics
- [ ] Security: SASL/ACLs, šifrování, secrets vault
- [ ] Kontrakty: Avro/Protobuf + compatibility
- [ ] Clients: idempotent producer, at-least-once consumer
- [ ] DLQ + poison pill strategie
- [ ] Observabilita: dashboard (lag, throughput, errors)
- [ ] Alerty
- [ ] `docs/STREAMING_RUNBOOK.md` (incident handling)
- [ ] CHANGELOG entry

**DoD:**
- [ ] Streaming zapnutý a bezpečný
- [ ] Metriky/alerty aktivní
- [ ] Runbook hotový s playbooks

---

### S7: Dokumentace (Ucelená)

**Status:** 📅 Planned  
**PR:** [#TBD - S7: Comprehensive Documentation](link)  
**Estimate:** 8h  
**Dependencies:** S6 ✅

**Deliverables:**
- [ ] `docs/NAMING_GUIDE.md` (S1) ✅
- [ ] `docs/PRESENCE.md` (S2)
- [ ] `docs/PREAGG_REFRESH.md` (S3)
- [ ] `AUDIT_REPORT_PLATFORM.md` + `TODO_AUDIT_PLATFORM.md` (S4)
- [ ] `docs/METAMODEL_TO_CUBE.md` (S5)
- [ ] `docs/STREAMING_RUNBOOK.md` (S6)
- [ ] `docs/HYBRID_MODE.md` (OLTP vs CUBE)
- [ ] `TESTING.md` (local + CI)
- [ ] Příklady request/response
- [ ] Běžné chyby + řešení
- [ ] CHANGELOG entry

**DoD:**
- [ ] Všechny dokumenty aktuální
- [ ] Příklady funkční
- [ ] Běžné chyby dokumentované

---

### S8: Testy & Security (Předprodukční jistota)

**Status:** 📅 Planned  
**PR:** [#TBD - S8: Comprehensive Testing & Security](link)  
**Estimate:** 24h  
**Dependencies:** S7 ✅

**Deliverables:**

**Backend:**
- [ ] Test profil (bez Docker): Caffeine, mock JWT, WireMock
- [ ] IT testy:
  - [ ] Reporting DSL: valid/invalid, cache, 429, CB
  - [ ] OLTP: /search, PATCH 409, bulk-update
  - [ ] Presence + Kafka MUTATING/MUTATED
  - [ ] Security: header hardening, JSON size, Content-Type
- [ ] Coverage ≥80% nového kódu

**Frontend:**
- [ ] Playwright E2E:
  - [ ] OLTP grid: inline edit, 409, bulk
  - [ ] CUBE: grid/pivot/chart + drill
  - [ ] Presence: 2 prohlížeče, stale
- [ ] ESLint/TS typecheck
- [ ] Import cycles check

**CI Security:**
- [ ] SAST (Sonar/CodeQL)
- [ ] DAST (ZAP baseline)
- [ ] Dependency (OWASP DC/Trivy)
- [ ] Secrets (GitLeaks/TruffleHog)
- [ ] SBOM (CycloneDX)

**DoD:**
- [ ] Vše zelené
- [ ] Žádné High/Critical findings
- [ ] Coverage goals met

---

## 🔒 Merge Gates (Každý PR)

- [ ] ✅ Build + Unit + IT + E2E zelené
- [ ] ✅ SAST/DAST/Dependency/Secrets „bez High/Critical"
- [ ] ✅ Lint/format OK; naming-lint prošel
- [ ] ✅ OpenAPI/Swagger aktualizovaný
- [ ] ✅ Dokumentace doplněná; CHANGELOG položka

---

## 📊 Progress Overview

| Fáze | Status | PR | Estimate | Actual | Efficiency |
|------|--------|-----|----------|--------|------------|
| S1 | ✅ Complete | #TBD | 8h | 4h | 50% |
| S2 | 📅 Planned | #TBD | 16h | - | - |
| S3 | 📅 Planned | #TBD | 12h | - | - |
| S4 | 📅 Planned | #TBD | 6h | - | - |
| S5 | 📅 Planned | #TBD | 10h | - | - |
| S6 | 📅 Planned | #TBD | 20h | - | - |
| S7 | 📅 Planned | #TBD | 8h | - | - |
| S8 | 📅 Planned | #TBD | 24h | - | - |
| **TOTAL** | | | **104h** | **4h** | **12.5%** |

---

## 📝 Notes

- OSS stack: AG Grid Community, FINOS Perspective, ECharts, Resilience4j, Bucket4j, WireMock, Playwright
- Všechny PR musí projít merge gates
- Dokumentace a testy jsou povinné pro každý PR
- CHANGELOG musí být aktualizován v každém PR

---

**Last Updated:** 11. října 2025  
**Maintainer:** Platform Team
