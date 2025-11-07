# 🎨 EPIC-014 Dependencies: Nedokončené User Stories

**Datum:** 7. listopadu 2025  
**Účel:** Mapování všech TODO/PLANNED/IN PROGRESS stories, které čekají na dokončení EPIC-014 (UX/UI Design System)

---

## 🎯 Executive Summary

### EPIC-014 Status: 🟡 **20% COMPLETE** (2/10 stories)

**Hotovo:**
- ✅ S1: MUI Theme System (glassmorphic design, color palette, spacing tokens)
- ✅ S2: Component Library Basics (základní komponenty)

**Chybí (blokuje další EPICs):**
- ⏳ S3: Form Components & Validation (~800 LOC)
- ⏳ S4: Navigation Patterns (~600 LOC)
- ⏳ S5: Responsive Design (~500 LOC)
- ⏳ S6: Accessibility WCAG 2.1 AA (~700 LOC)
- ⏳ S7: Loading States & Animations (~400 LOC)
- ⏳ S8: Error States & Feedback (~600 LOC)
- ⏳ S9: Data Table Components (~900 LOC)
- ⏳ S10: Design Tokens & Documentation (~500 LOC)

**Celkem TODO:** ~5,000 LOC (80% EPIC-014 zbývá)

---

## 📊 Impact Analysis: Které EPICs jsou blokovány?

### 🔴 CRITICAL DEPENDENCY (100% funkční, ale čekají na UX polish)

#### **EPIC-004: Reporting Analytics Infrastructure** 
**Status:** ✅ 100% implementováno, ⚠️ čeká na EPIC-014 UI komponenty

**Závislosti na EPIC-014:**

| EPIC-014 Story | EPIC-004 Use Case | Current Workaround | Impact |
|----------------|-------------------|--------------------| ------|
| **S3: Form Components** | Advanced filter UI (date pickers, multi-select) | Basic MUI forms | ⚠️ Medium - filter UX suboptimal |
| **S9: Data Tables** | Tabular reports, sorting, pagination | Direct MUI DataGrid | ⚠️ Medium - no consistent table styles |
| **S7: Loading States** | Long query progress (Cube.js queries) | Spinner only | ⚠️ Low - acceptable UX |
| **S8: Error States** | Query failures, timeout errors | Basic error messages | ⚠️ Medium - no retry/recovery UI |
| **S6: Accessibility** | WCAG 2.1 AA compliance | Not tested | ⚠️ High - compliance risk |

**Příklad kódu (současný stav):**
```typescript
// backend/src/main/java/cz/muriel/core/reporting/api/ReportQueryController.java
// ✅ Backend používá MetamodelSpecService (EPIC-005 integrace funguje)
EntitySpec spec = metamodelSpecService.getFullEntitySpec(entity);

// frontend/src/components/dashboards/DashboardBuilder.tsx
// ⚠️ Frontend používá přímé MUI komponenty (čeká na EPIC-014 wrappery)
import { Card, Grid } from '@mui/material';  // Direct import, ne z design systému
import { useTheme } from '@mui/material/styles';  // ✅ Theme funguje (S1 done)

const DashboardBuilder = () => {
  const theme = useTheme();  // ✅ Glassmorphic theme z EPIC-014 S1
  
  return (
    <Grid container spacing={2}>  {/* ⚠️ Hardcoded spacing - čeká na S10 tokens */}
      <Card sx={{  /* ⚠️ Custom styling - měl by být z EPIC-014 S2 */
        backdropFilter: 'blur(10px)',
        backgroundColor: theme.palette.background.paper,  // ✅ Theme color
      }}>
        {/* ⏳ S3: Filtry používají basic MUI formy */}
        {/* ⏳ S9: Tabulky používají direct MUI DataGrid */}
        {/* ⏳ S7: Loading state je jen CircularProgress */}
      </Card>
    </Grid>
  );
};
```

