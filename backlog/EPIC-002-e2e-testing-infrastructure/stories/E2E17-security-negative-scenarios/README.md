# E2E17: Security & Negative E2E Scenarios

**Status:** 🔵 **TODO**  
**Priority:** P0 (HIGH VALUE - Phase 1-2)  
**Effort:** ~8 hodin  
**LOC:** ~500 řádků

---

## 🎯 Cíl Story

**Security E2E scénáře** pro ověření tenant isolation, RBAC, authentication a negative cases.

**As a** developer  
**I want** security E2E scénáře  
**So that** vím že tenant isolation a RBAC funguje správně

---

## 📋 Acceptance Criteria

### 1. Security Test Scenarios (5 scénářů)

✅ **SECURITY-1: Unauthenticated Access → Redirect to Login**
```typescript
test('SECURITY-1: Unauthenticated user redirected to login @SECURITY @CRITICAL', async ({ page }) => {
  // 1. Try to access protected route without login
  await page.goto('https://core-platform.local/admin');
  
  // 2. Verify redirect to Keycloak login
  await expect(page).toHaveURL(/admin.core-platform.local.*realms\/admin\/protocol\/openid-connect\/auth/);
  
  // 3. Verify login form visible
  await expect(page.getByLabel('Username')).toBeVisible();
  await expect(page.getByLabel('Password')).toBeVisible();
  
  // 4. Try to access API directly
  const response = await page.request.get('https://core-platform.local/api/metamodel/entities');
  
  // 5. Verify 401 Unauthorized
  expect(response.status()).toBe(401);
});
```

✅ **SECURITY-2: Tenant Isolation (Tenant A ≠ Tenant B)**
```typescript
test('SECURITY-2: Tenant A cannot see Tenant B data @SECURITY @CRITICAL', async ({ page }) => {
  // 1. Login as Tenant A admin
  await loginAs(page, 'e2e_tenant_a', 'Test.1234');
  
  // 2. Create entity in Tenant A
  await page.goto('/admin/metamodel');
  await createEntity(page, 'TenantA_Entity', 'Private to Tenant A');
  
  // 3. Logout
  await logout(page);
  
  // 4. Login as Tenant B admin
  await loginAs(page, 'e2e_tenant_b', 'Test.1234');
  
  // 5. Navigate to Metamodel
  await page.goto('/admin/metamodel');
  
  // 6. Verify Tenant A entity NOT visible
  await expect(page.getByText('TenantA_Entity')).not.toBeVisible();
  
  // 7. Try direct API access to Tenant A entity
  const tenantAEntityId = await getEntityId('TenantA_Entity');
  const response = await page.request.get(`/api/metamodel/entities/${tenantAEntityId}`);
  
  // 8. Verify 403 Forbidden or 404 Not Found
  expect([403, 404]).toContain(response.status());
  
  // Cleanup
  await loginAs(page, 'e2e_tenant_a', 'Test.1234');
  await deleteEntity(page, 'TenantA_Entity');
});
```

✅ **SECURITY-3: RBAC - Non-Admin Cannot Access Admin Section**
```typescript
test('SECURITY-3: USER role cannot access admin features @SECURITY @CRITICAL', async ({ page }) => {
  // 1. Login as regular user (no admin role)
  await loginAs(page, 'test_user', 'Test.1234');
  
  // 2. Verify admin menu items NOT visible
  await expect(page.getByRole('link', { name: 'User Management' })).not.toBeVisible();
  await expect(page.getByRole('link', { name: 'System Settings' })).not.toBeVisible();
  
  // 3. Try to navigate to admin route directly
  await page.goto('/admin/users');
  
  // 4. Verify access denied (403 or redirect)
  await expect(page.getByText('Access Denied')).toBeVisible()
    .or(expect(page).toHaveURL(/\/admin$/)); // Redirect to dashboard
  
  // 5. Try API call to admin endpoint
  const response = await page.request.get('/api/admin/users');
  
  // 6. Verify 403 Forbidden
  expect(response.status()).toBe(403);
});
```

✅ **SECURITY-4: Expired Token → Redirect to Login**
```typescript
test('SECURITY-4: Expired token redirects to login @SECURITY', async ({ page, context }) => {
  // 1. Login normally
  await loginAs(page, 'test_admin', 'Test.1234');
  
  // 2. Verify logged in
  await expect(page.getByText('test_admin')).toBeVisible();
  
  // 3. Manually expire token (set cookie expiry to past)
  const cookies = await context.cookies();
  const sessionCookie = cookies.find(c => c.name === 'SESSION' || c.name === 'JSESSIONID');
  
  if (sessionCookie) {
    await context.addCookies([{
      ...sessionCookie,
      expires: Math.floor(Date.now() / 1000) - 3600 // 1 hour ago
    }]);
  }
  
  // 4. Try to access protected page
  await page.goto('/admin/metamodel');
  
  // 5. Verify redirect to login
  await expect(page).toHaveURL(/realms\/admin\/protocol\/openid-connect\/auth/);
});
```

