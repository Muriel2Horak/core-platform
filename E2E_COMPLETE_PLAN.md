# 🎯 Complete E2E Testing & Configuration Plan

**Date**: 2025-01-16  
**Goal**: Complete all E2E tests, fix role configuration, and ensure comprehensive testing coverage  

---

## 📋 Task List

### 1. Role Configuration (HIGH PRIORITY)

#### Current State Analysis
**Roles defined in `permissions.yml`:**
- ✅ `CORE_ROLE_ADMIN` - Full admin
- ✅ `CORE_ROLE_TENANT_ADMIN` - Tenant admin  
- ✅ `CORE_ROLE_USER_MANAGER` - User manager
- ✅ `CORE_ROLE_USER` - Basic user

**Roles in Keycloak `realm-admin.json`:**
- ✅ `CORE_ROLE_USER`
- ✅ `CORE_ROLE_ADMIN`
- ✅ `CORE_ROLE_MONITORING`
- ✅ `CORE_ROLE_TENANT_MONITORING`

#### Missing Roles in Keycloak
❌ **Need to add to `docker/keycloak/realm-admin.json`:**
1. `CORE_ROLE_TENANT_ADMIN`
2. `CORE_ROLE_USER_MANAGER`
3. `CORE_ROLE_STUDIO` (new - for Metamodel Studio access)
4. `CORE_ROLE_REPORTING` (new - for Reporting Explorer access)

#### Action Items
- [ ] Add missing roles to `realm-admin.json`
- [ ] Add missing roles to `realm-admin.template.json`
- [ ] Update `permissions.yml` with STUDIO and REPORTING roles
- [ ] Update test users to have correct roles
- [ ] Document role hierarchy

---

### 2. E2E Test Completion (HIGH PRIORITY)

#### 2.1 AI Tests (0/2 files)
**Files**:
- `specs/ai/ai-help-widget.spec.ts`
- `specs/ai/mcp-endpoints.spec.ts`

**Migration needed**:
- [ ] Replace `[data-testid="ai-help-button"]` with a11y selectors
- [ ] Add aria-labels to AI widget if missing
- [ ] Test AI enable/disable toggle
- [ ] Test MCP endpoint responses

**Estimated time**: 1 hour

#### 2.2 Monitoring Tests (0/1 file)
**File**:
- `specs/monitoring/grafana-scenes-integration.spec.ts`

**Migration needed**:
- [ ] Already uses API-based testing (good!)
- [ ] Verify Grafana provisioning flow
- [ ] Test multi-tenant data isolation
- [ ] No GUI selectors to migrate

**Estimated time**: 30 minutes (just review)

---

### 3. CRUD Testing (CRITICAL - MISSING!)

#### 3.1 User Management CRUD
**File to create**: `specs/admin/users-crud.spec.ts`

**Tests needed**:
- [ ] Create user (admin)
- [ ] Read user list (admin, user_manager)
- [ ] Update user (admin, user_manager)
- [ ] Delete user (admin only)
- [ ] Assign roles (admin, user_manager)
- [ ] RBAC: user_manager can't delete users

**Estimated time**: 2 hours

#### 3.2 Role Management CRUD
**File to create**: `specs/admin/roles-crud.spec.ts`

**Tests needed**:
- [ ] Create role (admin only)
- [ ] Read role list (admin, user_manager read-only)
- [ ] Update role (admin only)
- [ ] Delete role (admin only)
- [ ] RBAC: user_manager can only read

**Estimated time**: 1.5 hours

#### 3.3 Group Management CRUD
**File to create**: `specs/admin/groups-crud.spec.ts`

**Tests needed**:
- [ ] Create group (admin only)
- [ ] Read group list (admin, user_manager)
- [ ] Update group (admin only)
- [ ] Delete group (admin only)
- [ ] Add members to group
- [ ] Remove members from group

**Estimated time**: 2 hours

