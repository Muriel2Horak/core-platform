---
id: US-020-009
epic: EPIC-020-secure-sdlc-quality-gates
title: "AI-generated code guardrails"
priority: P0
status: in_progress
assignee: ""
created: 2026-01-15
updated: 2026-01-17
estimate: "2 days"
path_mapping:
  code_paths:
    - .github/PULL_REQUEST_TEMPLATE.md
    - .github/copilot-golden-rules.md
    - .github/copilot-instructions.md
    - .github/CODEOWNERS
    - .github/workflows/quality-gates-pr.yml
    - .github/workflows/quality-gates-nightly.yml
    - .github/workflows/quality-gates-release.yml
    - scripts/ci/ai-risky-patterns.txt
    - scripts/ci/check-ai-risky-patterns.sh
  test_paths:
    - backend/src/test/java/cz/muriel/core/metamodel/validator/AiSchemaValidatorTest.java
    - backend/src/test/java/cz/muriel/core/controller/ai/AiContextControllerSecurityTest.java
  docs_paths:
    - backlog/EPIC-020-secure-sdlc-quality-gates/README.md
    - backlog/EPIC-000-security-platform-hardening/README.md
    - backlog/EPIC-020-secure-sdlc-quality-gates/stories/US-020-009-ai-code-guardrails/README.md
---

# US-020-009: AI-generated code guardrails

**EPIC:** EPIC-020 Secure SDLC & Quality Gates
**Priority:** P0
**Status:** 🟡 **IN PROGRESS**
**Estimate:** 2 days

## User Story

**Jako:** Security/Platform owner  
**Chci:** mit jasna pravidla a automaticke guardrails pro AI-generated code  
**Aby:** se AI vystupy nedostaly do main bez security review.

## Dokumentační zdroje

**Primární zdroj:** backlog/EPIC-020-secure-sdlc-quality-gates/README.md
**Další zdroj:** backlog/EPIC-000-security-platform-hardening/README.md

**Obsah z dokumentace:**
- AI code musi mit security checklist a disclosure v PR.
- Risky pattern detection musi bezet na PR.
- Security-critical cesty vyzaduji explicitni review.

## Definition of Ready (DoR)

- [x] Je definovan seznam security-critical cest.
- [x] PR template je dostupny a muzeme ho upravit.
- [x] Nastroj pro pattern detection je zvolen (script/ci step).
- [ ] Akceptacni kriteria jsou testovatelna.
- [ ] Odhad je potvrzen tymem.

## Acceptance Criteria

- [x] AI Code Security Checklist je zdokumentovan a pouzit v PR.
- [x] Risky pattern detection bezi na PR a vytvari warning.
- [x] PR template vyzaduje AI disclosure (tool + files).
- [x] CODEOWNERS vynucuje security review pro citlive cesty.
- [x] Developer training materials jsou dostupne.

## Definition of Done (DoD)

**Kód:**
- [x] CI check nebo script pro risky pattern detection je nasazen.
- [x] CODEOWNERS pravidla jsou soucasti repozitare.

**Testy:**
- [ ] Testovaci PR s risky patternem je oznacen jako warning.
- [ ] CODEOWNER enforcement je overen.

**Dokumentace:**
- [x] AI checklist a pravidla jsou popsana.
- [x] Training materials jsou dostupne pro tym.

**Deployment:**
- [x] Guardrails jsou aktivni v PR pipeline.

## Závislosti

- EPIC-000 (Security baseline)
- EPIC-020 (CI orchestration)
- EPIC-003 (Observability, pokud logujeme metriky)

## Implementační tasky

- [TASK-020-009-01: AI checklist + PR template update](subtasks/TASK-020-009-01-ai-checklist-pr-template.md)
- [TASK-020-009-02: Risky pattern detection step](subtasks/TASK-020-009-02-risky-pattern-detection.md)
- [TASK-020-009-03: CODEOWNERS rules pro security review](subtasks/TASK-020-009-03-codeowners-rules.md)
- [TASK-020-009-04: AI usage metrics + training materials](subtasks/TASK-020-009-04-ai-metrics-training.md)
