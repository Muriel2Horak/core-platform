# 👥 E2E Test Users Documentation

**Last Updated:** 16. října 2025

---

## 🎯 Overview

Tento dokument popisuje test users používané v E2E testech, jejich credentials, role a permissions.

---

## 📋 Test Users

### 1. `test` - Regular User (Basic Access)

**Credentials:**
```
Username: test
Password: Test.1234
```

**Roles:**
- `CORE_ROLE_USER` - Basic user role
- Možná další tenant-specific roles

**Permissions:**
- ✅ Access to Dashboard
- ✅ Access to User Directory (public)
- ✅ View own profile
- ✅ Access to Reports (Grafana Scenes)
- ✅ Access to Reporting Explorer (ag-grid)
- ❌ NO admin access
- ❌ NO tenant management
- ❌ NO user management

**Use Cases:**
- Testing basic user flows
- Testing RBAC (verify admin menu is hidden)
- Testing public features accessibility

**E2E Test Usage:**
```typescript
// Login as regular user
await login(page, {
  username: 'test',
  password: 'Test.1234',
});
```

---

### 2. `test_admin` - Admin User (Full Access)

**Credentials:**
```
Username: test_admin
Password: Test.1234
```

**Roles (Expected):**
- `CORE_ROLE_ADMIN` - Full admin role
- `CORE_ROLE_USER_MANAGER` - User management
- `CORE_ROLE_TENANT_ADMIN` - Tenant management
- ⚠️ **UNKNOWN**: Studio/Workflow designer roles
- ⚠️ **TO VERIFY**: Grafana admin role

**Permissions:**
- ✅ Full access to Core Admin section (`/core-admin/*`)
  - Users management
  - Roles management
  - Tenants management
  - Groups management
  - Security settings
  - Audit logs
  - Monitoring
  - Keycloak sync
  - Streaming dashboard
- ✅ Access to all user features (Dashboard, Reports, etc.)
- ⚠️ **UNKNOWN**: Metamodel Studio (`/core-admin/studio`)
- ⚠️ **UNKNOWN**: Workflow Designer (`/core-admin/workflows`)

**Use Cases:**
- Testing admin functionality
- Testing RBAC (verify admin menu is visible)
- Testing user/role/tenant CRUD operations
- Testing Keycloak sync
- Testing monitoring features

**E2E Test Usage:**
```typescript
// Login as admin
await login(page, {
  username: 'test_admin',
  password: 'Test.1234',
});
```

**⚠️ Known Issues:**
User identified: "všiml jsem si, že nám chybí založené a přidělení role pro Studia pro test_admin"
- Need to verify Studio access
- Need to verify Workflow designer access
- May need additional role assignments

---

### 3. `test_tenant_admin` - Tenant Admin (Tenant-Scoped)

**Status:** ❌ **NOT YET CREATED**

**Planned Credentials:**
```
Username: test_tenant_admin
Password: Test.1234
```

**Planned Roles:**
- `TENANT_ROLE_ADMIN` - Tenant admin role
- Scoped to specific tenant

**Planned Permissions:**
- ✅ Access to Tenant Admin section (`/tenant-admin/*`)
  - Tenant-scoped users
  - Tenant-scoped roles
  - Tenant-scoped groups
  - Tenant Keycloak sync
- ✅ Access to Dashboard (tenant-scoped data)
- ❌ NO Core Admin access
- ❌ NO cross-tenant access

**Use Cases:**
- Testing tenant admin functionality
- Testing tenant data isolation
- Testing tenant-scoped RBAC

**TODO:**
- [ ] Create `test_tenant_admin` user in Keycloak
- [ ] Assign appropriate tenant admin role
- [ ] Add to test data setup script
- [ ] Document tenant assignment

---

## 🔐 Password Policy

All test users use the same password pattern for simplicity:
```
Password: Test.1234
```

**Password requirements:**
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (`.`)

---

## 🌍 Environment Variables

Test user credentials can be overridden via environment variables:

### Regular User:
```bash
export E2E_USER="test"           # Default: test
export E2E_PASS="Test.1234"      # Default: Test.1234
```

### Admin User:
```bash
export E2E_ADMIN_USER="test_admin"  # Default: test_admin
export E2E_ADMIN_PASS="Test.1234"   # Default: Test.1234
```

### Usage in tests:
```typescript
// e2e/config/read-config.ts
const username = process.env.E2E_USER || 'test';
const password = process.env.E2E_PASS || 'Test.1234';

// e2e/helpers/api.ts
export async function getAdminAuthToken() {
  return getAuthToken(
    process.env.E2E_ADMIN_USER || 'test_admin',
    process.env.E2E_ADMIN_PASS || 'Test.1234'
  );
}
```

---

## 📊 Role Matrix

