# 🚀 EPIC-014 Implementation Plan: Unblock Dependencies

**Created:** 7. listopadu 2025  
**Goal:** Dokončit EPIC-014 (UX/UI Design System) → Odblokovat EPIC-004 + EPIC-003  
**Effort:** ~80 hours (2 developers × 4 weeks)  
**Priority:** 🔴 CRITICAL - blokuje 4 EPICs (EPIC-003, 004, 005, 002)

---

## 🎯 Success Criteria

**Definition of Done:**
- ✅ All S3, S6, S7, S8, S9 tasks implemented (18 tasks total)
- ✅ EPIC-004 can use design system components (forms, tables, loading, errors)
- ✅ EPIC-003 can implement S8-S10 (Business + Reporting Dashboards)
- ✅ WCAG 2.1 AA compliance achieved
- ✅ Storybook documentation for all components
- ✅ Unit tests (>80% coverage)

**Blocked EPICs unblocked:**
1. **EPIC-004** (Reporting Analytics) - dashboard UX polish
2. **EPIC-003** (Monitoring) - S8-S10 frontend dashboards
3. **EPIC-005** (Metamodel) - META-005 Visual Studio UI
4. **EPIC-002** (E2E Testing) - S10 Coverage Dashboard

---

## 📋 Sprint Plan (4 Weeks, 2 Developers)

### 🏃 **Sprint 1: Critical Blockers** (Week 1-2, ~40h)

**Goal:** Unblock EPIC-004 tabular reports + filters

#### **Story 1: S9 - Data Table Components** (~12h, Priority P0)
**Why Critical:** Blokuje 4 EPICs! (EPIC-004 tables, EPIC-003 S8, EPIC-002 S10, EPIC-005 META-005)

**Tasks:**
- [ ] **T1: Table Core Component** (3h)
  - Path: `stories/S9/T1-table-core.md`
  - Deliverable: MUI DataGrid wrapper, theme integration
  - Definition of Done: Basic table renders with theme styles
  
- [ ] **T2: Virtual Scrolling** (3h)
  - Path: `stories/S9/T2-virtual-scrolling.md`
  - Deliverable: Optimize for 10k+ rows
  - Definition of Done: Table handles large datasets without lag
  
- [ ] **T3: Responsive Table** (3h)
  - Path: `stories/S9/T3-responsive-table.md`
  - Deliverable: Mobile-friendly table (card view on small screens)
  - Definition of Done: Table usable on mobile devices
  
- [ ] **T4: Advanced Features** (3h)
  - Path: `stories/S9/T4-advanced-features.md`
  - Deliverable: Sorting, filtering, column visibility, export
  - Definition of Done: All advanced features working

**Unblocks:**
- ✅ EPIC-004 tabular reports → production-ready tables
- ✅ EPIC-003 S8 (Business Dashboards) → data display
- ✅ EPIC-002 S10 (Coverage Dashboard) → test metrics table
- ✅ EPIC-005 META-005 (Visual Studio) → entity browser

---

#### **Story 2: S3 - Form Components & Validation** (~10h, Priority P0)
**Why Critical:** Blokuje EPIC-004 advanced filters, EPIC-003 S9 reporting filters

**Tasks:**
- [ ] **T1: Form Components** (3h)
  - Path: `stories/S3/T1-form-components.md`
  - Deliverable: Input, Select, Checkbox, Radio wrappers
  - Definition of Done: All form inputs styled with theme
  
- [ ] **T2: Validation System** (3h)
  - Path: `stories/S3/T2-validation-system.md`
  - Deliverable: Yup schema integration, error display
  - Definition of Done: Form validation working with error messages
  
- [ ] **T3: Form State Management** (2h)
  - Path: `stories/S3/T3-form-state-management.md`
  - Deliverable: React Hook Form integration
  - Definition of Done: Forms handle state correctly
  
- [ ] **T4: Accessibility** (2h)
  - Path: `stories/S3/T4-accessibility.md`
  - Deliverable: ARIA labels, keyboard navigation
  - Definition of Done: Forms pass accessibility audit

**Unblocks:**
- ✅ EPIC-004 advanced filters → date pickers, multi-select
- ✅ EPIC-003 S9 (Reporting Dashboards) → filter UI
- ✅ EPIC-005 META-004 (Constraint editor) → form builder

---

#### **Story 3: S7 - Loading States & Animations** (~5h, Priority P1)
**Why Important:** Výrazně zlepší UX během dlouhých Cube.js queries

**Tasks:**
- [ ] **T1: Loading Indicators** (2h)
  - Path: `stories/S7/T1-loading-indicators.md`
  - Deliverable: Skeleton loaders, spinners, progress bars
  - Definition of Done: Loading states for all async operations
  
- [ ] **T2: Micro-Animations** (2h)
  - Path: `stories/S7/T2-micro-animations.md`
  - Deliverable: Hover, click, focus animations
  - Definition of Done: Smooth 60fps animations
  
- [ ] **T3: Page Transitions** (1h)
  - Path: `stories/S7/T3-page-transitions.md`
  - Deliverable: Fade, slide transitions
  - Definition of Done: Page transitions feel polished

