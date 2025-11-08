# INF-020: Multi-Tenancy Architecture (Subdomains)

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 dny, ~1200 LOC  
**Owner:** Platform + Architecture Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**

```
# Single-tenant deployment:
https://core-platform.local/
  ├── /api         → Backend
  ├── /grafana     → Monitoring (admin only)
  └── /            → Frontend

# ŽÁDNÁ tenant isolation:
- Frontend: Single deployment pro všechny
- Backend: Žádný tenant context
- Grafana: Shared dashboards
- Loki: Mixed logs
- N8N: Není multi-tenant
```

**Issues:**
- Data leakage risk (tenant A vidí data tenant B)
- Žádná tenant-specific konfigurace
- Performance bottlenecks (všichni sdílí resources)

### Goal

**Subdomain-based multi-tenancy:**

```
# Tenant isolation via subdomains:
https://tenant-a.core-platform.com/     → Tenant A frontend
https://tenant-b.core-platform.com/     → Tenant B frontend

# Shared services (admin access):
https://admin.core-platform.com/        → Admin panel
https://admin.core-platform.com/grafana → Monitoring
https://admin.core-platform.com/api     → Admin API

# Workflow engine per tenant:
https://workflows-tenant-a.core-platform.com/ → N8N for Tenant A
https://workflows-tenant-b.core-platform.com/ → N8N for Tenant B
```

**Architecture:**