| Feature / Route | `test` (User) | `test_admin` (Admin) | `test_tenant_admin` (Tenant) |
|---|---|---|---|
| `/dashboard` | ✅ | ✅ | ✅ |
| `/user-directory` | ✅ | ✅ | ✅ |
| `/reports` (Grafana) | ✅ | ✅ | ✅ |
| `/reporting` (Explorer) | ✅ | ✅ | ✅ |
| `/profile` | ✅ | ✅ | ✅ |
| **Core Admin** | | | |
| `/core-admin/monitoring` | ❌ | ✅ | ❌ |
| `/core-admin/users` | ❌ | ✅ | ❌ |
| `/core-admin/roles` | ❌ | ✅ | ❌ |
| `/core-admin/tenants` | ❌ | ✅ | ❌ |
| `/core-admin/groups` | ❌ | ✅ | ❌ |
| `/core-admin/security` | ❌ | ✅ | ❌ |
| `/core-admin/audit` | ❌ | ✅ | ❌ |
| `/core-admin/keycloak-sync` | ❌ | ✅ | ❌ |
| `/core-admin/streaming` | ❌ | ✅ | ❌ |
| `/core-admin/workflows` | ❌ | ⚠️ Unknown | ❌ |
| `/core-admin/studio` | ❌ | ⚠️ Unknown | ❌ |
| **Tenant Admin** | | | |
| `/tenant-admin/` | ❌ | ❌ | ✅ |
| `/tenant-admin/users` | ❌ | ❌ | ✅ |
| `/tenant-admin/roles` | ❌ | ❌ | ✅ |
| `/tenant-admin/groups` | ❌ | ❌ | ✅ |
| `/tenant-admin/keycloak-sync` | ❌ | ❌ | ✅ |

---

## 🔍 Verification Steps

### Check user roles via API:
```bash
# Get auth token
TOKEN=$(curl -s -X POST https://admin.core-platform.local/realms/admin/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web" \
  -d "username=test_admin" \
  -d "password=Test.1234" \
  -d "grant_type=password" \
  | jq -r '.access_token')

# Get current user info
curl -H "Authorization: Bearer $TOKEN" \
  https://admin.core-platform.local/api/users/me | jq '.roles'

# List all roles
curl -H "Authorization: Bearer $TOKEN" \
  https://admin.core-platform.local/api/admin/roles | jq '.[].name'
```

### Check in browser DevTools:
```javascript
// After login, check user object
console.log(user.roles);

// Check JWT token claims
const token = localStorage.getItem('token') || sessionStorage.getItem('token');
if (token) {
  const payload = JSON.parse(atob(token.split('.')[1]));
  console.log('Token claims:', payload);
  console.log('Roles:', payload.realm_access?.roles);
}
```

---

## ✅ Action Items

### Immediate (blokující E2E testy):
- [ ] **Verify `test_admin` has Studio role** - potřeba pro `/core-admin/studio` testy
- [ ] **Verify `test_admin` has Workflow designer role** - potřeba pro workflow testy
- [ ] **Document actual role names** - najít přesné názvy rolí v Keycloak

### Soon (pro kompletnější testy):
- [ ] **Create `test_tenant_admin` user** - potřeba pro tenant admin testy
- [ ] **Create test data setup script** - automaticky vytvořit test users
- [ ] **Add role assignment verification** - test který ověří že users mají správné role

### Later (nice to have):
- [ ] **Create additional test users** - různé kombinace rolí
- [ ] **Document feature flags** - které features vyžadují které role
- [ ] **Add RBAC test matrix** - automatizovaná verifikace všech kombinací

---

## 🎯 Test Data Setup Script (TODO)

Ideální by byl script který vytvoří všechny test users:

```bash
#!/bin/bash
# scripts/e2e/setup-test-users.sh

# Create test users in Keycloak
# Assign appropriate roles
# Verify setup

# Usage:
# ./scripts/e2e/setup-test-users.sh
```

**Features:**
- Create users if they don't exist
- Assign roles based on configuration
- Verify user can login
- Idempotent (can run multiple times)
- Output verification report

---

## 📝 Notes

- All test users are in Keycloak realm `admin`
- Client ID is `web` for frontend authentication
- Backend uses different client (`core-backend`)
- Test users persist across E2E test runs
- E2E tests should NOT delete test users (only test data they create)

---

## 🔗 Related Documentation

- [E2E Tests Comprehensive Revision](../E2E_COMPREHENSIVE_REVISION.md)
- [E2E Quick Fixes Applied](../E2E_QUICK_FIXES_APPLIED.md)
- [E2E Wave 2 Fixes Applied](../E2E_WAVE2_FIXES_APPLIED.md)
- [Testing FAQ](../TESTING_FAQ.md)

