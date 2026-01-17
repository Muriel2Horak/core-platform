---
id: US-020-008
epic: EPIC-020-secure-sdlc-quality-gates
title: "Compliance evidence a reporting"
priority: P0
status: in_progress
assignee: ""
created: 2026-01-15
updated: 2026-01-17
estimate: "3 days"
path_mapping:
  code_paths:
    - scripts/ci/aggregate-gates.sh
    - scripts/ci/collect-evidence.sh
    - scripts/ci/write-gate-result.sh
    - docs/QUALITY_GATES_RUNBOOK.md
  test_paths:
    - tests/make_report.sh
    - tests/loki_query.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-008-compliance-evidence-reporting/README.md
---

# US-020-008: Compliance evidence a reporting

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** 🟡 **IN PROGRESS**
**Estimate:** 3 days

## User Story

**Jako:** Security/Compliance owner  
**Chci:** mit auditovatelnou evidenci quality gate vysledku  
**Aby:** bylo mozne prokazat splneni EPIC-000 baseline.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- Vysledky kontrol musi byt auditovatelne.
- Evidence ma byt centralne ulozena.

## Definition of Ready (DoR)

- [x] Je definovan format evidence (JSON).
- [x] Je zvolene uloziste (artifact/Loki).
- [x] CI ma pristup k ulozisti.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Je definovana retence dat.

## Acceptance Criteria

- [x] CI agreguje vysledky do jednoho JSON summary.
- [x] Summary je ulozen jako artefakt a/nebo poslan do Loki.
- [x] Evidence obsahuje mapovani na EPIC-000 baseline body.
- [ ] Dashboard/report ukazuje posledni PASS/FAIL.

## Definition of Done (DoD)

**Kód:**
- [x] Collector krok je integrovany do CI.
- [x] Export do uloziste je funkcni.

**Testy:**
- [ ] Testovaci pipeline vygeneruje validni summary.

**Dokumentace:**
- [x] Format evidence a retence jsou popsane.

**Deployment:**
- [x] Evidence se generuje pro PR i nightly.

## Závislosti

- EPIC-003 (Observability)
- EPIC-020 (CI orchestration)

## Implementační tasky

- [TASK-020-008-01: Schema evidence a mapovani na EPIC-000](subtasks/TASK-020-008-01-evidence-schema.md)
- [TASK-020-008-02: Collector pro CI summary](subtasks/TASK-020-008-02-collector-implementation.md)
- [TASK-020-008-03: Export evidence do Loki/artefaktu](subtasks/TASK-020-008-03-loki-artifact-export.md)
- [TASK-020-008-04: Dashboard a status reporting](subtasks/TASK-020-008-04-dashboard-status.md)
