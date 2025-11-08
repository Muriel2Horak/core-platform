# S11: EPIC-014 Design System Integration

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🔴 **P0 - CRITICAL**  
**Effort:** ~45 hours  
**Sprint:** FINAL (after S1-S10 complete)  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Developer / UX Designer,  
**chci** replace ALL Material-UI components v EPIC-016 s EPIC-014 Design System components,  
**abych**:
- Měl konzistentní look & feel across celou aplikaci
- Použil accessible WCAG 2.1 AA compliant components z EPIC-014
- Eliminoval duplicitní MUI dependencies (reduce bundle size)
- Využil custom theme (brand colors, typography, spacing)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Table Component Migration (S1 DataView)

**GIVEN** DataView component používá MUI DataGrid  
**WHEN** migruji na EPIC-014 Table (S9)  
**THEN**:
- Replace `<DataGrid>` → `<Table>` (from EPIC-014)
- Features preserved:
  - Virtualized scrolling (react-window)
  - Column sorting & filtering
  - Row selection
  - Pagination
- WCAG 2.1 AA compliant:
  - Keyboard navigation (arrow keys, Tab)
  - Screen reader announcements
  - Focus indicators

**Before (MUI):**

```typescript
import { DataGrid } from '@mui/x-data-grid';

<DataGrid
  columns={columns}
  rows={rows}
  pageSize={50}
  onSelectionModelChange={handleSelection}
/>
```

**After (EPIC-014):**

```typescript
import { Table } from '@/components/design-system/Table'; // EPIC-014 S9

<Table
  columns={columns}
  data={rows}
  pagination={{ pageSize: 50 }}
  onSelectionChange={handleSelection}
  virtualScroll
  accessible
/>
```

### AC2: Form Component Migration (S8 Popup Layouts)

**GIVEN** popup layouts používají MUI form fields (TextField, Select, Checkbox)  
**WHEN** migruji na EPIC-014 Form components (S3)  
**THEN**:
- Replace:
  - `<TextField>` → `<FormInput>` (EPIC-014 S3)
  - `<Select>` → `<FormSelect>`
  - `<Checkbox>` → `<FormCheckbox>`
  - `<DatePicker>` → `<FormDatePicker>`
- Form validation support (Zod schema)
- Accessible labels, error messages

**Before (MUI):**

```typescript
import { TextField, Select } from '@mui/material';

<TextField label="Email" name="email" required />
<Select label="Role">
  <MenuItem value="ADMIN">Admin</MenuItem>
</Select>
```

**After (EPIC-014):**

```typescript
import { FormInput, FormSelect } from '@/components/design-system/Form'; // EPIC-014 S3

<FormInput label="Email" name="email" required />
<FormSelect label="Role" options={[{ value: 'ADMIN', label: 'Admin' }]} />
```

### AC3: Loading States Migration (S1 DataView, S2 Filtering)

**GIVEN** components používají MUI `<CircularProgress>` nebo `<Skeleton>`  
**WHEN** migruji na EPIC-014 Loading components (S7)  
**THEN**:
- Replace:
  - `<CircularProgress>` → `<Spinner>` (EPIC-014 S7)
  - `<Skeleton variant="rectangular">` → `<SkeletonTable>`
- Skeleton shapes match final UI (table skeleton, chart skeleton)

**Before (MUI):**

```typescript
import { CircularProgress, Skeleton } from '@mui/material';

{isLoading && <CircularProgress />}
{isLoading && <Skeleton variant="rectangular" height={400} />}
```

**After (EPIC-014):**

```typescript
import { Spinner, SkeletonTable } from '@/components/design-system/Loading'; // EPIC-014 S7

{isLoading && <Spinner />}
{isLoading && <SkeletonTable rows={10} columns={5} />}
```

### AC4: Error Handling Migration (All Stories)

**GIVEN** error states používají MUI `<Alert>`  
**WHEN** migruji na EPIC-014 Error Boundary (S8)  
**THEN**:
- Replace `<Alert severity="error">` → `<ErrorMessage>` (EPIC-014 S8)
- Wrap components v `<ErrorBoundary>` pro crash recovery

**Before (MUI):**

```typescript
import { Alert } from '@mui/material';

{error && <Alert severity="error">{error.message}</Alert>}
```

**After (EPIC-014):**

```typescript
import { ErrorMessage, ErrorBoundary } from '@/components/design-system/Errors'; // EPIC-014 S8

<ErrorBoundary>
  {error && <ErrorMessage message={error.message} />}
</ErrorBoundary>
```

### AC5: Accessibility Audit (WCAG 2.1 AA)