**Blokované Future Enhancements (čekají na EPIC-014):**
- 🔒 Advanced filter UI → čeká na S3 (Form Components)
- 🔒 Enhanced table widgets → čeká na S9 (Data Tables)
- 🔒 Better UX during long queries → čeká na S7 (Loading States)
- 🔒 Robust error handling → čeká na S8 (Error States)
- 🔒 WCAG 2.1 AA compliance → čeká na S6 (Accessibility)

---

#### **EPIC-003: Monitoring & Observability**
**Status:** 🟡 70% COMPLETE (7/10 stories), ⚠️ S8-S10 čekají na EPIC-014

**Nedokončené stories (čekají na EPIC-014):**

| Story | Status | LOC | EPIC-014 Dependency | Blocker |
|-------|--------|-----|---------------------|---------|
| **S8: Business Dashboards** | 🔵 TODO | ~2,500 | S9 (Data Tables), S7 (Loading) | ⚠️ Medium - potřebuje table komponenty |
| **S9: Reporting Dashboards** | 🔵 TODO | ~1,800 | S3 (Forms), S9 (Tables) | ⚠️ Medium - Cube.js UI needs forms + tables |
| **S10: Real-Time Widgets** | 🔵 TODO | ~1,200 | S7 (Loading), S8 (Errors) | ⚠️ Low - WebSocket updates need loading states |

**Impact:**
- ⚠️ Frontend dashboards **nelze dokončit** bez EPIC-014 S9 (Data Tables)
- ⚠️ Business metrics UI **čeká** na EPIC-014 S3 (Form filters)
- ⚠️ Real-time widgets **potřebují** EPIC-014 S7 (Loading states)

**Workaround:**
- ✅ Grafana dashboardy fungují (backend-only, bez frontend UI)
- ⚠️ React dashboardy čekají na EPIC-014 komponenty

---

### 🟡 MEDIUM DEPENDENCY (můžou pokračovat, ale UX bude horší)

#### **EPIC-005: Metamodel Generator Studio**
**Status:** 🟢 60% COMPLETE (3/5 stories), 📋 S4-S5 PLANNED

**Plánované stories (budou potřebovat EPIC-014):**

| Story | Status | EPIC-014 Dependency | Future Need |
|-------|--------|---------------------|-------------|
| **META-004: Advanced Constraints** | 📋 PLANNED | S3 (Forms), S8 (Errors) | Constraint editor UI |
| **META-005: Visual Studio UI** | 📋 PLANNED | S3 (Forms), S4 (Navigation), S9 (Tables) | Full visual editor |

**Impact:**
- ⏳ Visual editor **nemůže být implementován** bez EPIC-014 form komponent
- ⏳ Constraint builder **čeká** na EPIC-014 S3 (advanced forms)

---

#### **EPIC-002: E2E Testing Infrastructure**
**Status:** 🔵 IN PROGRESS (7/14 stories done)

**Nedokončené stories (některé budou potřebovat EPIC-014):**

| Story | Status | LOC | EPIC-014 Dependency | Impact |
|-------|--------|-----|---------------------|--------|
| **S10: Coverage Dashboard** | 🔵 TODO | ~500 | S9 (Data Tables) | Dashboard vizualizace |
| **S8: Test Registry** | 🔵 TODO | ~600 | S9 (Tables) | Test evidence tabulka |
| **S12: Testing Standards** | 🔵 TODO | ~600 | S10 (Docs) | Documentation site |

**Impact:**
- ⚠️ Test dashboardy **budou vypadat lépe** s EPIC-014 komponentami
- ✅ Testování může pokračovat i bez EPIC-014 (Playwright funguje)

---

#### **EPIC-009: AI Integration**
**Status:** 🟡 40% IN PROGRESS

**Stories v progress:**

