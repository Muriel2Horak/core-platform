---
id: S2
epic: EPIC-014-ux-ui-design-system
title: "Design Tokens System"
priority: P2
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: ""
path_mapping:
  code_paths:
    - frontend/src/shared/theme/tokens.ts
  test_paths: []
  docs_paths:
    - backlog/EPIC-014-ux-ui-design-system/stories/UX2-design-tokens-system/README.md
    - backlog/EPIC-014-ux-ui-design-system/README.md
---

# S2: Design Tokens System

**EPIC:** [EPIC-004: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**
**Implementováno:** Červenec 2024  
**LOC:** ~300 řádků

---

## 📋 Story Description

Jako **frontend developer**, chci **centralized design tokens**, abych **používal konzistentní hodnoty pro spacing, colors, shadows napříč aplikací**.

---

## 🎯 Acceptance Criteria

### AC1: Tokens Exported
- **GIVEN** import `import { tokens } from '@/shared/theme/tokens'`
- **WHEN** použiju `tokens.spacing.md`
- **THEN** dostanu 16 (px)

### AC2: TypeScript Autocomplete
- **GIVEN** VSCode s TypeScript
- **WHEN** píšu `tokens.`
- **THEN** zobrazí autocomplete (colors, spacing, borderRadius, shadows, breakpoints)

### AC3: Tokens Usage
- **GIVEN** komponenta potřebuje spacing
- **WHEN** použiju `sx={{ p: tokens.spacing.md / 8 }}`  # MUI spacing(2) = 16px
- **THEN** aplikuje padding 16px

---

## ✅ Implementation

**Files:**
- `frontend/src/shared/theme/tokens.ts`

**Usage Examples:**
```tsx
import { tokens } from '@/shared/theme/tokens';

// Colors
<Box sx={{ bgcolor: tokens.colors.primary.main }} />

// Spacing
<Box sx={{ mt: tokens.spacing.lg / 8 }} />  // margin-top: 24px

// Border Radius
<Card sx={{ borderRadius: `${tokens.borderRadius.md}px` }} />

// Shadows
<Paper elevation={0} sx={{ boxShadow: tokens.shadows.md }} />
```

**Details:** See [EPIC-004 README](../README.md#s2-design-tokens-system)
