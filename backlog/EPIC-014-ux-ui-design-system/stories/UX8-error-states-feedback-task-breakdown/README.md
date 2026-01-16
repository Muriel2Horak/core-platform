---
id: S8
epic: EPIC-014-ux-ui-design-system
title: "Error States & Feedback - Task Breakdown"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "8 hours"
path_mapping:
  code_paths:
    - frontend/src/shared/ui
    - frontend/src/components
  test_paths:
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-014-ux-ui-design-system/stories/UX8-error-states-feedback-task-breakdown/README.md
    - backlog/EPIC-014-ux-ui-design-system/README.md
---


# S8: Error States & Feedback - Task Breakdown

**EPIC:** [EPIC-014: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**
**Priority:** P1  
**Effort:** ~8h | **LOC:** ~600

## 📋 Story Description

Jako **uzivatel** potrebuji **jasne error stavy a feedback**, abych **vedel co se stalo a jak pokracovat**.

## ✅ Acceptance Criteria

1. **Error message komponenta**
   - Podporuje severity (info/warn/error) a akci (retry/dismiss).
   - Error text je konzistentni napric UI (formulare, tabulky, detail).

2. **Toast/notification system**
   - Toasty maji frontu, auto-dismiss a manual close.
   - Primarni akce (save/export) vraci success/error toast.

3. **Empty states**
   - Empty state ma icon + message + CTA.
   - Podporuje prazdne tabulky, vyhledavani a filtry.

4. **Global error handling**
   - Existuje jednotny pattern pro API chyby (mapovani kodu na UI).

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Error Message Components](subtasks/T1-error-messages.md) | 3h | EPIC-014 S1, S2 |
| 2 | [T2: Toast Notification System](subtasks/T2-toast-notifications.md) | 3h | T1 |
| 3 | [T3: Empty States](subtasks/T3-empty-states.md) | 2h | T1 |

## 🔗 Závislosti

- EPIC-014 S1 (MUI Theme Foundation)
- EPIC-014 S2 (Design Tokens System)