| Story | Status | EPIC-014 Dependency | Future Need |
|-------|--------|---------------------|-------------|
| **AI-001: MCP Server** | 🟡 60% done | None (backend only) | - |
| **AI-003: Test Generation** | ⏳ PENDING | S10 (Docs) | Documentation UI |
| **AI-004: Code Review Bot** | 🔮 PLANNED | S8 (Error States) | PR comment UI |

**Impact:**
- ✅ MCP server nezávisí na EPIC-014 (backend only)
- ⏳ Test generation UI bude potřebovat EPIC-014 S10 (Docs site)

---

### 🟢 LOW/NO DEPENDENCY (můžou pokračovat nezávisle)

#### **EPIC-001: Backlog System**
**Status:** ✅ 50% COMPLETE (5/10 stories)

**Plánované stories:**
- CORE-008: Story Schema Validator (CLI nástroj, no UI)
- CORE-009: Makefile Integration (CLI, no UI)
- CORE-010: Copilot Prompt Generator (CLI, no UI)
- CORE-011: Story Quality Metrics (CLI + možná dashboard → čeká na EPIC-014 S9)
- CORE-012: Backlog Dashboard Generator (HTML gen → možná EPIC-014 S9 pro styling)

**Impact:** ✅ Většina stories jsou CLI nástroje → **nezávislé na EPIC-014**

---

#### **EPIC-008: Document Management System**
**Status:** ⏳ IN PROGRESS (0/5 stories)

**Stories:**
- S1-S5: Backend-only (MinIO, file upload API)
- **S5: Document Preview** → možná EPIC-014 S2 (Modal componenty)

**Impact:** ✅ Většina je backend → **minimální závislost na EPIC-014**

---

#### **EPIC-010: ML Platform**
**Status:** ⏳ IN PROGRESS (0/4 stories)

**Impact:** ✅ Backend-heavy (MLflow, model serving) → **nezávislé na EPIC-014**

---

#### **EPIC-011: n8n Workflow Automation**
**Status:** 📋 PLANNED

**Impact:** ✅ n8n má vlastní UI → **nezávislé na EPIC-014**

---

#### **EPIC-012: Vault Integration**
**Status:** 📋 PLANNED

**Impact:** ✅ Backend secrets management → **nezávislé na EPIC-014**

---

#### **EPIC-015: DMS Module**
**Status:** 🟡 80% COMPLETE

**Pending stories:**
- DMS-004: Metadata Search (Elasticsearch) - backend mostly
- DMS-005: Document Versioning - backend mostly

**Impact:** ✅ Backend-heavy → **nízká závislost na EPIC-014**

---

## 🎯 Prioritization Matrix: Co dokončit v jakém pořadí?

### Phase 1: UNBLOCK CRITICAL UX (EPIC-004 + EPIC-003)

**Doporučené pořadí EPIC-014 stories:**

1. **S9: Data Table Components** (~900 LOC, ~12h)
   - **Unblocks:**
     - EPIC-004 tabular reports
     - EPIC-003 S8 (Business Dashboards)
     - EPIC-002 S10 (Coverage Dashboard)
   - **Priority:** 🔴 P0 - blokuje nejvíc EPICs

2. **S3: Form Components & Validation** (~800 LOC, ~10h)
   - **Unblocks:**
     - EPIC-004 advanced filters
     - EPIC-003 S9 (Reporting Dashboards)
     - EPIC-005 META-005 (Visual Studio)
   - **Priority:** 🔴 P0 - kritické pro UX

3. **S7: Loading States & Animations** (~400 LOC, ~5h)
   - **Unblocks:**
     - EPIC-004 long query UX
     - EPIC-003 S10 (Real-time Widgets)
   - **Priority:** 🟡 P1 - zlepšuje UX výrazně

4. **S8: Error States & User Feedback** (~600 LOC, ~8h)
   - **Unblocks:**
     - EPIC-004 query error handling
     - EPIC-003 S10 (Real-time Widgets)
   - **Priority:** 🟡 P1 - nutné pro production

