---
id: S10
epic: EPIC-002-e2e-testing-infrastructure
title: "Coverage Dashboard - Implementation Tasks"
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
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E10-coverage-dashboard-implementation-tasks/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---


# S10: Coverage Dashboard (Implementation)

**Status:** 🔵 TODO  
**Effort:** ~8h | **LOC:** ~500  
**Owner:** DevOps + QA

---

## 📋 Story Description

Jako **QA/DevOps**, chci **coverage dashboard pro test registry**, abych **videl pokryti user stories napric testy**.

---

## 🎯 Acceptance Criteria

1. **Grafana dashboard**
   - Dashboard zobrazuje coverage per story a per test type
   - Trend coverage v case (daily)

2. **Datasource**
   - Grafana ma datasource na PostgreSQL `test_registry`
   - Query performance < 1s pro coverage views

3. **Security**
   - Dashboard dostupny jen admin realm

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Grafana Dashboard JSON](subtasks/T1-grafana-dashboard.md) | 3h | E2E8 |
| 2 | [T2: PostgreSQL Data Source](subtasks/T2-postgresql-datasource.md) | 2h | E2E8 |
| 3 | [T3: Dashboard Panels (Coverage)](subtasks/T3-coverage-panels.md) | 3h | T1, T2 |

---

## 🔗 Zavisnosti

- **E2E8:** Test Registry & Tracking (data source)
