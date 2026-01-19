---
id: S10
epic: EPIC-003-monitoring-observability
title: "Real-Time Monitoring Widgets - Implementation Tasks"
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
    - backlog/EPIC-003-monitoring-observability/stories/MON10-real-time-monitoring-widgets-implementat/README.md
    - backlog/EPIC-003-monitoring-observability/README.md
---

# S10: Real-Time Monitoring Widgets

**EPIC:** [EPIC-003: Monitoring & Observability](../README.md)  
**Status:** 🔴 TODO  
**Effort:** ~12h | **LOC:** ~1,200  
**Owner:** Backend + Frontend

---

## 📋 Story Description

Jako **operator/tenant admin**, chci **real-time widgety pro metriky, alerty a aktivitu**, abych **videl zmeny bez manualniho refresh**.

---

## 🎯 Acceptance Criteria

1. **WebSocket channel**
   - `/ws/live-metrics` funguje a je zabezpecen JWT
   - Tenant scope je vynucen v payloadu i na serveru

2. **Live widgets**
   - Live metrics (CPU, error rate, latency) se aktualizuji kazde 2s
   - Activity feed ukazuje posledni udalosti (errors, deployments)

3. **Alert notifications**
   - UI zobrazi real-time alert notifikace z Alertmanager
   - Historie notifikaci dostupna za poslednich 24h

4. **Fallback**
   - Pri ztrate WS spojeni UI prejde na polling

---

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: WebSocket Infrastructure](subtasks/T1-websocket-infrastructure.md) | 4h | none |
| 2 | [T2: Live Metrics Widgets](subtasks/T2-live-metrics-widgets.md) | 4h | T1, MON1 |
| 3 | [T3: Activity Feed Widget](subtasks/T3-activity-feed-widget.md) | 2h | T1, MON5 |
| 4 | [T4: Alert Notifications](subtasks/T4-alert-notifications.md) | 2h | T1, MON3 |

---

## 🔗 Závislosti

- **MON1:** Prometheus metriky  
- **MON3:** Alertmanager rules  
- **MON5:** Loki logy / activity stream