### Phase 2: ACCESSIBILITY & POLISH

5. **S6: Accessibility (WCAG 2.1 AA)** (~700 LOC, ~12h)
   - **Unblocks:**
     - EPIC-004 compliance
     - EPIC-003 compliance
   - **Priority:** 🔴 P0 - legal requirement!

### Phase 3: ADVANCED FEATURES

6. **S4: Navigation Patterns** (~600 LOC, ~8h)
   - **Unblocks:**
     - EPIC-005 META-005 (Visual Studio)
   - **Priority:** 🟢 P2 - nice to have

7. **S5: Responsive Design** (~500 LOC, ~7h)
   - **Unblocks:**
     - Mobile dashboards (EPIC-004, EPIC-003)
   - **Priority:** 🟢 P2 - mobile je bonus

8. **S10: Design Tokens & Documentation** (~500 LOC, ~8h)
   - **Unblocks:**
     - EPIC-002 S12 (Testing Standards docs)
     - EPIC-009 AI-003 (Test Generation UI)
   - **Priority:** 🟢 P2 - long-term consistency

---

## 📋 EPIC-014 Completion Roadmap

### Sprint 1 (Week 1-2): Data Tables + Forms (~40h)
```
✅ Complete S9: Data Table Components (900 LOC, 12h)
   - MUI DataGrid wrapper
   - Pagination, sorting, filtering
   - Virtual scrolling
   - Responsive columns
   → UNBLOCKS: EPIC-004 tabular reports, EPIC-003 S8

✅ Complete S3: Form Components (800 LOC, 10h)
   - Input, Select, Checkbox wrappers
   - Form validation (Yup integration)
   - Error display
   → UNBLOCKS: EPIC-004 filters, EPIC-003 S9

✅ Complete S7: Loading States (400 LOC, 5h)
   - Skeleton loaders
   - Progress bars
   - Spinners
   → UNBLOCKS: EPIC-004 query UX

✅ Complete S8: Error States (600 LOC, 8h)
   - Error boundaries
   - Toast notifications
   - Retry UI
   → UNBLOCKS: EPIC-004 error handling
```

### Sprint 2 (Week 3): Accessibility (~15h)
```
✅ Complete S6: Accessibility (700 LOC, 12h)
   - WCAG 2.1 AA audit
   - Keyboard navigation
   - ARIA labels
   - Screen reader testing
   → CRITICAL: Compliance requirement
```

### Sprint 3 (Week 4): Polish (~25h)
```
✅ Complete S4: Navigation Patterns (600 LOC, 8h)
✅ Complete S5: Responsive Design (500 LOC, 7h)
✅ Complete S10: Design Tokens (500 LOC, 8h)
```

**Total Effort:** ~80 hours (2 devs x 4 weeks = DONE)

---

## 🚀 Quick Wins: Co lze udělat TEĎKA?

### Okamžitě implementovatelné (bez EPIC-014 závislostí):

✅ **EPIC-001 stories** (CORE-008, 009, 010) - CLI nástroje  
✅ **EPIC-008 backend** (S1-S4) - File upload API, MinIO  
✅ **EPIC-010 backend** - MLflow integration  
✅ **EPIC-011** - n8n deployment (má vlastní UI)  
✅ **EPIC-012** - Vault integration (backend only)  
✅ **EPIC-015 DMS-004/005** - Elasticsearch + versioning (backend)

### Po dokončení EPIC-014 S9 + S3 (Data Tables + Forms):

✅ **EPIC-004 polish** - Better dashboards, advanced filters  
✅ **EPIC-003 S8-S9** - Business + Reporting Dashboards  
✅ **EPIC-005 META-005** - Visual Metamodel Studio UI  
✅ **EPIC-002 S10** - Coverage Dashboard

---

## 📊 Dependency Graph (ASCII)