#### 3.4 Tenant Management CRUD
**File to create**: `specs/admin/tenants-crud.spec.ts`

**Tests needed**:
- [ ] Create tenant (admin only)
- [ ] Read tenant list (admin, tenant_admin)
- [ ] Update tenant (admin, tenant_admin)
- [ ] Delete tenant (admin only)
- [ ] Verify Grafana provisioning on create
- [ ] Verify cleanup on delete

**Estimated time**: 2 hours

---

### 4. Streaming & CDC Testing (CRITICAL - MISSING!)

#### 4.1 Kafka Streaming Tests
**File to create**: `specs/streaming/kafka-events.spec.ts`

**Tests needed**:
- [ ] User created → Kafka event published
- [ ] User updated → Kafka event published
- [ ] User deleted → Kafka event published
- [ ] Role assigned → Kafka event published
- [ ] Tenant created → Kafka event published
- [ ] Verify event schema (UserEvent, RoleEvent, TenantEvent)
- [ ] Verify partition key (tenantId)
- [ ] Verify message ordering

**Estimated time**: 3 hours

#### 4.2 CDC (Change Data Capture) Tests
**File to create**: `specs/streaming/cdc-sync.spec.ts`

**Tests needed**:
- [ ] Create user in Keycloak → CDC syncs to DB
- [ ] Update user in Keycloak → CDC syncs to DB
- [ ] Delete user in Keycloak → CDC marks as deleted
- [ ] Verify sync latency (<5 seconds)
- [ ] Verify no data loss
- [ ] Test reconnection after Kafka failure

**Estimated time**: 3 hours

#### 4.3 Full Refresh Tests
**File to create**: `specs/streaming/full-refresh.spec.ts`

**Tests needed**:
- [ ] Trigger full refresh from Keycloak
- [ ] Verify all users synced
- [ ] Verify all roles synced
- [ ] Verify all groups synced
- [ ] Verify progress tracking
- [ ] Verify error handling (Keycloak down)
- [ ] Test incremental sync after full refresh

**Estimated time**: 2 hours

---

### 5. Test Tenant Cleanup (MEDIUM PRIORITY)

#### Issue
Found 2 test tenants in system - need to investigate origin.

**Action items**:
- [ ] Query database for tenant list
- [ ] Identify test tenant creation source
- [ ] Add cleanup in E2E test teardown
- [ ] Document tenant lifecycle management

**SQL Query**:
```sql
SELECT * FROM tenant WHERE key LIKE '%test%' OR key LIKE '%e2e%';
```

**Estimated time**: 1 hour

---

### 6. Test Data Fixtures (LOW PRIORITY)

#### Create Test Data Helpers
**File to create**: `e2e/helpers/fixtures.ts`

**Helpers needed**:
- [ ] `createTestUser()`
- [ ] `createTestRole()`
- [ ] `createTestGroup()`
- [ ] `createTestTenant()`
- [ ] `cleanupTestData()`
- [ ] `resetToCleanState()`

**Estimated time**: 2 hours

---

## 📊 Estimated Time Summary

| Task | Time | Priority |
|------|------|----------|
| **Role Configuration** | 2 hours | 🔴 HIGH |
| **AI Tests Migration** | 1 hour | 🔴 HIGH |
| **Monitoring Tests Review** | 30 min | 🟡 MEDIUM |
| **Users CRUD** | 2 hours | 🔴 CRITICAL |
| **Roles CRUD** | 1.5 hours | 🔴 CRITICAL |
| **Groups CRUD** | 2 hours | 🔴 CRITICAL |
| **Tenants CRUD** | 2 hours | 🔴 CRITICAL |
| **Kafka Streaming** | 3 hours | 🔴 CRITICAL |
| **CDC Sync** | 3 hours | 🔴 CRITICAL |
| **Full Refresh** | 2 hours | 🔴 CRITICAL |
| **Test Tenant Cleanup** | 1 hour | 🟡 MEDIUM |
| **Test Fixtures** | 2 hours | 🟢 LOW |
| **TOTAL** | **22 hours** | |

