# S1: MUI Theme Foundation

**EPIC:** [EPIC-004: UX/UI & Design System](../README.md)  
**Status:** ✅ **DONE**  
**Implementováno:** Červenec 2024  
**LOC:** ~500 řádků

---

## 📋 Story Description

Jako **frontend developer**, chci **MUI theme s glassmorphic designem a dark/light mode**, abych **měl konzistentní styling napříč celou aplikací**.

---

## 🎯 Acceptance Criteria

### AC1: Theme Configuration
- **GIVEN** aplikace nastartuje
- **WHEN** zobrazím jakoukoliv stránku
- **THEN** používá corePlatformTheme
- **AND** respektuje systémový dark/light mode

### AC2: Glassmorphic Design
- **GIVEN** komponenta s MUI Paper
- **WHEN** zobrazím komponentu
- **THEN** vidím semi-transparent background
- **AND** backdrop-filter: blur efekt

### AC3: Color Palette
- **GIVEN** MUI komponenta
- **WHEN** použiju `color="primary"`
- **THEN** zobrazí #1976d2 (modrá)

### AC4: Typography Scale
- **GIVEN** <Typography variant="h1">
- **WHEN** zobrazím text
- **THEN** velikost 2.5rem, weight 600

---

## ✅ Implementation

**Files:**
- `frontend/src/shared/theme/theme.ts` (313 lines)
- `frontend/src/App.jsx` (ThemeProvider setup)

**Details:** See [EPIC-004 README](../README.md#s1-mui-theme-foundation)
