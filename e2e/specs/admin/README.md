# Admin CRUD E2E Tests

> **55 kompletních E2E testů pro admin funkcionality**  
> Pokrytí: Users, Roles, Groups, Tenants, Keycloak Sync

---

## 📊 Přehled testů

| Test Suite | Počet | Soubor | Popis |
|------------|-------|--------|-------|
| **Users CRUD** | 10 | `users-crud.spec.ts` | Správa uživatelů + role assignment |
| **Roles CRUD** | 11 | `roles-crud.spec.ts` | Správa rolí + permissions |
| **Groups CRUD** | 11 | `groups-crud.spec.ts` | Správa skupin + members |
| **Tenants CRUD** | 13 | `tenants-crud.spec.ts` | Správa tenantů + Grafana provisioning |
| **Keycloak Sync** | 10 | `keycloak-sync.spec.ts` | Bulk sync z Keycloaku |
| **CELKEM** | **55** | - | - |

---

## 🚀 Spouštění

### Všechny admin testy (55 testů)
```bash
make test-e2e-admin
# nebo
cd e2e && npx playwright test specs/admin/
```

### Konkrétní test suite
```bash
# Users CRUD
npx playwright test specs/admin/users-crud.spec.ts

# Roles CRUD
npx playwright test specs/admin/roles-crud.spec.ts

# Groups CRUD
npx playwright test specs/admin/groups-crud.spec.ts

# Tenants CRUD
npx playwright test specs/admin/tenants-crud.spec.ts

# Keycloak Sync
make test-e2e-sync
# nebo
npx playwright test specs/admin/keycloak-sync.spec.ts
```

### Debug mode
```bash
# S UI
npx playwright test specs/admin/users-crud.spec.ts --headed

# Debug step-through
npx playwright test specs/admin/users-crud.spec.ts --debug
```

---

## 📋 Detaily test suites

### 1. Users CRUD (10 testů)

**Endpoints**:
- `POST /api/admin/users` - Create user
- `GET /api/admin/users` - List users
- `GET /api/admin/users/{id}` - Get user detail
- `PUT /api/admin/users/{id}` - Update user
- `DELETE /api/admin/users/{id}` - Delete user
- `POST /api/admin/users/{id}/roles` - Assign roles

