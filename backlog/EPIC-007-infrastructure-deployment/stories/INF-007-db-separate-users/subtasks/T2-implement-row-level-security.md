# T2: Implement Row-Level Security Policies

**Parent Story:** INF-007 DB Separate Users per Service  
**Status:** 🔴 TODO  
**Priority:** 🟡 MEDIUM  
**Effort:** 4 hours  
**Owner:** Backend

---

## 🎯 Objective

Add PostgreSQL Row-Level Security (RLS) policies to prevent cross-tenant data access.

---

## 📋 Tasks

### 1. Enable RLS on Core Tables

**File:** `backend/src/main/resources/db/migration/V1000__enable_rls.sql`

```sql
-- Enable RLS on all tenant-scoped tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE workflows ENABLE ROW LEVEL SECURITY;
ALTER TABLE executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Create policies for backend user
CREATE POLICY tenant_isolation_users ON users
    FOR ALL
    TO core_app
    USING (tenant_id = current_setting('app.tenant_id')::int);

CREATE POLICY tenant_isolation_workflows ON workflows
    FOR ALL
    TO core_app
    USING (tenant_id = current_setting('app.tenant_id')::int);

CREATE POLICY tenant_isolation_executions ON executions
    FOR ALL
    TO core_app
    USING (tenant_id = current_setting('app.tenant_id')::int);

CREATE POLICY tenant_isolation_audit_logs ON audit_logs
    FOR ALL
    TO core_app
    USING (tenant_id = current_setting('app.tenant_id')::int);
```

### 2. Create Tenant Context Filter

**File:** `backend/src/main/java/cz/muriel/core/config/TenantFilter.java`

```java
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {
    
    @Autowired
    private EntityManager entityManager;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain chain) throws ServletException, IOException {
        // Extract tenant from JWT or header
        String tenantId = extractTenantId(request);
        
        // Set PostgreSQL session variable
        entityManager.createNativeQuery(
            "SET LOCAL app.tenant_id = :tenantId"
        ).setParameter("tenantId", tenantId).executeUpdate();
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Reset after request
            entityManager.createNativeQuery(
                "RESET app.tenant_id"
            ).executeUpdate();
        }
    }
    
    private String extractTenantId(HttpServletRequest request) {
        // From JWT token claim
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("tenant_id");
        }
        
        // From X-Tenant-ID header (fallback)
        return request.getHeader("X-Tenant-ID");
    }
}
```

### 3. Test RLS Policies

```java
@Test
void testTenantIsolation() {
    // Set tenant context to 1
    entityManager.createNativeQuery("SET LOCAL app.tenant_id = 1").executeUpdate();
    
    // Query users (should only see tenant 1)
    List<User> users = userRepository.findAll();
    assertThat(users).allMatch(u -> u.getTenantId() == 1);
    
    // Try to access tenant 2 data
    entityManager.createNativeQuery("SET LOCAL app.tenant_id = 2").executeUpdate();
    List<User> tenant2Users = userRepository.findAll();
    assertThat(tenant2Users).allMatch(u -> u.getTenantId() == 2);
    
    // Verify tenant 1 and 2 data is separate
    assertThat(users).noneMatch(u -> tenant2Users.contains(u));
}
```

---

## ✅ Acceptance Criteria

- [ ] RLS enabled on all tenant-scoped tables
- [ ] Policies prevent cross-tenant SELECT/INSERT/UPDATE/DELETE
- [ ] TenantFilter sets session variable on every request
- [ ] Unit tests verify tenant isolation
- [ ] Integration tests with Testcontainers pass

---

## 🔗 Dependencies

- Requires T1 (separate users created)
- Requires INF-020 (multi-tenancy architecture)
