---
id: US-020-001
epic: EPIC-020-secure-sdlc-quality-gates
title: "CI Quality Gates Orchestration"
priority: P0
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths:
    - .github/workflows/ci.yml
    - .github/workflows-disabled/security-scans.yml
  test_paths:
    - scripts/build/pre-build-test.sh
    - scripts/infra-smoke-test.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-001-ci-quality-gates-orchestration/README.md
---

# US-020-001: CI Quality Gates Orchestration

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** TODO
**Estimate:** 3 days

## User Story

**Jako:** Platform/DevOps engineer  
**Chci:** definovat jednotné quality gates pro PR, nightly a release pipeline  
**Aby:** se do main ani do releasu nedostal neověřený nebo nebezpečný změnový balík.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- PR pipeline musí být blocking pro všechny bezpečnostní a quality gate kontroly.
- Nightly pipeline běží full regresi (DAST, full E2E, skeny).
- Release pipeline musí splnit všechny kritické gate před tagem/releasem.

## Definition of Ready (DoR)

- [ ] Dokumentační zdroje jsou ověřeny
- [ ] Scope PR/nightly/release je odsouhlasen
- [ ] Nástroje pro gate kontroly jsou dostupné
- [ ] Závislosti na EPIC-000/002/003/007/012 jsou identifikovány
- [ ] Akceptační kritéria jsou měřitelná
- [ ] Odhad je potvrzen týmem

## Acceptance Criteria

- [ ] PR pipeline spouští minimální sadu gate kontrol a blokuje merge při failu.
- [ ] Nightly pipeline spouští full regresi (DAST + full E2E) a generuje report.
- [ ] Release pipeline je blocking a vyžaduje PASS všech kritických gate kontrol.
- [ ] Gate matice (PR/nightly/release) je zdokumentovaná a verzovaná v repozitáři.

## Definition of Done (DoD)

**Kód:**
- [ ] Workflow definice jsou v repozitáři a jsou spustitelné.
- [ ] Gating logika je centralizovaná (konfig/skript).

**Testy:**
- [ ] Pipeline validace běží pro PR i nightly a vrací PASS/FAIL.
- [ ] Failing gate skutečně blokuje merge.

**Dokumentace:**
- [ ] Runbook a popis triggerů jsou aktualizované.

**Deployment:**
- [ ] Gates jsou aktivní pro main a release branch.

## Závislosti

- EPIC-000 (Security baseline)
- EPIC-002 (E2E)
- EPIC-007 (Infra)
- EPIC-012 (Vault)

## Implementační tasky

- [TASK-020-001-01: Definice pipeline triggeru a scope](subtasks/TASK-020-001-01-pipeline-triggers-scope.md)
- [TASK-020-001-02: Workflow implementace a job struktura](subtasks/TASK-020-001-02-workflow-implementation.md)
- [TASK-020-001-03: Quality gate aggregator a pravidla failu](subtasks/TASK-020-001-03-quality-gate-aggregator.md)
- [TASK-020-001-04: Dokumentace a runbook pro quality gates](subtasks/TASK-020-001-04-docs-runbook.md)
