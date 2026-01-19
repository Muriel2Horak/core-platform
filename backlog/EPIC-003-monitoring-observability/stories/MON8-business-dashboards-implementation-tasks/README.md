---
id: S8
epic: EPIC-003-monitoring-observability
title: "Business Dashboards - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-003-monitoring-observability/stories/MON8-business-dashboards-implementation-tasks/README.md
    - backlog/EPIC-003-monitoring-observability/README.md
---

# S8: Business Dashboards (Native Monitoring UI)

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** 🔴 TODO  
**Effort:** ~20h | **LOC:** ~2,500  
**Owner:** Frontend + Backend

---

## 📋 Story Description

Jako **tenant admin / ops**, chci **nativni business dashboardy v monitoringu**, abych **mel okamzity prehled o provozu, vykonu a uzivatelske aktivite bez nutnosti Grafana**.

---

## 🎯 Acceptance Criteria

1. **Dashboardy a navigace**
   - `Overview`, `User Activity`, `System Health`, `Analytics` dostupne z jednoho menu
   - Breadcrumbs a route state funguje
   - Responzivni layout (desktop + tablet)

2. **Tenant scoping**
   - Tenant vidi pouze vlastni data (realm + tenant_id scope)
   - Admin realm muze prepinat tenant kontext (read-only)

3. **Data freshness**
   - Default refresh 30s, manual refresh dostupny
   - P95 load cas hlavniho dashboardu < 2s

4. **Export**
   - Export vybranych grafu do CSV/JSON/PDF
   - Export respektuje tenant scope

---

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Dashboard Layout & Navigation](subtasks/T1-dashboard-layout-navigation.md) | 4h | none |
| 2 | [T2: Metric Cards & Charts Library](subtasks/T2-metric-cards-charts-library.md) | 6h | T1 |
| 3 | [T3: Backend API Endpoints](subtasks/T3-backend-api-endpoints.md) | 4h | MON1, MON5, MON6 |
| 4 | [T4: Real-Time Updates (WebSocket)](subtasks/T4-realtime-updates-websocket.md) | 3h | T3 |
| 5 | [T5: Data Export (CSV/JSON/PDF)](subtasks/T5-data-export.md) | 3h | T2, T3 |

---

## 🔗 Závislosti

- **MON1:** Prometheus metriky (backend)  
- **MON5:** Centralized logging (Loki)  
- **MON6:** Health checks / readiness  
- **MON10:** Real-time widgets (navazuje na websocket infrastrukturu)
