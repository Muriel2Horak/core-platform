---
id: S7
epic: EPIC-014-ux-ui-design-system
title: "Loading States & Animations - Task Breakdown"
priority: P2
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "5 hours"
path_mapping:
  code_paths:
    - frontend/src/shared/ui
    - frontend/src/styles
  test_paths:
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-014-ux-ui-design-system/stories/UX7-loading-states-animations-task-breakdown/README.md
    - backlog/EPIC-014-ux-ui-design-system/README.md
---


# S7: Loading States & Animations - Task Breakdown

**EPIC:** [EPIC-014: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**
**Priority:** P2  
**Effort:** ~5h | **LOC:** ~400

## 📋 Story Description

Jako **uzivatel** potrebuji **jasny loading feedback a jemne animace**, abych **mel jistotu, ze system pracuje a UI je citelne**.

## ✅ Acceptance Criteria

1. **Loading indikatory**
   - Komponenty pro loader/skeleton jsou pouzitelne v tabulkach, kartach a formulacich.
   - Primary akce maji disabled/loading stav.

2. **Micro-animations**
   - Animace jsou konzistentni (duration, easing) podle tokenu.
   - `prefers-reduced-motion` je respektovano.

3. **Page transitions**
   - Route zmeny maji plynuly prechod bez layout shift.
   - Prechod neblokuje interakce dele nez 200ms.

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Loading Indicators](subtasks/T1-loading-indicators.md) | 2h | EPIC-014 S1, S2 |
| 2 | [T2: Micro-Animations](subtasks/T2-micro-animations.md) | 2h | T1 |
| 3 | [T3: Page Transitions](subtasks/T3-page-transitions.md) | 1h | T1 |

## 🔗 Závislosti

- EPIC-014 S1 (MUI Theme Foundation)
- EPIC-014 S2 (Design Tokens System)
