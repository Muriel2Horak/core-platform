---
id: S11
epic: EPIC-002-e2e-testing-infrastructure
title: "CI/CD Quality Gates - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "6 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E11-ci-cd-quality-gates-implementation-tasks/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---

# S11: CI/CD Quality Gates (Implementation)

**Status:** 🔵 TODO  
**Effort:** ~6h | **LOC:** ~400  
**Owner:** DevOps + QA

---

## 📋 Story Description

Jako **team lead**, chci **quality gates v CI**, abych **blokoval merge pri selhani testu nebo poklesu coverage**.

---

## 🎯 Acceptance Criteria

1. **PR gates**
   - Smoke E2E v PR je povinny
   - Unit/Integration testy nesmi failnout

2. **Coverage thresholds**
   - FE/BE coverage prahy jsou vynucene v CI
   - Fail pipeline pri poklesu

3. **Signal**
   - CI logy jasne ukazuji fail reason

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: GitHub Actions Workflow](subtasks/T1-github-actions.md) | 2h | none |
| 2 | [T2: E2E Smoke in PR Checks](subtasks/T2-e2e-smoke-pr.md) | 1h | T1 |
| 3 | [T3: Coverage Threshold Check](subtasks/T3-coverage-threshold.md) | 2h | T1 |
| 4 | [T4: Build Fail on Test Failure](subtasks/T4-fail-on-tests.md) | 1h | T2, T3 |

---

## 🔗 Zavisnosti

- **E2E1/E2E2:** E2E framework a POM  
- **E2E9:** Tagging system (smoke tags)
