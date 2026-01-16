---
id: US-020-002
epic: EPIC-020-secure-sdlc-quality-gates
title: "SAST a code quality gates"
priority: P0
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "4 days"
path_mapping:
  code_paths:
    - .github/workflows-disabled/security-scans.yml
    - .github/workflows-disabled/code-quality.yml
    - backend/pom.xml
    - frontend/eslint.config.cjs
  test_paths:
    - backend/src/test/java/**/*.java
    - frontend/tests/e2e/**/*.spec.ts
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-002-sast-code-quality/README.md
---


# US-020-002: SAST a code quality gates

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** ✅ **DONE**
**Estimate:** 4 days

## User Story

**Jako:** Security engineer  
**Chci:** automaticky spoustet SAST a code quality analyzu pro BE i FE  
**Aby:** kriticke chyby v kodu neprosly do main.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-000-security-platform-hardening/README.md
**Další zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md

**Obsah z dokumentace:**
- SAST musi blokovat merge pri Critical/High zranitelnostech.
- Quality gate musi vynucovat minimalni standard kodu.

## Definition of Ready (DoR)

- [ ] Nastroje SAST jsou vybrane (CodeQL/SonarQube).
- [ ] Repo ma definovane build kroky pro BE/FE.
- [ ] Zavislosti na CI pipeline jsou identifikovany.
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.
- [ ] Je definovan proces pro vyjimky.

## Acceptance Criteria

- [ ] CodeQL (nebo ekvivalent) bezi na PR pro backend i frontend.
- [ ] Quality gate failne PR pri nove Critical/High issue.
- [ ] SAST report je ulozen jako artefakt a soucast summary.
- [ ] Vyjimky/false positives jsou zdokumentovane a verzovane.

## Definition of Done (DoD)

**Kód:**
- [ ] SAST workflow je implementovan pro BE/FE.
- [ ] Quality gate je napojeny do pipeline.

**Testy:**
- [ ] Testovaci commit s umyslnou chybou SAST failne pipeline.

**Dokumentace:**
- [ ] Popis SAST pravidel a vyjimek je v README.

**Deployment:**
- [ ] SAST gate je aktivni v PR pipeline.

## Závislosti

- EPIC-000 (Security baseline)
- EPIC-020 (CI orchestration)

## Implementační tasky

- [TASK-020-002-01: Konfigurace CodeQL pro BE a FE](subtasks/TASK-020-002-01-codeql-setup.md)
- [TASK-020-002-02: Nastaveni SonarQube quality gate](subtasks/TASK-020-002-02-sonarqube-setup.md)
- [TASK-020-002-03: Pravidla a suppressions pro SAST](subtasks/TASK-020-002-03-rules-suppressions.md)
- [TASK-020-002-04: Validace SAST gate na vzorku](subtasks/TASK-020-002-04-validation-samples.md)