**Realistic completion**: 3-4 working days

---

## 🎯 Execution Order

### Phase 1: Foundation (Day 1 - 6 hours)
1. ✅ Fix role configuration in Keycloak (2h)
2. ✅ Migrate AI tests (1h)
3. ✅ Review monitoring tests (30min)
4. ✅ Create test fixtures helpers (2h)
5. ✅ Cleanup test tenants (30min)

### Phase 2: CRUD Tests (Day 2 - 8 hours)
1. ✅ Users CRUD + RBAC (2h)
2. ✅ Roles CRUD + RBAC (1.5h)
3. ✅ Groups CRUD + RBAC (2h)
4. ✅ Tenants CRUD + Grafana (2h)
5. ✅ Integration testing (30min)

### Phase 3: Streaming & CDC (Day 3-4 - 8 hours)
1. ✅ Kafka event publishing tests (3h)
2. ✅ CDC sync tests (3h)
3. ✅ Full refresh tests (2h)

---

## 📝 Detailed Implementation Notes

### Role Configuration Changes

#### Add to `docker/keycloak/realm-admin.json`:
```json
{
  "name": "CORE_ROLE_TENANT_ADMIN",
  "description": "Tenant administrator - manage users and roles within tenant"
},
{
  "name": "CORE_ROLE_USER_MANAGER", 
  "description": "User manager - create and update users, assign roles"
},
{
  "name": "CORE_ROLE_STUDIO",
  "description": "Metamodel Studio access - entity design and AI configuration"
},
{
  "name": "CORE_ROLE_REPORTING",
  "description": "Reporting Explorer access - advanced data analysis"
}
```

#### Add to `permissions.yml`:
```yaml
CORE_ROLE_STUDIO:
  display_name: "Studio Designer"
  description: "Navrhář entit a AI konfigurace"
  api_permissions:
    - "metamodel:read:all"
    - "metamodel:create:all"
    - "metamodel:update:all"
    - "metamodel:delete:all"
    - "ai-config:read:all"
    - "ai-config:update:all"
  menu_items:
    - id: "studio"
      label: "Metamodel Studio"
      path: "/core-admin/studio"
      icon: "SettingsIcon"
  features:
    - "entity_designer"
    - "ai_configuration"
  data_scope: "all_tenants"

CORE_ROLE_REPORTING:
  display_name: "Reporting Analyst"
  description: "Pokročilá analýza dat"
  api_permissions:
    - "reporting:read:all"
    - "reporting:export:all"
  menu_items:
    - id: "reporting"
      label: "Reporting Explorer"
      path: "/reporting"
      icon: "TableChartIcon"
  features:
    - "advanced_filters"
    - "data_export"
    - "grid_customization"
  data_scope: "all_tenants"
```

### Test User Updates

#### Update `test_admin` user:
```yaml
username: test_admin
roles:
  - CORE_ROLE_ADMIN
  - CORE_ROLE_USER
  - CORE_ROLE_MONITORING
  - CORE_ROLE_STUDIO
  - CORE_ROLE_REPORTING
```

#### Create `test_studio` user:
```yaml
username: test_studio
password: Test.1234
roles:
  - CORE_ROLE_USER
  - CORE_ROLE_STUDIO
```

#### Create `test_analyst` user:
```yaml
username: test_analyst
password: Test.1234
roles:
  - CORE_ROLE_USER
  - CORE_ROLE_REPORTING
```

---

## 🚀 Next Immediate Actions

1. **Update realm-admin.json** with missing roles
2. **Create test fixtures** helper file
3. **Start with Users CRUD** test (highest business value)
4. **Add Kafka streaming** tests (most critical for data consistency)
5. **Document everything** as we go

---

*Generated: 2025-01-16*  
*Priority: CRITICAL - Required for production readiness*  
*Estimated completion: 3-4 days of focused work*
