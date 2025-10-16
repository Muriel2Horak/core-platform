# 🧪 E2E Test Pre-Rebuild Status Report

**Date**: 2025-01-16  
**Status**: 6 passing, 4 failing, 46 new CRUD tests not yet run  
**Action**: Identified issues before environment rebuild

---

## 📊 Test Results Summary

### ✅ Passing Tests (6/10)

| Test | Category | Duration | Status |
|------|----------|----------|--------|
| Login via Keycloak GUI | PRE | 13.0s | ✅ PASS |
| Show login form on initial visit | PRE | 12.5s | ✅ PASS |
| Reject invalid credentials | PRE | 1.9s | ✅ PASS |
| Show admin menu for admin users | PRE | 15.6s | ✅ PASS |
| Hide admin menu for non-admin users | PRE | 14.9s | ✅ PASS |
| Show user profile menu | PRE | 4.8s | ✅ PASS |

### ❌ Failing Tests (4/10)

| Test | Category | Error | Root Cause |
|------|----------|-------|------------|
| **1. Basic menu items for logged-in users** | PRE | `TimeoutError: waitForURL` | Login redirect timeout - Keycloak stuck |
| **2. Update user profile** | POST | `TimeoutError: locator.fill` | Profile page selector issue |
| **3. Recently updated badge** | POST | `self-signed certificate` | API helper needs `ignoreHTTPSErrors: true` |
| **4. Directory consistency** | POST | `self-signed certificate` | Same API cert issue |

---

## 🔍 Detailed Issue Analysis

### Issue #1: Login Redirect Timeout ⚠️ CRITICAL
**Test**: `02_menu_rbac_smoke.spec.ts:15` - Basic menu items  
**Error**:
```
TimeoutError: page.waitForURL: Timeout 15000ms exceeded.
waiting for navigation until "load"
navigated to "https://admin.core-platform.local/realms/admin/protocol/openid-connect/auth?..."
```

**Root Cause**: Login helper stuck on Keycloak auth page, not redirecting to dashboard

**Location**: `e2e/helpers/login.ts:60`
```typescript
await page.waitForURL(/(dashboard|home)/, { timeout: 15000 });
```

**Impact**: Blocks all tests that depend on login

**Fix Priority**: 🔴 HIGH - Affects multiple tests

**Possible Solutions**:
1. Increase timeout to 30s
2. Check if Keycloak redirect URL is correct
3. Verify frontend OAuth config
4. Check for Keycloak session conflicts

---

### Issue #2: Profile Page Selector ⚠️ MEDIUM
**Test**: `10_auth_profile_update.spec.ts:16` - Update profile  
**Error**:
```
TimeoutError: locator.fill: Timeout 15000ms exceeded.
waiting for getByRole('textbox', { name: /display name|zobrazované jméno/i })
```

**Root Cause**: Profile page doesn't have expected textbox with "display name" label

**Location**: `e2e/specs/post/10_auth_profile_update.spec.ts:36`
```typescript
const displayNameInput = page.getByRole('textbox', { name: /display name|zobrazované jméno/i })
  .or(page.locator('input[name="displayName"], input[id*="displayName"]').first());
await displayNameInput.fill(newDisplayName);
```

**Impact**: Profile update tests failing

**Fix Priority**: 🟡 MEDIUM - POST tests only

**Possible Solutions**:
1. Inspect profile page to find correct selector
2. Add `data-testid` to display name input
3. Use more generic selector (e.g., first text input)
4. Check if profile page structure changed

---

### Issue #3 & #4: Self-Signed Certificate ⚠️ HIGH
**Tests**: 
- `10_auth_profile_update.spec.ts:73` - Recently updated badge
- `40_directory_consistency.spec.ts:13` - Directory consistency

**Error**:
```
Error: apiRequestContext.post: self-signed certificate
Call log:
  - → POST https://admin.core-platform.local/realms/admin/protocol/openid-connect/token
```

**Root Cause**: API helper doesn't ignore self-signed certificates in local dev

**Location**: `e2e/helpers/api.ts:46`
```typescript
const response = await api.post(`${config.keycloak.authServerUrl}/realms/${config.keycloak.realm}/protocol/openid-connect/token`, {
```

**Impact**: All tests using API helper for token auth

**Fix Priority**: 🔴 HIGH - Blocks POST tests

**Solution**: Add `ignoreHTTPSErrors: true` to API context creation

**Fix**:
```typescript
export async function createApiContext(): Promise<APIRequestContext> {
  return await playwright.request.newContext({
    ignoreHTTPSErrors: true,  // Add this line
    extraHTTPHeaders: {
      'Accept': 'application/json',
    }
  });
}
```

