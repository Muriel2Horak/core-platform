---
id: S6
epic: EPIC-014-ux-ui-design-system
title: "Accessibility (WCAG 2.1 AA) - Task Breakdown"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "10 hours"
path_mapping:
  code_paths:
    - frontend/src/shared/ui
    - frontend/src/shared/theme
    - frontend/src/components
  test_paths:
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-014-ux-ui-design-system/stories/UX6-accessibility-wcag-2-1-aa-task-breakdown/README.md
    - backlog/EPIC-014-ux-ui-design-system/README.md
---

# S6: Accessibility (WCAG 2.1 AA) - Task Breakdown

**EPIC:** [EPIC-014: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**
**Priority:** P1  
**Effort:** ~10h | **LOC:** ~700

## 📋 Story Description

Jako **uzivatel se specifickymi potrebami** potrebuji **plne pristupne UI**, abych **mohl platformu pouzivat bez bariér** a splnovali jsme WCAG 2.1 AA.

## ✅ Acceptance Criteria

1. **Klavesova navigace**
   - Vsechny interaktivni prvky jsou dostupne z klavesnice.
   - Fokus poradi je logicke napric layoutem.

2. **Screen reader**
   - Formulare, navigace a tabulky maji spravne ARIA role/labels.
   - Chybove hlasky jsou oznamene pomoci `aria-live`.

3. **Kontrast a tema**
   - Text splnuje minimalni kontrast 4.5:1 (AA).
   - Theme/tokens obsahuji definice pro high-contrast variantu.

4. **Focus management**
   - Viditelny focus ring na vsech ovladacich.
   - Dialogy a drawery maji focus trap a spravne focus return.

5. **Audit**
   - A11y audit (axe/Lighthouse) bez critical chyb pro hlavni komponenty.

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Keyboard Navigation](subtasks/T1-keyboard-navigation.md) | 3h | EPIC-014 S1, S2 |
| 2 | [T2: Screen Reader Support](subtasks/T2-screen-reader.md) | 3h | T1 |
| 3 | [T3: Color Contrast & Themes](subtasks/T3-color-contrast.md) | 2h | EPIC-014 S1, S2 |
| 4 | [T4: Focus Management](subtasks/T4-focus-management.md) | 2h | T1, T2 |

## 🔗 Závislosti

- EPIC-014 S1 (MUI Theme Foundation)
- EPIC-014 S2 (Design Tokens System)
- EPIC-014 S3 (Form Components)
- EPIC-014 S4 (Navigation Patterns)