**Unblocks:**
- ✅ EPIC-004 long query UX → skeleton while loading
- ✅ EPIC-003 S10 (Real-time Widgets) → loading states

---

#### **Story 4: S8 - Error States & User Feedback** (~8h, Priority P1)
**Why Important:** Production-ready error handling pro EPIC-004

**Tasks:**
- [ ] **T1: Error Messages** (3h)
  - Path: `stories/S8/T1-error-messages.md`
  - Deliverable: Error display component, error boundaries
  - Definition of Done: All errors caught and displayed nicely
  
- [ ] **T2: Toast Notifications** (3h)
  - Path: `stories/S8/T2-toast-notifications.md`
  - Deliverable: Toast system (success, error, warning, info)
  - Definition of Done: Toasts work globally
  
- [ ] **T3: Empty States** (2h)
  - Path: `stories/S8/T3-empty-states.md`
  - Deliverable: No data, no results, 404 states
  - Definition of Done: Empty states guide users

**Unblocks:**
- ✅ EPIC-004 query error handling → retry UI
- ✅ EPIC-003 S10 (Real-time Widgets) → error recovery

---

### 🏃 **Sprint 2: Compliance** (Week 3, ~15h)

#### **Story 5: S6 - Accessibility (WCAG 2.1 AA)** (~12h, Priority P0)
**Why Critical:** ⚠️ LEGAL COMPLIANCE REQUIREMENT!

**Tasks:**
- [ ] **T1: Keyboard Navigation** (3h)
  - Path: `stories/S6/T1-keyboard-navigation.md`
  - Deliverable: Tab order, skip links, keyboard shortcuts
  - Definition of Done: App fully keyboard-navigable
  
- [ ] **T2: Screen Reader Support** (4h)
  - Path: `stories/S6/T2-screen-reader.md`
  - Deliverable: ARIA labels, roles, live regions
  - Definition of Done: Screen reader announces all actions
  
- [ ] **T3: Color Contrast** (2h)
  - Path: `stories/S6/T3-color-contrast.md`
  - Deliverable: WCAG AA contrast ratios (4.5:1)
  - Definition of Done: All text passes contrast checker
  
- [ ] **T4: Focus Management** (3h)
  - Path: `stories/S6/T4-focus-management.md`
  - Deliverable: Focus indicators, focus trapping in modals
  - Definition of Done: Focus always visible and logical

**Unblocks:**
- ✅ EPIC-004 WCAG compliance → legal requirement met
- ✅ EPIC-003 WCAG compliance → monitoring dashboards accessible

**Testing:**
- [ ] Lighthouse accessibility score >90
- [ ] axe DevTools: 0 violations
- [ ] Manual screen reader test (VoiceOver)
- [ ] Manual keyboard navigation test

---

### 🏃 **Sprint 3: Polish** (Week 4, ~25h - optional)

#### **Story 6: S4 - Navigation Patterns** (~8h, Priority P2)
**Tasks:**
- [ ] T1-T4 (Path: `stories/S4/`)

#### **Story 7: S5 - Responsive Design** (~7h, Priority P2)
**Tasks:**
- [ ] T1-T3 (Path: `stories/S5/`)

#### **Story 8: S10 - Design Tokens & Documentation** (~8h, Priority P2)
**Tasks:**
- [ ] T1-T3 (Path: `stories/S10/`)

**Note:** S4, S5, S10 jsou nice-to-have, můžou počkat. Focus na S3, S6, S7, S8, S9 first!

---

## 📊 Progress Tracking

### Week-by-Week Targets:

**Week 1: Data Tables + Forms** (~22h)
```
✅ S9: Data Table Components (12h)
   - T1: Table Core (3h)
   - T2: Virtual Scrolling (3h)
   - T3: Responsive Table (3h)
   - T4: Advanced Features (3h)

✅ S3: Form Components (10h)
   - T1: Form Components (3h)
   - T2: Validation System (3h)
   - T3: Form State (2h)
   - T4: Accessibility (2h)
```

**Week 2: Loading + Errors** (~13h)
```
✅ S7: Loading States (5h)
   - T1: Loading Indicators (2h)
   - T2: Micro-Animations (2h)
   - T3: Page Transitions (1h)

✅ S8: Error States (8h)
   - T1: Error Messages (3h)
   - T2: Toast Notifications (3h)
   - T3: Empty States (2h)
```

**Week 3: Accessibility** (~12h)
```
✅ S6: Accessibility (12h)
   - T1: Keyboard Navigation (3h)
   - T2: Screen Reader (4h)
   - T3: Color Contrast (2h)
   - T4: Focus Management (3h)
```

**Week 4: Testing + Documentation** (~15h)
```
✅ Integration testing (5h)
✅ Storybook docs (5h)
✅ E2E tests (5h)
```

---

## 🎯 Milestone Checkpoints