**GIVEN** all EPIC-016 components migrated  
**WHEN** spustím accessibility audit (axe-core)  
**THEN**:
- **0 critical violations** (color contrast, keyboard nav, screen reader)
- **All interactive elements** mají proper ARIA labels
- **Focus management** works across modals, popups, grids

**Audit command:**

```bash
npm run test:a11y
# → Uses @axe-core/playwright
# → Scans all EPIC-016 pages
# → Report: accessibility-report.html
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Table Migration** (12h)

**Files to migrate:**

- `frontend/src/components/data-view/DataView.tsx` (S1)
- `frontend/src/components/query-builder/QueryPreview.tsx` (S6)

**Steps:**

1. **Install EPIC-014 Table:**

```bash
# Assume EPIC-014 exports Table component
npm install @core-platform/design-system
```

2. **Replace imports:**

```typescript
// Before
import { DataGrid } from '@mui/x-data-grid';

// After
import { Table } from '@core-platform/design-system';
```

3. **Update props mapping:**

```typescript
// MUI DataGrid props → EPIC-014 Table props
const tableProps = {
  columns: muiColumns.map(col => ({
    id: col.field,
    header: col.headerName,
    accessor: col.field,
    sortable: col.sortable !== false,
    width: col.width
  })),
  data: rows,
  pagination: {
    pageSize: 50,
    pageSizeOptions: [10, 25, 50, 100]
  },
  virtualScroll: true,
  onSelectionChange: (selectedRows) => {
    // MUI: onSelectionModelChange
    // EPIC-014: onSelectionChange
  }
};
```

4. **Test coverage:**

```typescript
// e2e/specs/data-view/table-migration.spec.ts
test('Table component works after EPIC-014 migration', async ({ page }) => {
  await page.goto('/data-view/users');

  // Verify table renders
  await expect(page.locator('[data-testid="epic014-table"]')).toBeVisible();

  // Verify sorting works
  await page.click('th:has-text("Email")');
  await expect(page.locator('tbody tr').first()).toContainText('alice@');

  // Verify pagination
  await page.click('button:has-text("Next")');
  await expect(page.locator('text=Page 2 of')).toBeVisible();
});
```

**Deliverable:** DataView uses EPIC-014 Table

---

#### **T2: Form Migration** (10h)

**Files to migrate:**

- `frontend/src/components/popup/LayoutDesigner.tsx` (S8)
- `frontend/src/components/query-builder/FilterBuilder.tsx` (S6)

**Migration script:**

```typescript
// scripts/migrate-forms.ts
import { Project } from 'ts-morph';

const project = new Project({ tsConfigFilePath: 'tsconfig.json' });

project.getSourceFiles('src/components/**/*.tsx').forEach(sourceFile => {
  // Replace MUI imports
  sourceFile.getImportDeclarations().forEach(importDecl => {
    if (importDecl.getModuleSpecifierValue() === '@mui/material') {
      const namedImports = importDecl.getNamedImports();
      
      namedImports.forEach(namedImport => {
        const name = namedImport.getName();
        if (['TextField', 'Select', 'Checkbox'].includes(name)) {
          // Remove from MUI import
          namedImport.remove();
        }
      });

      // Add EPIC-014 import
      sourceFile.addImportDeclaration({
        moduleSpecifier: '@core-platform/design-system',
        namedImports: ['FormInput', 'FormSelect', 'FormCheckbox']
      });
    }
  });

  // Replace JSX usage
  sourceFile.getDescendantsOfKind(SyntaxKind.JsxOpeningElement).forEach(element => {
    const tagName = element.getTagNameNode().getText();
    if (tagName === 'TextField') {
      element.getTagNameNode().replaceWithText('FormInput');
    }
    // ... similar for Select, Checkbox
  });

  sourceFile.saveSync();
});
```

**Deliverable:** All forms use EPIC-014 Form components

---

#### **T3: Loading States Migration** (8h)

**Global replace:**

```bash
# Find all CircularProgress usage
grep -r "CircularProgress" frontend/src/components/

# Replace with EPIC-014 Spinner
sed -i '' 's/<CircularProgress/<Spinner/g' frontend/src/components/**/*.tsx
sed -i '' "s/from '@mui\/material'/from '@core-platform\/design-system'/g" frontend/src/components/**/*.tsx
```

**Skeleton table example:**

```typescript
// Before (MUI)
{isLoading && (
  <>
    <Skeleton variant="rectangular" height={60} />
    <Skeleton variant="rectangular" height={400} sx={{ mt: 2 }} />
  </>
)}

