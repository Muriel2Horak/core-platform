---
id: S9
epic: EPIC-014-ux-ui-design-system
title: "Data Table Components - Task Breakdown"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "12 hours"
path_mapping:
  code_paths:
    - frontend/src/components/common
    - frontend/src/shared/ui
  test_paths:
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-014-ux-ui-design-system/stories/UX9-data-table-components-task-breakdown/README.md
    - backlog/EPIC-014-ux-ui-design-system/README.md
---

# S9: Data Table Components - Task Breakdown

**EPIC:** [EPIC-014: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**
**Priority:** P1  
**Effort:** ~12h | **LOC:** ~900

## 📋 Story Description

Jako **uzivatel pracujici s daty** potrebuji **robustni tabulkove komponenty**, abych **mohl rychle filtrovat, tridit a analyzovat velke datasety**.

## ✅ Acceptance Criteria

1. **Core funkce**
   - Tabulka podporuje sorting, filtering, pagination a column resize.
   - Sloupce maji jednotne styling a sticky header.

2. **Virtualizace**
   - Pro 10k+ radku je zapnuta virtual scrolling.
   - Scroll je plynuly bez viditelneho lag.

3. **Responsivita**
   - Na mobilu se tabulka prepina do card/list view.
   - Skryte sloupce jsou dostupne v detailu radku.

4. **Advanced features**
   - Row selection + bulk actions.
   - Column visibility/presets a export trigger (CSV/PDF).

5. **A11y**
   - Tabulka je ovladatelna klavesnici a ma ARIA labels.

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Table Core Features](subtasks/T1-table-core.md) | 4h | EPIC-014 S1, S2 |
| 2 | [T2: Virtual Scrolling](subtasks/T2-virtual-scrolling.md) | 4h | T1 |
| 3 | [T3: Responsive Table Patterns](subtasks/T3-responsive-table.md) | 2h | T1, EPIC-014 S5 |
| 4 | [T4: Advanced Features](subtasks/T4-advanced-features.md) | 2h | T1 |

## 🔗 Závislosti

- EPIC-014 S1 (MUI Theme Foundation)
- EPIC-014 S2 (Design Tokens System)
- EPIC-014 S5 (Responsive Design)
- EPIC-014 S6 (Accessibility)
