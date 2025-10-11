# Platform Hardening Epic - Tracking Issue

**Epic Branch**: `feature/platform-hardening-epic`  
**Created**: 2025-10-11  
**Status**: 🚧 In Progress  
**Target**: Production-Ready Platform (Naming, Presence, Kafka, Audit, Metamodel, Streaming, Docs, Tests)

---

## 🎯 Epic Goals

Deliver unified naming conventions, real-time presence with Kafka integration, event-driven Cube.js computations, comprehensive audit beyond reporting, automated Metamodel↔Cube sync, streaming infrastructure review & activation, complete documentation, and comprehensive tests & security controls.

---

## 📋 Phases & PRs

### S1: Naming - Rules + Auto-Fix Refactoring + CI Lints ⏳
**Status**: 🟡 In Progress  
**Estimate**: 8h  
**PR**: TBD  
**Branch**: `feature/s1-naming-guide`

**Scope**:
- [ ] Create `docs/NAMING_GUIDE.md` with all naming conventions
- [ ] Implement `tools/naming-lint/` (Node/TS) with validators
- [ ] Refactor existing codebase to comply
- [ ] Add migration aliases & deprecations
- [ ] CI integration (fail on violations)

**DoD**:
- ✅ Lints run in CI and fail on violations
- ✅ Repo consistent per NAMING_GUIDE.md
- ✅ Alias maps & deprecations documented in CHANGELOG

---

### S2: Online Presence + Kafka "Stale" ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 16h  
**PR**: TBD  
**Branch**: `feature/s2-presence-kafka`

**Scope**:
- [ ] WS endpoint `/ws/presence` (SUB/UNSUB/HB/FOCUS/BLUR/LOCK/UNLOCK)
- [ ] Redis backplane for locks & TTL
- [ ] Kafka consumer for `entity.lifecycle` (MUTATING/MUTATED)
- [ ] BE endpoint 423 Locked support
- [ ] FE hook `usePresence(entity, id)`
- [ ] E2E tests (2 browsers: lock, stale, auto-refresh)

**DoD**:
- ✅ Real-time presence working
- ✅ Read-only mode respected during stale
- ✅ Auto-refresh after write completion
- ✅ Tests green

---

### S3: Cube Computations via Kafka ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 12h  
**PR**: TBD  
**Branch**: `feature/s3-cube-kafka`

**Scope**:
- [ ] Topic `core.reporting.preagg.refresh`
- [ ] Producer (BE): bulk job → refresh event
- [ ] Worker (consumer): Cube pre-agg build API call
- [ ] BFF guardrails: cache HIT during BUILDING
- [ ] Metrics: `preagg_build_duration_seconds`, `preagg_build_failures_total`

**DoD**:
- ✅ Events drive pre-agg builds
- ✅ Peak load queued in Kafka
- ✅ Metrics & alerts exist

---

### S4: Supplementary Audit (Streaming/Backup/Grafana) ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 6h  
**PR**: TBD  
**Branch**: `feature/s4-audit-platform`

**Scope**:
- [ ] Create `AUDIT_REPORT_PLATFORM.md`
- [ ] Create `TODO_AUDIT_PLATFORM.md`
- [ ] Audit: Streaming/Kafka (infra, topics, DLQ, schemas, observability, security)
- [ ] Audit: Backup & Restore (RPO/RTO, scripts, restore drill)
- [ ] Audit: Grafana Scenes (BFF integration, cache/CB/limit) + OIDC (admin subpath, CSP)
- [ ] CI: Add "audit run" job

**DoD**:
- ✅ Reports exist
- ✅ TODO has P0/P1/P2 list with estimates
- ✅ CI audit job (optional)

---

### S5: Metamodel ↔ Cube Automation ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 10h  
**PR**: TBD  
**Branch**: `feature/s5-metamodel-cube-gen`

**Scope**:
- [ ] `tools/modelgen/` (Node/TS): metamodel JSON → Cube schema
- [ ] Generate: measures, dimensions, joins, RLS, pre-aggs
- [ ] Lint: mandatory timeDimension, max 2 pre-aggs/entity, no high-cardinality
- [ ] CI: `modelgen-validate`, `cube-smoke` (meta + 2 load queries)
- [ ] Dev watch task: auto-regenerate on metamodel change

**DoD**:
- ✅ Metamodel change → new Cube schema
- ✅ specVersion bump → BFF cache invalidation
- ✅ CI validates & smokes

---

### S6: Streaming Complete Review & Activation ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 14h  
**PR**: TBD  
**Branch**: `feature/s6-streaming-production`

