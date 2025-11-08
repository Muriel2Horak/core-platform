# S11: EPIC-014 Integration Layer

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🔴 **P0** | **Effort:** ~45h | **Sprint:** 12-13

---

## 📋 USER STORY

**Jako** Developer, **chci** integrovat EPIC-014 Design System komponenty do EPIC-016, **abych** zajistil konzistentní UX a accessibility.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Replace MUI Components**: Nahradit MUI DataGrid → EPIC-014 S9 Table
2. **Form Integration**: EPIC-014 S3 Forms v popup editacích
3. **Loading States**: EPIC-014 S7 Loading skeletons pro DataView
4. **Error Handling**: EPIC-014 S8 Error boundaries
5. **Accessibility**: WCAG 2.1 AA compliance (EPIC-014 S6)

---

## 🏗️ TASK BREAKDOWN (~45h)

### T1: Table Component Migration (15h)
- Replace MUI DataGrid → EPIC-014 Table
- Update props mapping
- Test all DataView modes

### T2: Form Component Integration (10h)
- Use EPIC-014 Form in popups
- Update validation logic

### T3: Loading State Integration (8h)
- Add EPIC-014 Skeleton to DataView
- Grid layout skeleton

### T4: Error Boundary Integration (5h)
- Wrap DataView in EPIC-014 ErrorBoundary

### T5: Accessibility Audit (5h)
- WCAG 2.1 AA compliance check
- Screen reader testing

### T6: Testing (2h)

---

## 📦 DEPENDENCIES

- **EPIC-014 S3**: Forms ⏳
- **EPIC-014 S6**: Accessibility ⏳
- **EPIC-014 S7**: Loading ⏳
- **EPIC-014 S8**: Errors ⏳
- **EPIC-014 S9**: Tables ⏳

---

## 📊 SUCCESS METRICS

- 100% EPIC-014 component usage
- WCAG 2.1 AA compliant
- Zero MUI dependencies