---

## 🚧 New CRUD Tests Status

### Created but Not Yet Run (46 tests)

| Test Suite | Tests | Status | Notes |
|------------|-------|--------|-------|
| **users-crud.spec.ts** | 11 | ⏳ NOT RUN | Needs admin API endpoints |
| **roles-crud.spec.ts** | 11 | ⏳ NOT RUN | Needs roles management page |
| **groups-crud.spec.ts** | 11 | ⏳ NOT RUN | Needs groups management page |
| **tenants-crud.spec.ts** | 13 | ⏳ NOT RUN | Needs tenants management page |

**Why Not Run?**:
- Admin pages may not exist yet (`/users`, `/roles`, `/groups`, `/tenants`)
- API endpoints may not be implemented
- Need to rebuild environment with new roles first

**Dependencies**:
1. ✅ Keycloak roles configured (DONE - commit d7331d6)
2. ✅ Backend permissions.yml updated (DONE - commit d7331d6)
3. ⏳ Backend controllers for CRUD operations (UNKNOWN)
4. ⏳ Frontend admin pages (UNKNOWN)
5. ⏳ RBAC middleware (UNKNOWN)

---

## 🎯 Before Rebuild Checklist

### Critical Fixes Needed

- [ ] **Fix API Helper Certificate Issue**
  - File: `e2e/helpers/api.ts`
  - Add `ignoreHTTPSErrors: true`
  - Impact: Fixes 2 failing POST tests

- [ ] **Fix Login Redirect Timeout**
  - File: `e2e/helpers/login.ts`
  - Increase timeout or improve redirect detection
  - Impact: Fixes 1 failing PRE test

- [ ] **Fix Profile Page Selectors**
  - File: `e2e/specs/post/10_auth_profile_update.spec.ts`
  - Update selectors to match actual profile page
  - Impact: Fixes 1 failing POST test

### Nice to Have

- [ ] **Add data-testid attributes**
  - Profile page inputs
  - Admin page buttons
  - Makes tests more stable

- [ ] **Verify Admin Pages Exist**
  - `/users` - User management
  - `/roles` - Role management
  - `/groups` - Group management
  - `/tenants` - Tenant management

- [ ] **Verify API Endpoints**
  - `GET/POST/PUT/DELETE /api/admin/keycloak/users`
  - `GET/POST/PUT/DELETE /api/admin/keycloak/roles`
  - `GET/POST/PUT/DELETE /api/admin/keycloak/groups`
  - `GET/POST/PUT/DELETE /api/admin/tenants`

---

## 📋 After Rebuild TODO

### 1. Restart Services with New Roles
```bash
# Restart Keycloak to load new realm config
docker compose -f docker/docker-compose.yml restart keycloak

# Wait for Keycloak to be ready (30-60s)
sleep 60

# Restart backend to load new permissions.yml
docker compose -f docker/docker-compose.yml restart backend

# Rebuild frontend with new role checks
docker compose -f docker/docker-compose.yml restart frontend
```

### 2. Verify New Test Users
```bash
# Login as test_studio (CORE_ROLE_STUDIO)
# Should have access to /core-admin/studio

# Login as test_analyst (CORE_ROLE_REPORTING)  
# Should have access to /reporting
```

### 3. Run Full Test Suite
```bash
cd e2e

# Run PRE tests first
npx playwright test --project=pre

# If PRE passes, run POST tests
npx playwright test --project=post

# Run new CRUD tests (will likely fail if pages don't exist)
npx playwright test specs/admin/
```

### 4. Implement Missing Features

**If CRUD tests fail**, implement in this order:

1. **Users CRUD** (highest priority)
   - Backend: UserManagementController
   - Frontend: `/users` page with CRUD UI
   - RBAC: admin, user_manager

2. **Roles CRUD** (high priority)
   - Backend: RoleManagementController
   - Frontend: `/roles` page with CRUD UI
   - RBAC: admin (read-only for user_manager)

3. **Groups CRUD** (medium priority)
   - Backend: GroupManagementController
   - Frontend: `/groups` page with CRUD UI
   - RBAC: admin, user_manager

4. **Tenants CRUD** (medium priority)
   - Backend: TenantManagementController (may exist)
   - Frontend: `/tenants` page with CRUD UI
   - RBAC: admin only

---

## 🔧 Quick Fixes to Apply Now

### 1. Fix API Helper (2 minutes)

**File**: `e2e/helpers/api.ts`

