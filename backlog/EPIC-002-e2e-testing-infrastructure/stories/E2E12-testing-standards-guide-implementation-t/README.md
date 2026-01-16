---
id: S12
epic: EPIC-002-e2e-testing-infrastructure
title: "Testing Standards & Guide - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "8 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E12-testing-standards-guide-implementation-t/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---


# S12: Testing Standards & Guide (Implementation)

**Status:** 🔵 TODO  
**Effort:** ~8h | **LOC:** ~600  
**Owner:** QA + Dev Leads

---

## 📋 Story Description

Jako **developer**, chci **testing standards a guide**, abych **psal testy konzistentne a rychle**.

---

## 🎯 Acceptance Criteria

1. **Guide**
   - Dokumentace pokryva unit/integration/e2e/a11y
   - Jasne konvence pro naming a tagging

2. **Examples**
   - Priklady pro Playwright, JUnit, Vitest
   - Best practices pro test data

3. **How-to run**
   - Makefile prikazy pro lokalni beh
   - Troubleshooting nejcastejsich chyb

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Testing Guide Documentation](subtasks/T1-testing-guide.md) | 4h | none |
| 2 | [T2: Code Examples (All Types)](subtasks/T2-code-examples.md) | 3h | T1 |
| 3 | [T3: Running Instructions (Makefile)](subtasks/T3-running-tests.md) | 1h | T1 |