✅ **SECURITY-5: CSRF Token Validation**
```typescript
test('SECURITY-5: CSRF token required for state-changing operations @SECURITY', async ({ page }) => {
  // 1. Login
  await loginAs(page, 'test_admin', 'Test.1234');
  
  // 2. Get current CSRF token from page
  const csrfToken = await page.evaluate(() => {
    const meta = document.querySelector('meta[name="csrf-token"]');
    return meta?.getAttribute('content');
  });
  
  // 3. Make POST request WITHOUT CSRF token
  const responseNoCsrf = await page.request.post('/api/metamodel/entities', {
    data: { name: 'TestEntity', description: 'Test' }
    // No CSRF token header
  });
  
  // 4. Verify 403 Forbidden
  expect(responseNoCsrf.status()).toBe(403);
  
  // 5. Make POST request WITH valid CSRF token
  const responseWithCsrf = await page.request.post('/api/metamodel/entities', {
    headers: { 'X-CSRF-TOKEN': csrfToken },
    data: { name: 'TestEntity', description: 'Test' }
  });
  
  // 6. Verify success (201 Created)
  expect(responseWithCsrf.status()).toBe(201);
  
  // Cleanup
  const entityId = (await responseWithCsrf.json()).id;
  await page.request.delete(`/api/metamodel/entities/${entityId}`, {
    headers: { 'X-CSRF-TOKEN': csrfToken }
  });
});
```

### 2. Negative Test Scenarios

✅ **NEGATIVE-1: Invalid Input Validation**
```typescript
test('NEGATIVE-1: Invalid entity data rejected @SECURITY', async ({ page }) => {
  await loginAs(page, 'test_admin', 'Test.1234');
  await page.goto('/admin/metamodel');
  
  // Try to create entity with invalid data
  await page.getByRole('button', { name: 'New Entity' }).click();
  
  // Empty name
  await page.getByLabel('Entity Name').fill('');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Name is required')).toBeVisible();
  
  // Name too long (> 255 chars)
  await page.getByLabel('Entity Name').fill('A'.repeat(300));
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Name must be less than 255 characters')).toBeVisible();
  
  // Special characters not allowed
  await page.getByLabel('Entity Name').fill('Test<script>alert("XSS")</script>');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('Invalid characters in name')).toBeVisible();
});
```

✅ **NEGATIVE-2: Unauthorized API Access**
```typescript
test('NEGATIVE-2: API rejects requests without proper headers @SECURITY', async ({ page }) => {
  // Try API call without authentication
  const responseNoAuth = await page.request.get('/api/metamodel/entities');
  expect(responseNoAuth.status()).toBe(401);
  
  // Try API call with invalid token
  const responseInvalidToken = await page.request.get('/api/metamodel/entities', {
    headers: { 'Authorization': 'Bearer invalid-token-123' }
  });
  expect(responseInvalidToken.status()).toBe(401);
  
  // Try API call without Content-Type header on POST
  await loginAs(page, 'test_admin', 'Test.1234');
  const responseNoContentType = await page.request.post('/api/metamodel/entities', {
    data: 'invalid data'
    // No Content-Type header
  });
  expect(responseNoContentType.status()).toBe(415); // Unsupported Media Type
});
```

### 3. Security Test Tagging

✅ Všechny security testy tagged:
- `@SECURITY` - Security test identifier
- `@CRITICAL` - Critical security path (SECURITY-1, 2, 3)
- `@TENANT(admin)` - Runs in admin tenant context

### 4. Test Data Isolation

✅ Security test data cleanup:
```typescript
// e2e/helpers/security-cleanup.ts
export async function cleanupSecurityTestData() {
  // Delete all entities created by e2e_tenant_a, e2e_tenant_b
  await deleteEntitiesWithPrefix('TenantA_');
  await deleteEntitiesWithPrefix('TenantB_');
  
  // Clear any leaked sessions
  await clearExpiredSessions();
}
```

---

## 📂 Implementace

### File Structure

```
e2e/
├── specs/
│   └── security/
│       ├── auth/
│       │   ├── unauthenticated.spec.ts     (SECURITY-1)
│       │   └── expired-token.spec.ts       (SECURITY-4)
│       ├── tenant-isolation.spec.ts        (SECURITY-2)
│       ├── rbac.spec.ts                    (SECURITY-3)
│       ├── csrf.spec.ts                    (SECURITY-5)
│       └── negative/
│           ├── invalid-input.spec.ts       (NEGATIVE-1)
│           └── unauthorized-api.spec.ts    (NEGATIVE-2)
├── helpers/
│   ├── security-cleanup.ts
│   └── tenant-helpers.ts
└── fixtures/
    └── security-test-users.ts

docs/
└── testing/
    └── security-testing.md
```