```
EPIC-014 (UX/UI Design System) 20% DONE
│
├─ S1: MUI Theme ✅ DONE
│  └─ Used by: EPIC-004, EPIC-003 (all dashboards use theme)
│
├─ S2: Basic Components ✅ DONE
│  └─ Used by: EPIC-004 (Cards, Grid layout)
│
├─ S3: Form Components 🔵 TODO (~800 LOC, 10h)
│  ├─ BLOCKS: EPIC-004 (Advanced filters)
│  ├─ BLOCKS: EPIC-003 S9 (Reporting Dashboards)
│  ├─ BLOCKS: EPIC-005 META-005 (Visual Studio)
│  └─ BLOCKS: EPIC-005 META-004 (Constraint editor)
│
├─ S9: Data Tables 🔵 TODO (~900 LOC, 12h) ⚠️ CRITICAL BLOCKER
│  ├─ BLOCKS: EPIC-004 (Tabular reports)
│  ├─ BLOCKS: EPIC-003 S8 (Business Dashboards)
│  ├─ BLOCKS: EPIC-002 S10 (Coverage Dashboard)
│  ├─ BLOCKS: EPIC-002 S8 (Test Registry)
│  └─ BLOCKS: EPIC-005 META-005 (Entity browser)
│
├─ S7: Loading States 🔵 TODO (~400 LOC, 5h)
│  ├─ BLOCKS: EPIC-004 (Long query UX)
│  └─ BLOCKS: EPIC-003 S10 (Real-time Widgets)
│
├─ S8: Error States 🔵 TODO (~600 LOC, 8h)
│  ├─ BLOCKS: EPIC-004 (Query error handling)
│  └─ BLOCKS: EPIC-003 S10 (Real-time Widgets)
│
├─ S6: Accessibility 🔵 TODO (~700 LOC, 12h) ⚠️ COMPLIANCE RISK
│  ├─ BLOCKS: EPIC-004 (WCAG 2.1 AA)
│  └─ BLOCKS: EPIC-003 (WCAG 2.1 AA)
│
├─ S4: Navigation 🔵 TODO (~600 LOC, 8h)
│  └─ BLOCKS: EPIC-005 META-005 (Visual Studio navigation)
│
├─ S5: Responsive 🔵 TODO (~500 LOC, 7h)
│  └─ BLOCKS: EPIC-004, EPIC-003 (Mobile dashboards)
│
└─ S10: Design Tokens 🔵 TODO (~500 LOC, 8h)
   ├─ BLOCKS: EPIC-002 S12 (Docs site)
   └─ BLOCKS: EPIC-009 AI-003 (Test Generation UI)
```

---

## 🎯 Recommended Action Plan

### IMMEDIATE (This Week):
1. **Commit EPIC-004 README** s dependency mapping (tento soubor)
2. **Prioritize EPIC-014 S9 (Data Tables)** - unblocks 4 EPICs
3. **Start EPIC-014 S3 (Form Components)** - parallel s S9

### SHORT TERM (Next 2 Weeks):
1. Complete EPIC-014 S9 + S3 + S7 + S8
2. Unblock EPIC-004 dashboard polish
3. Unblock EPIC-003 S8-S9 (Frontend dashboards)

### MEDIUM TERM (Next Month):
1. Complete EPIC-014 S6 (Accessibility) - compliance!
2. Polish EPIC-014 S4, S5, S10
3. Implement EPIC-005 META-005 (Visual Studio UI)

### PARALLEL WORK (No blockers):
- ✅ Continue EPIC-001 CLI tools (CORE-008, 009, 010)
- ✅ Implement EPIC-008 backend (File upload API)
- ✅ Deploy EPIC-011 (n8n) + EPIC-012 (Vault)
- ✅ Finish EPIC-015 DMS-004/005 backend

---

**Last Updated:** 7. listopadu 2025  
**Maintained By:** Martin Horak  
**Review:** Quarterly (next: Q1 2026)