**Scope**:
- [ ] Infra: Kafka (KRaft), Schema Registry, Kafka UI, JMX→Prometheus
- [ ] Security: SASL/ACLs, encryption, secrets externalized
- [ ] Contracts: Avro/Protobuf + compatibility policy
- [ ] Clients: idempotent producer, at-least-once consumer, DLQ strategy
- [ ] Observability: dashboard (lag, throughput, errors), alerts
- [ ] Docs/Runbook: incidents, replay, schema evolution

**DoD**:
- ✅ Streaming enabled & secure
- ✅ Metrics/alerts active
- ✅ Runbook complete

---

### S7: Unified Documentation ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 8h  
**PR**: TBD  
**Branch**: `feature/s7-docs-complete`

**Scope**:
- [ ] `docs/NAMING_GUIDE.md` (from S1)
- [ ] `docs/PRESENCE.md` (from S2)
- [ ] `docs/PREAGG_REFRESH.md` (from S3)
- [ ] `AUDIT_REPORT_PLATFORM.md` + `TODO_AUDIT_PLATFORM.md` (from S4)
- [ ] `docs/METAMODEL_TO_CUBE.md` (from S5)
- [ ] `docs/STREAMING_RUNBOOK.md` (from S6)
- [ ] `docs/HYBRID_MODE.md` (OLTP vs CUBE summary)
- [ ] `TESTING.md` (local + CI)

**DoD**:
- ✅ All docs exist & current
- ✅ Request/response examples
- ✅ Common errors documented

---

### S8: Tests & Security (Pre-Production Assurance) ⏸️
**Status**: ⚪ Not Started  
**Estimate**: 18h  
**PR**: TBD  
**Branch**: `feature/s8-tests-security`

**Scope**:

**Backend**:
- [ ] Test profile: no Docker, Caffeine, mock JWT, WireMock Cube
- [ ] IT: Reporting DSL (valid/invalid, cache, 429, 503, CB open)
- [ ] IT: OLTP (/search, PATCH If-Match 409, bulk-update dryRun/cancel)
- [ ] IT: Presence + Kafka MUTATING/MUTATED → STALE_ON/OFF, 423
- [ ] Security tests: header hardening, JSON size limits, Content-Type

**Frontend**:
- [ ] Playwright E2E: OLTP grid (inline edit, 409, bulk)
- [ ] E2E: CUBE grid/pivot/chart + drill
- [ ] E2E: Presence/stale (2 browsers)
- [ ] ESLint/TS typecheck, import cycles, duplicate symbols

**CI Security**:
- [ ] SAST (SonarQube/CodeQL)
- [ ] DAST (ZAP baseline)
- [ ] Dependency scan (OWASP DC/Trivy)
- [ ] Secrets (GitLeaks/TruffleHog)
- [ ] SBOM (CycloneDX)

**DoD**:
- ✅ All green
- ✅ No High/Critical findings
- ✅ New code coverage ≥80% lines (BE), baseline FE

---

## 🚦 Merge Gates (Every PR)

- ✅ Build + Unit + IT + E2E green
- ✅ SAST/DAST/Dependency/Secrets: no High/Critical
- ✅ Lint/format OK; naming-lint passed
- ✅ OpenAPI/Swagger updated
- ✅ Documentation added; CHANGELOG entry

---

## 📊 Progress Tracking

| Phase | Status | Estimate | Actual | PR | Notes |
|-------|--------|----------|--------|-----|-------|
| S1 | 🟡 In Progress | 8h | - | TBD | Naming guide + lints |
| S2 | ⚪ Not Started | 16h | - | TBD | Presence + Kafka stale |
| S3 | ⚪ Not Started | 12h | - | TBD | Cube via Kafka |
| S4 | ⚪ Not Started | 6h | - | TBD | Platform audit |
| S5 | ⚪ Not Started | 10h | - | TBD | Metamodel→Cube gen |
| S6 | ⚪ Not Started | 14h | - | TBD | Streaming review |
| S7 | ⚪ Not Started | 8h | - | TBD | Complete docs |
| S8 | ⚪ Not Started | 18h | - | TBD | Tests & security |
| **TOTAL** | **0/8** | **92h** | **0h** | - | - |

---

## ✅ Epic Definition of Done

- ✅ Naming unified & enforced in CI
- ✅ Presence + Kafka "stale" deployed
- ✅ Pre-aggs controlled via Kafka
- ✅ Supplementary audit complete
- ✅ Metamodel↔Cube auto-gen working
- ✅ Streaming reviewed & enabled
- ✅ Unified documentation complete
- ✅ Tests & security scans green

---

**Last Updated**: 2025-10-11  
**Next Review**: Weekly (Mondays)
