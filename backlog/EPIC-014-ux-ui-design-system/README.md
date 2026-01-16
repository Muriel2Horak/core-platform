# EPIC-014: UX/UI & Design System

**Status:** 🟡 **20% COMPLETE** (2/10 stories done: MUI Theme, Component Library)  
**Definice:** ✅ **100%** (S3-S10 zadani rozpracovano)  
**Priority:** P0 (Foundation)  
**LOC:** ~6,200 řádků (~1,200 done + ~5,000 TODO)  
**Dependencies:** Material-UI 5.x, React 18.x

---

## 🎯 Vision

**Vytvořit konzistentní design systém** s reusable komponentami, UX guidelines a accessibility standardy, který zajistí jednotnou user experience napříč celou platformou.

### Business Goals
- **Brand Consistency**: Jednotný vzhled a cítění
- **Developer Productivity**: Rychlejší vývoj s ready-made komponentami
- **Accessibility**: WCAG 2.1 AA compliance
- **Themability**: Support pro dark/light mode + custom tenant themes
- **Scalability**: Design system škálující na 100+ screens

### Principles
```
Design Principles:
✅ Simplicity First - Minimalistický, čistý design
✅ Accessibility - WCAG 2.1 AA jako standard
✅ Consistency - Stejné patterns pro stejné akce
✅ Responsive - Mobile-first approach
✅ Performance - 60fps animations, lazy loading
```

---

## 🏗️ Architecture

### Design System Stack
```
Core Platform Design System
│
├── Foundation Layer (S1-S2) ✅ DONE
│   ├── MUI Theme (Glassmorphic)
│   │   ├── Color Palette (light/dark mode)
│   │   ├── Typography Scale
│   │   ├── Spacing System (8px grid)
│   │   └── Shadows & Elevation
│   └── Design Tokens (tokens.ts)
│
├── Component Library (S3-S5) 🔵 TODO
│   ├── Layout Components
│   │   ├── AppShell, Sidebar, Header
│   │   ├── Page Templates
│   │   └── Grid System
│   ├── Form Components
│   │   ├── Inputs, Selects, Checkboxes
│   │   ├── Form Validation
│   │   └── Error States
│   └── Data Display
│       ├── Tables, Cards, Lists
│       ├── Charts, Graphs
│       └── Modals, Dialogs
│
├── UX Patterns (S6-S7) 🔵 TODO
│   ├── Navigation Patterns
│   ├── Loading States
│   ├── Error Handling
│   └── Empty States
│
└── Documentation (S8-S9) 🔵 TODO
    ├── Component Storybook
    ├── UX Guidelines
    └── Accessibility Docs
```

---

## 📋 Stories Overview

## 📋 Stories Overview

