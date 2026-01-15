---
id: S9
epic: EPIC-003-monitoring-observability
title: "Reporting Dashboards - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-003-monitoring-observability/stories/MON9-reporting-dashboards-implementation-task/README.md
    - backlog/EPIC-003-monitoring-observability/README.md
---

# S9: Reporting Dashboards (Cube.js Analytics)

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** 🔴 TODO  
**Effort:** ~16h | **LOC:** ~1,800  
**Owner:** Data + Frontend

---

## 📋 Story Description

Jako **management/finance**, chci **reporting dashboardy nad Cube.js**, abych **mel konzistentni prehled o revenue, usage a compliance napric tenanty**.

---

## 🎯 Acceptance Criteria

1. **Datovy model**
   - Cube.js schemata pokryvaji revenue, usage, compliance a tenanty
   - Pre-aggregations funguje a query P95 < 1s

2. **Dashboardy**
   - Revenue, Usage, Compliance a Executive Summary dostupne v UI
   - Kazdy dashboard ma definovane filtry (tenant, time range)

3. **Tenant izolace**
   - Tenant vidi pouze vlastni data
   - Admin realm muze videt aggregate all-tenant view

4. **Export a audit**
   - CSV/JSON export z kazdeho dashboardu
   - Audit log pro export akce

---

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Cube.js Schema Definition](subtasks/T1-cubejs-schema-definition.md) | 6h | EPIC-004, EPIC-013 |
| 2 | [T2: Revenue Dashboard](subtasks/T2-revenue-dashboard.md) | 4h | T1 |
| 3 | [T3: Usage Reports Dashboard](subtasks/T3-usage-reports-dashboard.md) | 3h | T1 |
| 4 | [T4: Compliance Dashboard](subtasks/T4-compliance-dashboard.md) | 3h | T1 |
| 5 | [T5: Executive Summary](subtasks/T5-executive-summary.md) | 2h | T2, T3, T4 |

---

## 🔗 Závislosti

- **EPIC-004:** Reporting analytics infrastructure  
- **EPIC-013:** Reporting module (data sources)