### Security Test Documentation

**File:** `docs/testing/security-testing.md`

```markdown
# Security Testing Guide

## Security Test Categories

### 1. Authentication Tests
- Unauthenticated access → redirect to login
- Expired token → re-authentication required
- Invalid credentials → error message

### 2. Authorization Tests (RBAC)
- Admin role → full access
- User role → limited access
- No role → access denied

### 3. Tenant Isolation Tests
- Tenant A cannot see Tenant B data
- Tenant A cannot modify Tenant B data
- Cross-tenant API calls rejected

### 4. CSRF Protection
- POST/PUT/DELETE require CSRF token
- GET requests don't require CSRF token
- Invalid CSRF token → 403 Forbidden

### 5. Input Validation
- SQL injection prevented
- XSS prevented
- Invalid characters rejected
- Length limits enforced

## Test Users

| Username | Role | Tenant | Purpose |
|----------|------|--------|---------|
| `e2e_tenant_a` | ADMIN | tenant_a | Tenant isolation tests |
| `e2e_tenant_b` | ADMIN | tenant_b | Tenant isolation tests |
| `test_user` | USER | admin | RBAC tests (limited access) |
| `test_admin` | ADMIN | admin | RBAC tests (full access) |

## Running Security Tests

```bash
# All security tests
npm run test:security

# Specific category
npx playwright test --grep "@SECURITY.*auth"

# Tenant isolation only
npx playwright test specs/security/tenant-isolation.spec.ts
```

## Security Checklist

- [ ] Unauthenticated access blocked
- [ ] Expired tokens handled correctly
- [ ] Tenant isolation enforced
- [ ] RBAC permissions enforced
- [ ] CSRF protection active
- [ ] Input validation working
- [ ] API authorization checked
```

---

## ✅ Definition of Done

- [ ] 5 security test scénářů implementovány (auth, tenant, RBAC, token, CSRF)
- [ ] 2 negative test scénáře (invalid input, unauthorized API)
- [ ] Všechny security testy tagged `@SECURITY`
- [ ] Critical security tests tagged `@CRITICAL`
- [ ] Security test data cleanup implemented
- [ ] Security testing guide dokumentace
- [ ] Test users pro tenant isolation vytvořeni
- [ ] NPM script `test:security`
- [ ] Security tests běží < 10 min
- [ ] CI integrace ready (GitHub Actions)

---

## 📊 Success Metrics

- **Execution Time:** < 10 min
- **Coverage:** 5 kritických security scénářů pokryto
- **Reliability:** < 3% flaky rate (authentication může být flaky)
- **Detection:** 100% security regressions detected

---

## 🔗 Dependencies

- **E2E1:** Playwright framework setup
- **E2E2:** Page Object Model (LoginPage)
- **E2E14:** Test data management (e2e_tenant_a, e2e_tenant_b users)
- Keycloak realm konfigurace (admin realm)
- Backend Spring Security konfigurace (CSRF, RBAC)

---

## 📝 Implementation Notes

### Security Test Design Principles

1. **Defense in Depth:** Test multiple layers (UI, API, DB)
2. **Real Attacks:** Simulate real security vulnerabilities
3. **Positive + Negative:** Test both allowed and blocked scenarios
4. **Isolation:** Security tests nesmí ovlivnit ostatní testy

### Tenant Isolation Strategy

- **Setup:** Create 2 separate tenant users (`e2e_tenant_a`, `e2e_tenant_b`)
- **Test:** Tenant A creates data → Tenant B cannot see/access it
- **Verify:** Both UI visibility and API access blocked
- **Cleanup:** Delete all tenant-specific test data

### RBAC Test Strategy

- **Roles:** Test USER vs ADMIN permissions
- **Endpoints:** Test UI menu visibility + API access
- **Assertions:** Verify 403 Forbidden for unauthorized access

### CSRF Token Handling

- **Extraction:** Read CSRF token from meta tag or cookie
- **Validation:** POST/PUT/DELETE require token
- **Error:** Missing token → 403 Forbidden

---

## 🔒 Security Risks Mitigated

| Risk | Test Coverage |
|------|---------------|
| **Unauthorized access** | SECURITY-1, SECURITY-3 |
| **Tenant data leakage** | SECURITY-2 |
| **Session hijacking** | SECURITY-4 |
| **CSRF attacks** | SECURITY-5 |
| **XSS/SQL injection** | NEGATIVE-1 |
| **API abuse** | NEGATIVE-2 |

---

**Effort:** 8 hodin  
**Priority:** P0 (Phase 1-2 HIGH VALUE)  
**Value:** Ověření security mechanismů, detekce tenant isolation issues, RBAC validation

**Created:** 9. listopadu 2025