```
Nginx/Traefik
  ├─ *.core-platform.com → Wildcard SSL
  ├─ tenant-a.* → Frontend (tenant context)
  ├─ tenant-b.* → Frontend (tenant context)
  ├─ workflows-{tenant}.* → N8N instance
  └─ admin.* → Admin panel

Backend
  ├─ TenantFilter → Extract tenant from subdomain
  ├─ TenantContext → Thread-local storage
  └─ Row-level security → WHERE tenant_id = ?

Database
  ├─ Tenant table (id, subdomain, config)
  └─ All tables: tenant_id FK (NOT NULL)

Frontend
  ├─ Tenant config from API
  ├─ Branding per tenant (logo, colors)
  └─ Feature flags per tenant
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Subdomain Routing**
   - Wildcard DNS: `*.core-platform.com`
   - Nginx/Traefik: Extract tenant from subdomain
   - 404 if tenant not found

2. ✅ **Backend Tenant Context**
   - Servlet filter: Extract tenant from `Host` header
   - Thread-local tenant storage
   - All queries filtered by `tenant_id`

3. ✅ **Frontend Tenant Branding**
   - API: `/api/tenants/current` → config JSON
   - Dynamic logo, colors, title
   - Feature flags per tenant

4. ✅ **Monitoring Isolation**
   - Grafana: Separate org per tenant
   - Loki: Tenant label filtering
   - Prometheus: Tenant label on metrics

### Implementation

**File:** `docker/nginx/nginx-ssl.conf.template` (Wildcard routing)

```nginx
# Wildcard subdomain routing
server {
    listen 443 ssl http2;
    server_name *.${DOMAIN};
    
    ssl_certificate /etc/nginx/ssl/server.crt.pem;
    ssl_certificate_key /etc/nginx/ssl/server.key.pem;
    
    # Extract tenant from subdomain
    set $tenant "";
    if ($host ~ ^([a-z0-9-]+)\.${DOMAIN}$) {
        set $tenant $1;
    }
    
    # Admin subdomain → admin panel
    location ~ ^/admin {
        if ($tenant != "admin") {
            return 404;
        }
        proxy_pass http://backend:8080;
        proxy_set_header X-Tenant-ID "admin";
    }
    
    # Tenant-specific frontend
    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header X-Tenant-ID $tenant;
        proxy_set_header Host $host;
    }
    
    # API proxy (tenant context from subdomain)
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header X-Tenant-ID $tenant;
        proxy_set_header X-Forwarded-Host $host;
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/multitenancy/TenantFilter.java`

```java
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) {
        try {
            // Extract tenant from X-Tenant-ID header (set by Nginx)
            String tenantId = request.getHeader("X-Tenant-ID");
            
            if (tenantId == null || tenantId.isEmpty()) {
                // Fallback: Extract from Host header
                String host = request.getHeader("Host");
                tenantId = extractTenantFromHost(host);
            }
            
            // Validate tenant exists
            if (!tenantRepository.existsBySubdomain(tenantId)) {
                response.sendError(404, "Tenant not found: " + tenantId);
                return;
            }
            
            // Store in thread-local context
            TenantContext.setCurrentTenant(tenantId);
            
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    
    private String extractTenantFromHost(String host) {
        // Extract: tenant-a.core-platform.com → tenant-a
        String[] parts = host.split("\\.");
        return parts.length > 0 ? parts[0] : "default";
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/multitenancy/TenantContext.java`

```java
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/multitenancy/TenantRowLevelSecurity.java`

```java
@Component
@Aspect
public class TenantRowLevelSecurity {
    
    @Around("@annotation(org.springframework.data.jpa.repository.Query)")
    public Object enforceTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenant = TenantContext.getCurrentTenant();
        
        if (tenant == null) {
            throw new IllegalStateException("No tenant context set!");
        }
        
        // Inject tenant filter into query
        Object[] args = joinPoint.getArgs();
        // ... modify query to add WHERE tenant_id = :tenant
        
        return joinPoint.proceed(args);
    }
}
```

**File:** `backend/src/main/resources/db/migration/V4__multi_tenancy.sql`

```sql
-- Tenants table
CREATE TABLE tenants (
    id SERIAL PRIMARY KEY,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    active BOOLEAN DEFAULT TRUE
);

-- Add tenant_id to all existing tables
ALTER TABLE workflows ADD COLUMN tenant_id INTEGER REFERENCES tenants(id);
ALTER TABLE workflow_executions ADD COLUMN tenant_id INTEGER REFERENCES tenants(id);
ALTER TABLE users ADD COLUMN tenant_id INTEGER REFERENCES tenants(id);

-- Row-level security policies
ALTER TABLE workflows ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON workflows
    USING (tenant_id = current_setting('app.tenant_id')::int);

-- Insert default tenants
INSERT INTO tenants (subdomain, name, config) VALUES
    ('admin', 'Admin Tenant', '{"features": ["all"]}'),
    ('demo', 'Demo Tenant', '{"features": ["workflows", "monitoring"]}');
```

**File:** `frontend/src/hooks/useTenant.ts`

```typescript
export function useTenant() {
    const [tenant, setTenant] = useState<TenantConfig | null>(null);
    
    useEffect(() => {
        // Fetch tenant config from API
        fetch('/api/tenants/current')
            .then(res => res.json())
            .then(config => {
                setTenant(config);
                // Apply branding
                document.title = config.name;
                document.documentElement.style.setProperty('--primary-color', config.primaryColor);
            });
    }, []);
    
    return tenant;
}

// Usage in components:
function App() {
    const tenant = useTenant();
    
    if (!tenant) return <Loading />;
    
    return (
        <div className="app">
            <Logo src={tenant.logoUrl} />
            {tenant.features.includes('workflows') && <WorkflowManager />}
        </div>
    );
}
```

**File:** `docker/grafana/provisioning/datasources/loki-multitenancy.yml`

```yaml
apiVersion: 1

datasources:
  - name: Loki (Tenant A)
    type: loki
    url: http://loki:3100
    jsonData:
      derivedFields:
        - name: tenant_id
          value: tenant-a
  
  - name: Loki (Tenant B)
    type: loki
    url: http://loki:3100
    jsonData:
      derivedFields:
        - name: tenant_id
          value: tenant-b
```

**File:** `backend/src/main/resources/application.yml` (Logging with tenant)

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] [tenant=%X{tenant}] %-5level %logger{36} - %msg%n"

# Logback MDC filter
---
@Component
public class TenantMDCFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String tenant = TenantContext.getCurrentTenant();
        MDC.put("tenant", tenant);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("tenant");
        }
    }
}
```

**Effort:** 4 dny  
**LOC:** ~1200  
**Priority:** 🔥 CRITICAL

---

## 🧪 TESTING STRATEGY

```bash
# Test tenant isolation
curl -H "Host: tenant-a.core-platform.local" \
     https://core-platform.local/api/workflows
# → Returns ONLY tenant A workflows

curl -H "Host: tenant-b.core-platform.local" \
     https://core-platform.local/api/workflows
# → Returns ONLY tenant B workflows

# Test cross-tenant access blocked
curl -H "Host: tenant-a.core-platform.local" \
     https://core-platform.local/api/workflows/tenant-b-workflow-id
# → 404 Not Found
```

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