// After (EPIC-014)
{isLoading && <SkeletonTable rows={10} columns={5} />}
```

**Deliverable:** All loading states use EPIC-014 Loading components

---

#### **T4: Error Handling Migration** (8h)

**Wrap all data-fetching components:**

```typescript
// frontend/src/components/data-view/DataView.tsx
import { ErrorBoundary, ErrorMessage } from '@core-platform/design-system';

export const DataView = ({ dataSource, ...props }) => {
  const { data, error, isLoading } = useCubeQuery({ ... });

  return (
    <ErrorBoundary fallback={<ErrorMessage message="Failed to load data" />}>
      {error && <ErrorMessage message={error.message} retry={() => refetch()} />}
      {isLoading && <SkeletonTable />}
      {data && <Table data={data} />}
    </ErrorBoundary>
  );
};
```

**Deliverable:** Error boundaries across all EPIC-016 components

---

#### **T5: Accessibility Audit** (7h)

**Setup axe-core:**

```typescript
// e2e/tests/accessibility.spec.ts
import { test, expect } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';

test.describe('EPIC-016 Accessibility Audit', () => {
  test.beforeEach(async ({ page }) => {
    await injectAxe(page);
  });

  test('DataView page has no a11y violations', async ({ page }) => {
    await page.goto('/data-view/users');
    await checkA11y(page, undefined, {
      detailedReport: true,
      detailedReportOptions: { html: true }
    });
  });

  test('Dashboard Grid has no a11y violations', async ({ page }) => {
    await page.goto('/dashboard');
    await checkA11y(page);
  });

  test('Query Builder has no a11y violations', async ({ page }) => {
    await page.goto('/query-builder');
    await checkA11y(page);
  });
});
```

**Run audit:**

```bash
npm run test:a11y
# → Generates: playwright-report/accessibility-report.html
# → Must have 0 critical violations
```

**Deliverable:** 100% WCAG 2.1 AA compliance

---

## 🧪 TESTING

### Regression Tests

```typescript
// e2e/specs/epic016/regression-after-epic014.spec.ts
test('S1: DataView table works after migration', async ({ page }) => {
  await page.goto('/data-view/users');
  await expect(page.locator('[data-component="epic014-table"]')).toBeVisible();
  await expect(page.locator('tbody tr').count()).toBeGreaterThan(0);
});

test('S2: Filtering works with EPIC-014 forms', async ({ page }) => {
  await page.goto('/data-view/users');
  await page.fill('[data-component="epic014-form-input"][name="email"]', 'alice');
  await page.click('button:has-text("Apply")');
  await expect(page.locator('tbody tr').count()).toBe(1);
});

test('S3: Dashboard grid uses EPIC-014 components', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page.locator('[data-component="epic014-grid-layout"]')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ **0 MUI dependencies** remaining in EPIC-016 code (100% EPIC-014)
- ✅ **0 accessibility violations** (axe-core audit)
- ✅ **Bundle size reduction**: -150KB (remove MUI duplicates)
- ✅ **All E2E tests pass** after migration

---

## � DEPENDENCIES

- **EPIC-014 S3:** Form Components ⏳ BLOCKER
- **EPIC-014 S6:** Accessibility Utils ⏳ BLOCKER
- **EPIC-014 S7:** Loading States ⏳ BLOCKER
- **EPIC-014 S8:** Error Boundaries ⏳ BLOCKER
- **EPIC-014 S9:** Table Component ⏳ BLOCKER

---

## � DOCUMENTATION

- [ ] Migration Guide: MUI → EPIC-014 cheat sheet
- [ ] Component Mapping Table: Which MUI component → which EPIC-014 component
- [ ] Accessibility Report: axe-core results, violations fixed

---

**Status:** 📋 TODO → **FINAL STORY** (run after S1-S10 complete)  
**Blocker:** EPIC-014 S3, S6-S9 must be DONE first (~45h of EPIC-014 work)

---

## ✅ EPIC-016 COMPLETION CHECKLIST

Po dokončení S11:

- [x] S1: Data View Engine (~850 lines)
- [x] S2: Advanced Filtering (~900 lines)
- [x] S3: Dashboard Grid (~800 lines)
- [x] S4: Role Defaults (~550 lines)
- [x] S5: Multi-Window Editing (~650 lines)
- [x] S6: Visual Query Builder (~700 lines)
- [x] S7: Extended Widgets (~1,400 lines)
- [x] S8: Customizable Popups (~950 lines)
- [x] S9: Tile Click Actions (~650 lines)
- [x] S10: Layout Sharing (~500 lines)
- [x] S11: EPIC-014 Integration (~700 lines)

**Total:** ~10,000 lines of detailed documentation  
**Total Effort:** ~740 hours (~18-19 sprints)  
**Result:** Enterprise-grade Advanced Data UX Framework 🎉

