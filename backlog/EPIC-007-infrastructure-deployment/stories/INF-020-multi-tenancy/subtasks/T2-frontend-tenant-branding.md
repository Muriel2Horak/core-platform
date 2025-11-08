# T2: Implement Frontend Tenant Branding

**Parent Story:** INF-020 Multi-Tenancy Architecture  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 5 hours  
**Owner:** Frontend

---

## 🎯 Objective

Dynamically load tenant-specific branding (logo, colors, title) from API.

---

## 📋 Tasks

### 1. Create Tenant Config API

**File:** `backend/src/main/java/cz/muriel/core/tenant/TenantController.java`

```java
@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    
    @Autowired
    private TenantService tenantService;
    
    @GetMapping("/current")
    public TenantConfigDTO getCurrentTenant(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-ID");
        return tenantService.getTenantConfig(Long.parseLong(tenantId));
    }
}

@Data
public class TenantConfigDTO {
    private Long id;
    private String name;
    private String logo;           // URL to logo
    private String primaryColor;   // #1976d2
    private String secondaryColor; // #dc004e
    private String title;          // "Tenant A Portal"
    private List<String> features; // ["workflows", "reporting"]
}
```

### 2. Create useTenant React Hook

**File:** `frontend/src/hooks/useTenant.ts`

```typescript
import { useQuery } from '@tanstack/react-query';
import { api } from '@/api/client';

interface TenantConfig {
  id: number;
  name: string;
  logo: string;
  primaryColor: string;
  secondaryColor: string;
  title: string;
  features: string[];
}

export const useTenant = () => {
  return useQuery<TenantConfig>({
    queryKey: ['tenant', 'current'],
    queryFn: async () => {
      const { data } = await api.get('/api/tenants/current');
      return data;
    },
    staleTime: 5 * 60 * 1000, // Cache 5 minutes
  });
};
```

### 3. Create TenantProvider Context

**File:** `frontend/src/providers/TenantProvider.tsx`

```tsx
import React, { createContext, useContext } from 'react';
import { useTenant } from '@/hooks/useTenant';

const TenantContext = createContext<TenantConfig | null>(null);

export const TenantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { data: tenant, isLoading } = useTenant();

  if (isLoading) return <LoadingScreen />;
  if (!tenant) return <ErrorScreen />;

  // Apply theme dynamically
  document.documentElement.style.setProperty('--primary-color', tenant.primaryColor);
  document.documentElement.style.setProperty('--secondary-color', tenant.secondaryColor);
  document.title = tenant.title;

  return (
    <TenantContext.Provider value={tenant}>
      {children}
    </TenantContext.Provider>
  );
};

export const useTenantContext = () => {
  const context = useContext(TenantContext);
  if (!context) throw new Error('useTenantContext must be used within TenantProvider');
  return context;
};
```

### 4. Update App.tsx

```tsx
import { TenantProvider } from '@/providers/TenantProvider';

function App() {
  return (
    <TenantProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          {/* ... */}
        </Routes>
      </Router>
    </TenantProvider>
  );
}
```

### 5. Use in Components

```tsx
import { useTenantContext } from '@/providers/TenantProvider';

export const Header: React.FC = () => {
  const tenant = useTenantContext();

  return (
    <AppBar style={{ backgroundColor: tenant.primaryColor }}>
      <Toolbar>
        <img src={tenant.logo} alt={tenant.name} height={40} />
        <Typography variant="h6">{tenant.name}</Typography>
      </Toolbar>
    </AppBar>
  );
};
```

---

## ✅ Acceptance Criteria

- [ ] API endpoint `/api/tenants/current` returns config
- [ ] Frontend loads tenant config on app startup
- [ ] Logo, colors, title change per subdomain
- [ ] Feature flags control visible features
- [ ] Config cached for 5 minutes (reduce API calls)

---

## 🔗 Dependencies

- Requires T1 (Nginx subdomain routing)
- Requires backend tenant service