**Co testujeme**:
- ✅ Create user (admin + user_manager can create)
- ✅ Read user list (all roles can read)
- ✅ Update user (admin + user_manager can update)
- ✅ Delete user (admin only)
- ✅ Assign roles to user
- ✅ RBAC verification (user_manager can't delete)
- ✅ Search & filter
- ✅ Validation (required fields, duplicates)

**API Helpers** (v `helpers/api.ts`):
- `createUser(api, userData)`
- `getUserById(api, userId)`
- `updateUser(api, userId, updates)`
- `deleteUser(api, userId)`
- `assignRolesToUser(api, userId, roleIds)`

---

### 2. Roles CRUD (11 testů)

**Endpoints**:
- `POST /api/admin/roles` - Create role
- `GET /api/admin/roles` - List roles
- `GET /api/admin/roles/{id}` - Get role detail
- `PUT /api/admin/roles/{id}` - Update role
- `DELETE /api/admin/roles/{id}` - Delete role

**Co testujeme**:
- ✅ Create role (admin only)
- ✅ Read role list (admin + user_manager read-only)
- ✅ Update role (admin only)
- ✅ Delete role (admin only)
- ✅ RBAC verification (user_manager read-only, can't modify)
- ✅ Search & filter
- ✅ Validation (required, duplicates)
- ✅ Role permissions/capabilities display

**API Helpers**:
- `createRole(api, roleData)`
- `getRoleById(api, roleId)`
- `updateRole(api, roleId, updates)`
- `deleteRole(api, roleId)`

---

### 3. Groups CRUD (11 testů)

**Endpoints**:
- `POST /api/admin/groups` - Create group
- `GET /api/admin/groups` - List groups
- `GET /api/admin/groups/{id}` - Get group detail
- `PUT /api/admin/groups/{id}` - Update group
- `DELETE /api/admin/groups/{id}` - Delete group
- `POST /api/admin/groups/{id}/members` - Add member
- `DELETE /api/admin/groups/{id}/members/{userId}` - Remove member

**Co testujeme**:
- ✅ Create group (admin + user_manager)
- ✅ Read group list (admin + user_manager)
- ✅ Update group (admin + user_manager)
- ✅ Delete group (admin only)
- ✅ Add/Remove members
- ✅ RBAC verification
- ✅ Search & filter
- ✅ Validation
- ✅ Member count display

**API Helpers**:
- `createGroup(api, groupData)`
- `getGroupById(api, groupId)`
- `updateGroup(api, groupId, updates)`
- `deleteGroup(api, groupId)`
- `addGroupMember(api, groupId, userId)`
- `removeGroupMember(api, groupId, userId)`

---

### 4. Tenants CRUD (13 testů)

**Endpoints**:
- `POST /api/admin/tenants` - Create tenant
- `GET /api/admin/tenants` - List tenants
- `GET /api/admin/tenants/{id}` - Get tenant detail
- `PUT /api/admin/tenants/{id}` - Update tenant
- `DELETE /api/admin/tenants/{id}` - Delete tenant
- `PATCH /api/admin/tenants/{id}/toggle-enabled` - Toggle enabled status

**Co testujeme**:
- ✅ Create tenant (admin only)
- ✅ Read tenant list (admin sees all, tenant_admin sees own)
- ✅ Update tenant (admin only)
- ✅ Delete tenant (admin only)
- ✅ **Grafana datasource auto-provisioning** (on create)
- ✅ **Grafana cleanup** (on delete)
- ✅ Toggle enabled status
- ✅ RBAC verification
- ✅ Search & filter
- ✅ Validation (required, format, duplicates)
- ✅ Tenant statistics

**API Helpers**:
- `createTenant(api, tenantData)`
- `getTenantById(api, tenantId)`
- `updateTenant(api, tenantId, updates)`
- `deleteTenant(api, tenantId)`
- `toggleTenantEnabled(api, tenantId)`

**Speciální funkcionality**:
- Grafana datasource provisioning ověření
- Grafana cleanup při smazání tenantu

---

### 5. Keycloak Sync (10 testů) 🆕

**Endpoints**:
- `POST /api/admin/keycloak-sync/users/{tenantKey}` - Sync users
- `POST /api/admin/keycloak-sync/roles/{tenantKey}` - Sync roles
- `POST /api/admin/keycloak-sync/groups/{tenantKey}` - Sync groups
- `POST /api/admin/keycloak-sync/all/{tenantKey}` - Full sync
- `GET /api/admin/keycloak-sync/status/{syncId}` - Get sync status

**Co testujeme**:
- ✅ Sync users from Keycloak (async job)
- ✅ Sync roles from Keycloak (async job)
- ✅ Sync groups from Keycloak (async job)
- ✅ Full sync (users + roles + groups)
- ✅ Sync status tracking (polling async job progress)
- ✅ Idempotence verification (repeated sync is safe)
- ✅ RBAC verification (admin only)
- ✅ Error handling (invalid tenant key)
- ✅ Tenant isolation (sync only affects target tenant)
- ✅ Sync statistics validation (counts, errors)

**API Helpers** (v `helpers/api.ts`):
```typescript
// Trigger sync operations (all return 202 Accepted + syncId)
syncUsersFromKeycloak(api, tenantKey)      // Returns { status, syncId, message }
syncRolesFromKeycloak(api, tenantKey)      // Returns { status, syncId, message }
syncGroupsFromKeycloak(api, tenantKey)     // Returns { status, syncId, message }
syncAllFromKeycloak(api, tenantKey)        // Returns { status, syncId, message }

// Poll async job status
getSyncStatus(api, syncId)                 // Returns { status, result, progress, ... }
```

**Async Job Pattern**:
```typescript
// 1. Trigger sync (returns immediately)
const response = await syncUsersFromKeycloak(api, 'admin');
expect(response.status).toBe('ACCEPTED');
const syncId = response.syncId;

// 2. Wait for completion
await page.waitForTimeout(3000);

// 3. Poll status
const status = await getSyncStatus(api, syncId);
expect(status.status).toBe('COMPLETED');

// 4. Verify results
expect(status.result.usersProcessed).toBeGreaterThan(0);
```

**Status Progression**:
- `PENDING` → Initial state
- `RUNNING` → Job executing
- `COMPLETED` → Success
- `FAILED` → Error occurred

---

## 🧪 Test Coverage Matrix

| Feature | Create | Read | Update | Delete | RBAC | Search | Validation | Special |
|---------|--------|------|--------|--------|------|--------|------------|---------|
| **Users** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Role assignment |
| **Roles** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Permissions |
| **Groups** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Member mgmt |
| **Tenants** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Grafana provision |
| **Keycloak Sync** | N/A | ✅ | N/A | N/A | ✅ | N/A | ✅ | Async jobs |

---

## 🔒 RBAC Test Coverage

### Admin (full access)
- ✅ Can create/read/update/delete all entities
- ✅ Can trigger Keycloak Sync
- ✅ Can manage tenant provisioning

### User Manager (limited)
- ✅ Can create/read/update users
- ❌ Cannot delete users
- ✅ Can read roles (read-only)
- ❌ Cannot modify roles
- ✅ Can create/read/update groups
- ❌ Cannot delete groups
- ❌ Cannot manage tenants
- ❌ Cannot trigger Keycloak Sync

### Regular User
- ✅ Can read own user info
- ❌ Cannot access admin endpoints

---

## ⚙️ Prerequisites

### Backend musí běžet
```bash
make dev-up
# nebo
make up
```

### Backend musí být healthy
```bash
make verify
# nebo
docker inspect core-backend --format='{{.State.Health.Status}}'
```

### Test data
Testy vytvářejí a mazejí vlastní test data (atomické, nezávislé).

---

## 🐛 Troubleshooting

### Test timeout
```bash
# Zkontrolovat backend logy
make logs-backend

# Ověřit health
make verify
```

### RBAC test fails
```bash
# Zkontrolovat Keycloak roles
# Verify test_admin, test_user_manager users exist
```

### Keycloak Sync fails
```bash
# Zkontrolovat Keycloak connectivity
make logs-backend | grep -i keycloak

# Verify tenant 'admin' exists
curl -H "Authorization: Bearer $AT" http://localhost:8080/api/admin/tenants
```

---

## 📈 Performance

| Test Suite | Počet testů | Avg. doba | Max. doba |
|------------|-------------|-----------|-----------|
| Users CRUD | 10 | 45s | 60s |
| Roles CRUD | 11 | 50s | 70s |
| Groups CRUD | 11 | 55s | 75s |
| Tenants CRUD | 13 | 70s | 90s |
| Keycloak Sync | 10 | 60s | 90s |
| **CELKEM** | **55** | **~3-4 min** | **~5 min** |

---

## 🔄 CI/CD Integration

### GitHub Actions
```yaml
- name: Run admin CRUD E2E tests
  run: make test-e2e-admin
```

### Makefile
```bash
# Standalone
make test-e2e-admin

# V rámci full pipeline
make clean-fast      # Build
make test-e2e-admin  # Validate
```

---

## 📝 Maintenance

### Přidání nového testu

1. **Edituj existující suite** (např. `users-crud.spec.ts`):
   ```typescript
   test('should do new thing', async ({ page }) => {
     await loginAsAdmin(page);
     // test logic
   });
   ```

2. **Přidat helper do `api.ts`** (pokud nový endpoint):
   ```typescript
   export async function newOperation(api: ApiContext, data: any) {
     const response = await api.post('/api/admin/new-endpoint', { data });
     expect(response.ok()).toBeTruthy();
     return await response.json();
   }
   ```

3. **Spustit test**:
   ```bash
   npx playwright test specs/admin/users-crud.spec.ts
   ```

4. **Aktualizovat dokumentaci** (tento soubor)

---

**Poslední revize**: 26. října 2025  
**Kontakt**: Core Platform Team
