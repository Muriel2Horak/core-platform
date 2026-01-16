---
id: S14
epic: EPIC-002-e2e-testing-infrastructure
title: "Test Data Management - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "14 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E14-test-data-management-implementation-task/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---


# S14: Test Data Management (Implementation)

**Status:** 🔵 TODO  
**Effort:** ~14h | **LOC:** ~1,200  
**Owner:** QA + Backend

---

## 📋 Story Description

Jako **tester**, chci **bezpecne test data management**, abych **mel deterministicke testy bez rizika leakage do produkce**.

---

## 🎯 Acceptance Criteria

1. **Test data lifecycle**
   - Seeder + cleanup pokryva vsechny testy
   - Builders/fixtures pro konzistentni data

2. **Safety guards**
   - Ochrana proti behu na production
   - DB triggers blokujici test data v prod

3. **API helpers**
   - Test-only API pro tvorbu/mazani data
   - Playwright helpery pro testy

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Test Data Seeder (@Profile)](subtasks/T1-test-data-seeder.md) | 3h | none |
| 2 | [T2: Production Safety Guards](subtasks/T2-production-safety-guards.md) | 2h | T1 |
| 3 | [T3: Database Triggers (Block Test Data)](subtasks/T3-database-triggers.md) | 2h | T2 |
| 4 | [T4: Test Data Cleanup (@AfterEach)](subtasks/T4-test-data-cleanup.md) | 2h | T1 |
| 5 | [T5: Test Data Builders (Fluent API)](subtasks/T5-test-data-builders.md) | 2h | T1 |
| 6 | [T6: E2E Test Data Helpers (Playwright)](subtasks/T6-ee-test-data-helpers.md) | 2h | T5 |
| 7 | [T7: Test Data API (Dev/Test Only)](subtasks/T7-test-data-api.md) | 2h | T1, T2 |

---

## 🔗 Zavisnosti

- **E2E3:** Test data factories  
- **E2E1:** E2E framework setup
