# E2E Test Migration Plan - A11y Selectors

**Date**: 2025-01-16  
**Goal**: Migrate all E2E tests from data-testid to accessible role-based selectors  
**Status**: 🚧 IN PROGRESS

---

## 📋 Test Inventory

### ✅ Pre-Deploy Tests (DONE - 100% a11y)
- [x] `specs/pre/01_login_smoke.spec.ts` - Login & Keycloak flow
- [x] `specs/pre/02_menu_rbac_smoke.spec.ts` - Menu visibility & RBAC

### 🚧 Post-Deploy Tests (NEEDS MIGRATION)
- [ ] `specs/post/10_auth_profile_update.spec.ts` - Profile update & directory
- [ ] `specs/post/40_directory_consistency.spec.ts` - User directory consistency

### 🚧 AI Tests (NEEDS MIGRATION)
- [ ] `specs/ai/ai-help-widget.spec.ts` - AI help widget
- [ ] `specs/ai/mcp-endpoints.spec.ts` - MCP API endpoints

### 🚧 Monitoring Tests (NEEDS MIGRATION)
- [ ] `specs/monitoring/grafana-scenes-integration.spec.ts` - Grafana integration

---

## 🎯 Available Menu Items & Selectors

### User Menu (Account Menu)
**A11y Selector**: `page.getByRole('button', { name: /account menu/i })`

**Menu Items** (Czech UI):
1. **"Můj profil"** → Navigate to `/profile`
   - Selector: `page.getByRole('menuitem', { name: /můj profil/i })`
   - English: "My profile"
   
2. **"Přepnout tenant"** → (Admin only, multiple tenants)
   - Selector: `page.getByRole('menuitem', { name: /přepnout tenant/i })`
   - English: "Switch tenant"
   
3. **"Odhlásit se"** → Logout
   - Selector: `page.getByRole('menuitem', { name: /odhlásit/i })`
   - English: "Logout"

### Sidebar Navigation (defaultMenuItems)

**Level 1 - Always Visible**:
1. **Dashboard** → `/dashboard`
   - Icon: DashboardIcon
   - Selector: `page.getByRole('link', { name: /dashboard/i })`

2. **Adresář** → `/user-directory`
   - Icon: PersonIcon
   - Selector: `page.getByRole('link', { name: /adresář/i })`

**Level 1 - Analytics & Monitoring** (has children):
- **Reports** → `/reports` (Grafana dashboards)
- **Reporting Explorer** → `/reporting` (Grid analysis)
- **System Monitoring** → `/core-admin/monitoring` (Admin only)
- **Streaming Dashboard** → `/core-admin/streaming` (Admin, BETA)
- **Audit Log** → `/core-admin/audit` (Admin only)
- **Security** → `/core-admin/security` (Admin only)

**Level 1 - Správa Systému** (System Management, Admin only):
- **Uživatelé** (Users) → `/core-admin/users` (USER_MANAGER or ADMIN)
- **Role** (Roles) → `/core-admin/roles` (Admin only)
- **Skupiny** (Groups) → `/core-admin/groups` (Admin only)
- **Tenanti** (Tenants) → `/core-admin/tenants` (Admin only)
- **Keycloak Sync** → `/core-admin/keycloak-sync` (Admin only)
- **Historie Sync** (Sync History) → `/core-admin/sync-history` (Admin only)

**Level 1 - Studio & Design** (Admin only):
- **Metamodel Studio** → `/core-admin/studio` (Admin, NEW badge)
- **Workflow Designer** → (continuation...)

---

## 🔧 Required Roles

Based on `SidebarNav.tsx`:

| Role | Description | Menu Access |
|------|-------------|-------------|
| `CORE_ROLE_ADMIN` | Full admin | All menu items |
| `CORE_ROLE_USER_MANAGER` | User management | Dashboard, Directory, Users |
| (no role) | Regular user | Dashboard, Directory |

**Missing Role**: `CORE_ROLE_STUDIO` or similar for Studio access  
→ Currently Studio requires `CORE_ROLE_ADMIN`

---

## 📝 Migration Strategy

### Phase 1: User Menu & Profile Tests ✅
1. Migrate user menu click from data-testid to getByRole
2. Update "Můj profil" navigation
3. Update logout flow

### Phase 2: Directory Tests
1. Navigate to `/user-directory` (not `/directory/users`)
2. Use a11y selectors for search input
3. Verify user cards/list items

### Phase 3: Admin Menu Tests
1. Test menu visibility based on roles
2. Test navigation to admin routes
3. Verify RBAC enforcement

### Phase 4: AI & Monitoring Tests
1. Check if widgets have proper aria-labels
2. Add aria-labels if missing
3. Migrate to role-based selectors

---

## 🎨 Common Patterns

### Opening User Menu
```typescript
// ❌ OLD (data-testid)
const userMenu = page.locator('[data-testid="user-menu"]');
await userMenu.click();

// ✅ NEW (a11y role)
const userMenu = page.getByRole('button', { name: /account menu/i });
await userMenu.click();
```

### Clicking Menu Item
```typescript
// ❌ OLD (text locator)
const profileLink = page.locator('text=/profile/i');
await profileLink.click();

// ✅ NEW (a11y role with Czech/English)
const profileItem = page.getByRole('menuitem', { name: /můj profil|my profile/i });
await profileItem.click();
```

### Navigating Sidebar
```typescript
// ❌ OLD (generic link selector)
const link = page.locator('a[href="/user-directory"]');
await link.click();

// ✅ NEW (a11y role with accessible name)
const directoryLink = page.getByRole('link', { name: /adresář|directory/i });
await directoryLink.click();
```

### Search Input
```typescript
// ❌ OLD (type selector)
const search = page.locator('input[type="search"]');

// ✅ NEW (role with accessible name)
const search = page.getByRole('searchbox');
// OR if labeled:
const search = page.getByRole('textbox', { name: /search|hledat/i });
```

---

## 📊 Expected Improvements

| Metric | Before | Target | Notes |
|--------|--------|--------|-------|
| **Test reliability** | 80% | 100% | No data-testid stripping issues |
| **Test duration** | 5-10min | 2-3min | Faster selectors, optimized waits |
| **Accessibility** | 0% coverage | 100% | Tests enforce a11y attributes |
| **I18n support** | English only | CS+EN | Bilingual patterns |

---

## 🚀 Next Steps

1. **Update `specs/post/10_auth_profile_update.spec.ts`**:
   - User menu → getByRole('button')
   - Profile menu item → getByRole('menuitem')
   - Save button → getByRole('button', { name: /save|uložit/i })

2. **Update `specs/post/40_directory_consistency.spec.ts`**:
   - Search → getByRole('searchbox')
   - User cards → getByRole('article') or list items

3. **Update AI tests**:
   - Check AI widget aria-labels
   - Add if missing

4. **Update monitoring tests**:
   - Grafana iframe selectors
   - Dashboard navigation

5. **Add missing aria-labels** to components:
   - Search inputs
   - Action buttons
   - Cards/list items

---

*Generated: 2025-01-16*  
*Status: Planning phase complete, ready for implementation*
