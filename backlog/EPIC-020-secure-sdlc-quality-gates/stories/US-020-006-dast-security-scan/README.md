---
id: US-020-006
epic: EPIC-020-secure-sdlc-quality-gates
title: "DAST a bezpecnostni sken"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "4 days"
path_mapping:
  code_paths:
    - .github/workflows-disabled/security-scans.yml
    - .zap/rules.tsv
    - e2e/playwright.config.ts
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
**Status:** ✅ **DONE**
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

- [ ] OWASP ZAP baseline scan bezi nightly.
- [ ] High/Medium nove findingy failuji nightly pipeline.
- [ ] ZAP report je ulozen jako artefakt a odkazovan v summary.
- [ ] Scope skenu (URL, auth) je zdokumentovan.

## Definition of Done (DoD)

**Kód:**
- [ ] DAST job je integrovany do nightly pipeline.
- [ ] ZAP konfigurace je verzovana.

**Testy:**
- [ ] Testovaci zranitelnost vyvola fail v nightly.

**Dokumentace:**
- [ ] Dokumentovan scope a auth postup pro ZAP.

**Deployment:**
- [ ] Nightly pipeline bezi v pravidelnem intervalu.

## Závislosti

- EPIC-020 (CI orchestration)
- EPIC-007 (Infra)

## Implementační tasky

- [TASK-020-006-01: ZAP baseline konfigurace](subtasks/TASK-020-006-01-zap-baseline-config.md)
- [TASK-020-006-02: Integrace ZAP do nightly CI](subtasks/TASK-020-006-02-nightly-ci-integration.md)
- [TASK-020-006-03: Auth a scope konfigurace](subtasks/TASK-020-006-03-auth-scope-config.md)
- [TASK-020-006-04: Reporting a triage procesu](subtasks/TASK-020-006-04-reporting-triage.md)