| ID | Story | Status | LOC | Priority | Value |
|----|-------|--------|-----|----------|-------|
| [S1](#s1-mui-theme-system) | MUI Theme System | ✅ DONE | ~600 | P0 | Foundation |
| [S2](#s2-component-library-basics) | Component Library Basics | ✅ DONE | ~600 | P0 | Reusability |
| [S3](#s3-form-components--validation) | Form Components & Validation | 🔵 TODO | ~800 | P1 | Form UX |
| [S4](#s4-navigation-patterns) | Navigation Patterns & Menu | 🔵 TODO | ~600 | P1 | Orientation |
| [S5](#s5-responsive-design) | Responsive Design System | 🔵 TODO | ~500 | P2 | Mobile support |
| [S6](#s6-accessibility) | Accessibility (WCAG 2.1 AA) | 🔵 TODO | ~700 | P1 | Inclusivity |
| [S7](#s7-loading-states) | Loading States & Animations | 🔵 TODO | ~400 | P2 | Visual feedback |
| [S8](#s8-error-states) | Error States & User Feedback | 🔵 TODO | ~600 | P1 | Error handling |
| [S9](#s9-data-tables) | Data Table Components | 🔵 TODO | ~900 | P1 | Data display |
| [S10](#s10-design-tokens) | Design Tokens & Documentation | 🔵 TODO | ~500 | P2 | Consistency |
| **TOTAL** | | **2/10** | **~6,200** | | **Complete design system** |

---

## 🔍 GAP analýza (Current vs Target)

| Oblast | Story | Gap / Riziko |
| --- | --- | --- |
| Form components | S3 | Chybí jednotné form komponenty + validace → nekonzistentní UX |
| Navigation patterns | S4 | Chybí layouty a menu patterny |
| Responsive design | S5 | Chybí jasná responsive pravidla napříč aplikací |
| Accessibility | S6 | Neřešené WCAG 2.1 AA → compliance/UX riziko |
| Loading states | S7 | Chybí loading/skeleton patterny |
| Error states | S8 | Chybí standardní error UI + recovery flow |
| Data tables | S9 | Chybí standardní tabulky + data display |
| Tokens & docs | S10 | Chybí dokumentace tokenů a usage guide |

## 🧩 DEV tasky (PENDING) - popis a scope

| DEV task | Popis (high-level) | Výstup |
| --- | --- | --- |
| [S3: Form Components & Validation](#s3-form-components--validation) | Inputs, selects, validation patterns | Konzistentní form UX |
| [S4: Navigation Patterns](#s4-navigation-patterns) | App shell, menu, breadcrumbs | Jednotná navigace |
| [S5: Responsive Design](#s5-responsive-design) | Breakpoints + responsive layouty | Mobilní podpora |
| [S6: Accessibility](#s6-accessibility) | WCAG 2.1 AA standardy + audit | Přístupnost |
| [S7: Loading States](#s7-loading-states) | Skeletony, spinnery, transitions | Vizualní feedback |
| [S8: Error States](#s8-error-states) | Error patterns + recovery flows | Chybová UX |
| [S9: Data Tables](#s9-data-tables) | Table komponenty + paging | Data display |
| [S10: Design Tokens](#s10-design-tokens) | Token docs + guidelines | Dokumentace DS |

## 📖 Detailed Stories

### S1: MUI Theme Foundation

**Status:** ✅ **DONE**  
**Implementation:** Implementováno v `/frontend/src/shared/theme/theme.ts`  
**LOC:** ~500

#### Description
Material-UI theme s glassmorphic designem, dark/light mode support a custom color palette.

#### Key Features
- **Glassmorphic Design**: Semi-transparent backgrounds s backdrop-filter
- **Dark/Light Mode**: Automatická detekce systémového módu
- **Color Palette**: Primary (blue), Secondary (grey), custom colors
- **Typography Scale**: H1-H6, body, caption s optimálními sizes
- **Spacing System**: 8px grid (spacing(1) = 8px)

#### Implementation
```typescript
// frontend/src/shared/theme/theme.ts
export const corePlatformTheme = createTheme({
  palette: {
    mode: prefersDarkMode ? 'dark' : 'light',
    primary: { main: '#1976d2' },
    secondary: { main: '#455a64' },
    background: {
      default: '#f5f5f7',
      paper: 'rgba(255, 255, 255, 0.8)',  // Glassmorphic!
    }
  },
  typography: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto',
    h1: { fontSize: '2.5rem', fontWeight: 600 },
    // ... rest
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          textTransform: 'none',
          boxShadow: 'none',
        }
      }
    }
  }
});
```

#### Value
- **Consistent Branding**: Jednotný vzhled napříč platformou
- **Themability**: Snadné přepínání dark/light mode
- **Developer DX**: MUI theme = auto-styling všech MUI komponent

---

### S2: Design Tokens System

**Status:** ✅ **DONE**  
**Implementation:** `/frontend/src/shared/theme/tokens.ts`  
**LOC:** ~300

#### Description
Centralizované design tokens pro colors, spacing, borders, shadows a breakpoints.

#### Tokens
```typescript
// frontend/src/shared/theme/tokens.ts
export const tokens = {
  // Colors
  colors: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    grey: {
      50: '#fafafa',
      100: '#f5f5f5',
      // ... 900
    }
  },
  
  // Spacing (8px grid)
  spacing: {
    xs: 4,    // 0.5 * 8px
    sm: 8,    // 1 * 8px
    md: 16,   // 2 * 8px
    lg: 24,   // 3 * 8px
    xl: 32,   // 4 * 8px
  },
  
  // Borders
  borderRadius: {
    sm: 4,
    md: 8,
    lg: 12,
    xl: 16,
  },
  
  // Shadows
  shadows: {
    sm: '0 1px 2px rgba(0,0,0,0.05)',
    md: '0 4px 6px rgba(0,0,0,0.07)',
    lg: '0 10px 15px rgba(0,0,0,0.1)',
  },
  
  // Breakpoints
  breakpoints: {
    xs: 0,
    sm: 600,
    md: 960,
    lg: 1280,
    xl: 1920,
  }
};
```

#### Value
- **Single Source of Truth**: Jeden místo pro všechny design constants
- **Easy Updates**: Změna tokenu = změna napříč celou app
- **Type Safety**: TypeScript autocomplete pro tokeny

---

### S3: Component Library (Reusable Components)

**Status:** 🔵 **TODO**  
**Priority:** P0  
**LOC:** ~2,000  
**Effort:** ~16 hodin

#### Description
Knihovna reusable React komponent postavená na MUI s custom styling a business logic.

#### Components to Build

##### Layout Components
- **AppShell** - Hlavní layout wrapper (sidebar + header + content)
- **Sidebar** - Navigation sidebar s collapsible menu
- **Header** - Top bar s user menu, notifications, search
- **PageHeader** - Page title + breadcrumbs + actions
- **ContentArea** - Main content wrapper s padding
- **Footer** - Bottom footer

##### Navigation Components
- **NavItem** - Single navigation item
- **NavGroup** - Navigation group (collapsible)
- **Breadcrumbs** - Breadcrumb trail
- **Tabs** - Custom tabs s routing support

##### Feedback Components
- **Toast** - Notification toast (success, error, info, warning)
- **Alert** - Alert banners
- **LoadingSpinner** - Loading indicator
- **EmptyState** - Empty state placeholder
- **ErrorBoundary** - Error fallback UI

#### Example Usage
```tsx
// AppShell usage
<AppShell
  sidebar={<Sidebar items={navItems} />}
  header={<Header user={currentUser} />}
>
  <PageHeader 
    title="User Management"
    breadcrumbs={['Admin', 'Users']}
    actions={<Button>Add User</Button>}
  />
  <ContentArea>
    {/* Page content */}
  </ContentArea>
</AppShell>
```

#### Value
- **Faster Development**: No need to rebuild common components
- **Consistency**: Same components = same UX
- **Maintainability**: Fix once, fixes everywhere

---

### S4: Form Components & Validation

**Status:** 🔵 **TODO**  
**Priority:** P1  
**LOC:** ~1,200  
**Effort:** ~12 hodin

#### Components
- **FormField** - Wrapper s label, error, helper text
- **TextInput** - Text input s validation
- **Select** - Dropdown select (single/multi)
- **Checkbox** - Checkbox s label
- **Radio** - Radio button group
- **DatePicker** - Date picker (Material-UI X)
- **FileUpload** - File upload s drag-n-drop
- **RichTextEditor** - WYSIWYG editor (TipTap?)

#### Validation
```tsx
<Form
  onSubmit={handleSubmit}
  validationSchema={yup.object({
    email: yup.string().email().required(),
    password: yup.string().min(8).required(),
  })}
>
  <FormField name="email" label="Email">
    <TextInput type="email" />
  </FormField>
  
  <FormField name="password" label="Password">
    <TextInput type="password" />
  </FormField>
  
  <Button type="submit">Submit</Button>
</Form>
```

---

### S5: Data Display Components

**Status:** 🔵 **TODO**  
**Priority:** P1  
**LOC:** ~1,000  
**Effort:** ~10 hodin

#### Components
- **DataTable** - Sortable, filterable table
- **Card** - Card s header, body, footer
- **List** - List s items (virtualized for large lists)
- **Timeline** - Vertical timeline
- **Stat Card** - Metric card (value, trend, sparkline)
- **Progress** - Progress bar/circle
- **Badge** - Badge/chip

---

### S6: UX Patterns & Guidelines

**Status:** 🔵 **TODO**  
**Priority:** P2  
**LOC:** ~800  
**Effort:** ~8 hodin

#### Patterns
- **Navigation** - Sidebar, breadcrumbs, tabs patterns
- **Loading States** - Skeletons, spinners, progressive loading
- **Error Handling** - Error pages (404, 500), inline errors
- **Empty States** - No data placeholders
- **Confirmation** - Modals, dialogs for destructive actions

---

### S7: Accessibility (WCAG 2.1 AA)

**Status:** 🔵 **TODO**  
**Priority:** P1  
**LOC:** ~600  
**Effort:** ~6 hodin

#### Focus
- **Keyboard Navigation** - Tab order, focus indicators
- **Screen Reader Support** - ARIA labels, roles
- **Color Contrast** - WCAG AA minimum (4.5:1)
- **Focus Management** - Focus trapping v modals
- **Alt Text** - Images, icons

---

### S8: Storybook Documentation

**Status:** 🔵 **TODO**  
**Priority:** P2  
**LOC:** ~1,000  
**Effort:** ~10 hodin

#### Setup
- Storybook 7.x
- Stories pro všechny komponenty
- Interactive playground
- Design tokens docs

---

### S9: UX Writing Guidelines

**Status:** 🔵 **TODO**  
**Priority:** P2  
**LOC:** ~400  
**Effort:** ~4 hodin

#### Guidelines
- Button labels (Clear vs. Cancel)
- Error messages (friendly, actionable)
- Empty states (helpful, not boring)
- Confirmation dialogs (explain consequences)

---

## 🎨 Design System Examples

### Color Palette
```
Primary (Blue):
  main:  #1976d2
  light: #42a5f5
  dark:  #1565c0

Secondary (Grey):
  main:  #455a64
  light: #78909c
  dark:  #37474f

Semantic:
  success: #4caf50
  error:   #f44336
  warning: #ff9800
  info:    #2196f3
```

### Typography Scale
```
H1: 2.5rem (40px) / 600 weight
H2: 2rem (32px) / 600 weight
H3: 1.75rem (28px) / 600 weight
H4: 1.5rem (24px) / 600 weight
H5: 1.25rem (20px) / 600 weight
H6: 1rem (16px) / 600 weight
Body: 1rem (16px) / 400 weight
Caption: 0.875rem (14px) / 400 weight
```

### Spacing System (8px Grid)
```
xs: 4px   (0.5 * 8)
sm: 8px   (1 * 8)
md: 16px  (2 * 8)
lg: 24px  (3 * 8)
xl: 32px  (4 * 8)
xxl: 48px (6 * 8)
```

---

## 📊 Implementation Status

### Completed (S1-S2)
- ✅ MUI Theme s glassmorphic design
- ✅ Dark/light mode support
- ✅ Design tokens system
- ✅ Typography scale
- ✅ Color palette

### TODO (S3-S9)
- 🔵 Component library (~40 components)
- 🔵 Form components + validation
- 🔵 Data display components
- 🔵 UX patterns
- 🔵 Accessibility compliance
- 🔵 Storybook docs
- 🔵 UX writing guidelines

---

## 🎯 Success Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Component Coverage | 100% | 40% | 🟡 In Progress |
| WCAG 2.1 AA Compliance | 100% | TBD | 🔵 TODO |
| Storybook Stories | 100% | 0% | 🔵 TODO |
| Design System Usage | 90%+ | 60% | 🟡 Partial |
| Developer Satisfaction | 8/10 | TBD | 🔵 TODO |

---

**Next Steps:**
1. Complete S3 (Component Library) - P0 priority
2. Implement S4 (Form Components) - Critical for CRUD
3. Add S8 (Storybook) - Documentation gap
4. A11y audit (S7) - Compliance requirement

**Related EPICs:**
- EPIC-001: Backlog System (uses design system)
- EPIC-003: Monitoring (dashboards use components)
- All future features depend on this foundation