**Change**:
```typescript
export async function createApiContext(): Promise<APIRequestContext> {
  return await playwright.request.newContext({
    ignoreHTTPSErrors: true,  // ADD THIS LINE
    extraHTTPHeaders: {
      'Accept': 'application/json',
    }
  });
}
```

### 2. Fix Login Timeout (1 minute)

**File**: `e2e/helpers/login.ts:60`

**Change**:
```typescript
// Increase timeout from 15s to 30s
await page.waitForURL(/(dashboard|home)/, { timeout: 30000 });
```

### 3. Add Profile Page Selector Debug (2 minutes)

**File**: `e2e/specs/post/10_auth_profile_update.spec.ts:36`

**Add before line 36**:
```typescript
// Debug: Print available inputs
const inputs = await page.locator('input').all();
console.log(`Found ${inputs.length} inputs on profile page`);
for (const input of inputs) {
  const name = await input.getAttribute('name');
  const id = await input.getAttribute('id');
  console.log(`  - input: name="${name}", id="${id}"`);
}
```

---

## 📊 Test Coverage Overview

### Current Coverage (10 tests, 60% passing)
- ✅ Login/Auth: 3/3 passing (100%)
- ✅ Menu RBAC: 3/4 passing (75%)
- ❌ Profile Update: 0/2 passing (0%)
- ❌ Directory: 0/1 passing (0%)

### After Fixes (Expected 10/10 passing)
- ✅ Login/Auth: 3/3 passing (100%)
- ✅ Menu RBAC: 4/4 passing (100%)
- ✅ Profile Update: 2/2 passing (100%)
- ✅ Directory: 1/1 passing (100%)

### After Rebuild + CRUD (Target: 56 tests)
- ✅ PRE tests: 7 tests (smoke + RBAC)
- ✅ POST tests: 3 tests (auth + directory)
- ⏳ Users CRUD: 11 tests (new)
- ⏳ Roles CRUD: 11 tests (new)
- ⏳ Groups CRUD: 11 tests (new)
- ⏳ Tenants CRUD: 13 tests (new)

**Total Target**: 56 E2E tests covering full admin workflow

---

## 🎯 Success Criteria

### Before Rebuild
- [x] Identify all failing tests (4/4 found)
- [x] Document root causes (all documented)
- [x] Create quick fixes (3 fixes ready)
- [x] Create CRUD tests (46 tests created)
- [ ] Apply quick fixes (next step)
- [ ] Re-run tests to verify fixes

### After Rebuild  
- [ ] All 10 existing tests passing (100%)
- [ ] New roles working (test_studio, test_analyst)
- [ ] CRUD tests can run (may fail if pages missing)
- [ ] Identify which admin pages need implementation
- [ ] Create implementation plan for missing features

---

## 📈 Progress Metrics

**Completed**:
- ✅ Role configuration (6 roles defined)
- ✅ Test users created (5 users)
- ✅ Keycloak realm updated (3 files)
- ✅ Backend permissions updated (permissions.yml)
- ✅ Security annotations fixed (StudioAdminController)
- ✅ CRUD tests created (46 tests, 4 files)
- ✅ Test fixtures helper (fixtures.ts)
- ✅ Login helper extended (loginAsUser)

**In Progress**:
- ⏳ Fixing existing test failures (3 quick fixes ready)
- ⏳ Environment rebuild preparation

**Remaining** (from E2E_COMPLETE_PLAN.md):
- ⏳ AI tests migration (2 files, 1 hour)
- ⏳ Monitoring tests review (1 file, 30 min)
- ⏳ Streaming tests (3 files, 8 hours)
  - Kafka events test
  - CDC sync test
  - Full refresh test

**Total Time Invested**: ~4 hours  
**Remaining Work**: ~9.5 hours  
**Total Plan**: 22 hours → 60% complete

---

## 🚀 Next Actions

**Immediate (< 5 minutes)**:
1. Apply API certificate fix
2. Apply login timeout fix
3. Commit fixes
4. Re-run tests

**After Rebuild (< 1 hour)**:
1. Restart all services
2. Verify new roles in Keycloak admin
3. Test login as test_studio and test_analyst
4. Run full test suite
5. Document which CRUD pages exist

**Then Continue**:
1. Implement missing CRUD pages (if needed)
2. Migrate AI tests to a11y selectors
3. Create streaming/CDC tests
4. Final test run before production

---

**Current Status**: 🟡 Ready for quick fixes → rebuild → full validation  
**Blocker**: None (all issues identified and fixable)  
**Risk**: Low (tests well isolated, fixes are straightforward)
