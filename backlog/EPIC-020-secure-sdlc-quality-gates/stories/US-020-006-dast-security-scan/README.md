---
id: US-020-006
epic: EPIC-020-secure-sdlc-quality-gates
title: "DAST a bezpecnostni sken"
priority: P0
status: in_progress
assignee: ""
created: 2026-01-15
updated: 2026-01-17
estimate: "4 days"
path_mapping:
  code_paths:
    - .github/workflows/quality-gates-nightly.yml
    - scripts/ci/dast-smoke.sh
  test_paths:
    - tests/e2e/presence.spec.ts
    - scripts/run-frontend-e2e.sh
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-006-dast-security-scan/README.md
---

# US-020-006: DAST a bezpecnostni sken

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** 🟡 **IN PROGRESS**
**Estimate:** 4 days

## User Story

**Jako:** Security engineer  
**Chci:** pravidelne spoustet DAST nad stagingem  
**Aby:** se odhalily webove zranitelnosti pred releasem.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- Nightly pipeline ma obsahovat DAST (OWASP ZAP).
- Vysledky musi byt auditovatelne.

## Definition of Ready (DoR)

- [ ] Existuje staging nebo lokalni cil pro ZAP.
- [ ] Znamy auth flow pro DAST (pokud je potreba).
- [ ] Thresholdy pro fail jsou definovane.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Definovany scope skenu.

## Acceptance Criteria

- [x] OWASP ZAP baseline scan bezi nightly.
- [x] High/Medium nove findingy failuji nightly pipeline.
- [x] ZAP report je ulozen jako artefakt a odkazovan v summary.
- [x] Scope skenu (URL, auth) je zdokumentovan.

## Scope Configuration

- Target URL: `DAST_TARGET_URL` (defaults to `https://admin.core-platform.local`)
- Enablement: `RUN_DAST=1` in nightly workflow

## Definition of Done (DoD)

**Kód:**
- [x] DAST job je integrovany do nightly pipeline.
- [x] ZAP konfigurace je verzovana.

**Testy:**
- [ ] Testovaci zranitelnost vyvola fail v nightly.

**Dokumentace:**
- [x] Dokumentovan scope a auth postup pro ZAP.

**Deployment:**
- [x] Nightly pipeline bezi v pravidelnem intervalu.

## Závislosti

- EPIC-020 (CI orchestration)
- EPIC-007 (Infra)

## Implementační tasky

- [TASK-020-006-01: ZAP baseline konfigurace](subtasks/TASK-020-006-01-zap-baseline-config.md)
- [TASK-020-006-02: Integrace ZAP do nightly CI](subtasks/TASK-020-006-02-nightly-ci-integration.md)
- [TASK-020-006-03: Auth a scope konfigurace](subtasks/TASK-020-006-03-auth-scope-config.md)
- [TASK-020-006-04: Reporting a triage procesu](subtasks/TASK-020-006-04-reporting-triage.md)