### Checkpoint 1: After Week 1 (S9 + S3 done)
**Unblocked EPICs:**
- ✅ EPIC-004 can implement tabular reports
- ✅ EPIC-004 can implement advanced filters
- ✅ EPIC-003 S8 can start (Business Dashboards)

**Verification:**
```bash
# Test EPIC-004 integration
cd frontend/src/components/dashboards
# Should see: import { DataTable, FormInput } from '@/shared/components'
```

### Checkpoint 2: After Week 2 (S7 + S8 done)
**Unblocked EPICs:**
- ✅ EPIC-004 has production-ready error handling
- ✅ EPIC-004 has polished loading UX
- ✅ EPIC-003 S10 can start (Real-time Widgets)

**Verification:**
```bash
# Test EPIC-004 loading states
# Dashboard queries should show skeleton loaders
# Errors should show retry UI with toast notifications
```

### Checkpoint 3: After Week 3 (S6 done)
**Compliance Achieved:**
- ✅ WCAG 2.1 AA compliance
- ✅ EPIC-004 + EPIC-003 legally compliant
- ✅ Lighthouse accessibility >90

**Verification:**
```bash
# Run accessibility audit
npm run test:a11y
# Should pass: Lighthouse >90, axe 0 violations
```

---

## 🚀 Immediate Next Steps (This Week)

### Developer 1: Focus on S9 (Data Tables)
```bash
cd backlog/EPIC-014-ux-ui-design-system/stories/S9

# Day 1-2: T1 + T2 (Table Core + Virtual Scrolling)
# Day 3: T3 (Responsive Table)
# Day 4: T4 (Advanced Features)

# Implementation path:
frontend/src/shared/components/DataTable/
├── DataTable.tsx          # Main component
├── DataTableVirtual.tsx   # Virtual scrolling
├── DataTableMobile.tsx    # Responsive card view
└── DataTableToolbar.tsx   # Filters, export, column visibility
```

### Developer 2: Focus on S3 (Form Components)
```bash
cd backlog/EPIC-014-ux-ui-design-system/stories/S3

# Day 1-2: T1 + T2 (Components + Validation)
# Day 3: T3 (Form State Management)
# Day 4: T4 (Accessibility)

# Implementation path:
frontend/src/shared/components/Form/
├── Input.tsx
├── Select.tsx
├── Checkbox.tsx
├── Radio.tsx
├── FormField.tsx          # Wrapper with label + error
└── useFormValidation.ts   # Yup integration hook
```

---

## 📈 Impact Metrics

### Before EPIC-014 Completion:
- ❌ EPIC-004 dashboards use direct MUI (inconsistent UX)
- ❌ EPIC-003 S8-S10 blocked (no design system components)
- ❌ WCAG compliance: Unknown (not tested)
- ❌ Component reuse: ~40% (ad-hoc components)

### After EPIC-014 Completion:
- ✅ EPIC-004 dashboards use design system (consistent UX)
- ✅ EPIC-003 S8-S10 unblocked (can implement dashboards)
- ✅ WCAG compliance: WCAG 2.1 AA certified
- ✅ Component reuse: ~90% (standardized components)
- ✅ Development speed: +50% (reusable components)

---

## 🎯 Definition of Done (Overall)

**EPIC-014 is considered DONE when:**

1. **All Critical Stories Complete:**
   - ✅ S3: Form Components (4/4 tasks)
   - ✅ S6: Accessibility (4/4 tasks)
   - ✅ S7: Loading States (3/3 tasks)
   - ✅ S8: Error States (3/3 tasks)
   - ✅ S9: Data Tables (4/4 tasks)

2. **EPIC-004 Integration:**
   - ✅ Reporting dashboards use `<DataTable>` component
   - ✅ Advanced filters use `<FormInput>`, `<Select>` components
   - ✅ Loading states use `<SkeletonLoader>` component
   - ✅ Errors use `<ErrorBoundary>` + `<Toast>` components

3. **EPIC-003 Integration:**
   - ✅ S8 (Business Dashboards) can implement using design system
   - ✅ S9 (Reporting Dashboards) can implement using design system
   - ✅ S10 (Real-time Widgets) has loading + error states

4. **Quality Gates:**
   - ✅ Storybook documentation for all components
   - ✅ Unit tests >80% coverage
   - ✅ E2E tests for critical user flows
   - ✅ Lighthouse accessibility score >90
   - ✅ axe DevTools: 0 violations

5. **Documentation:**
   - ✅ Component usage guide
   - ✅ Design tokens documentation
   - ✅ Accessibility guidelines
   - ✅ Migration guide (MUI → Design System)

---

## 📞 Contact & Coordination

**Sprint Lead:** Martin Horak  
**Developers:** 2× Frontend (React + TypeScript experts)  
**Stakeholders:** EPIC-004 owner, EPIC-003 owner  
**Review:** Daily standups, weekly demo to stakeholders

**Slack Channel:** `#epic-014-design-system`  
**Jira Board:** `EPIC-014 Sprint Board`

---

**Last Updated:** 7. listopadu 2025  
**Status:** 🟢 READY TO START  
**Next Action:** Assign developers, start Sprint 1 (S9 + S3)
