Human: # 🔐 Centralizovaný Permission System

**Datum:** 8. října 2025  
**Účel:** Single source of truth pro permissions sdílený mezi Frontend a Backend

## 📋 Přehled

Tento systém řeší problém **synchronizace oprávnění** mezi FE a BE tím, že:
1. Definuje permissions v **jednom YAML souboru** (`backend/src/main/resources/permissions.yml`)
2. Backend poskytuje **API endpoint** pro načtení permissions
3. Frontend používá **usePermissions hook** pro kontrolu oprávnění
4. Obě strany sdílejí **stejnou logiku** a stejná data

## 🎯 Klíčové výhody

✅ **Single Source of Truth** - Permissions definované jednou, použité všude  
✅ **Type-safe** - Jasná struktura `resource:action:scope`  
✅ **Transparent** - Admin vidí co která role může  
✅ **Maintainable** - Změna v YAML = propagace do FE i BE  
✅ **Dynamic** - Menu a UI se renderují podle permissions  
✅ **Testable** - Snadno testovatelné API endpointy a hooks  

## 📁 Struktura projektu

```
backend/
  src/main/resources/
    permissions.yml                 # 📝 Single source of truth
  src/main/java/cz/muriel/core/
    config/
      PermissionConfig.java          # ⚙️ Spring Configuration
    service/
      PermissionService.java         # 🔧 Permission logic
    controller/
      PermissionController.java      # 🌐 API endpoints

frontend/
  src/
    hooks/
      usePermissions.js              # 🪝 React hook
    examples/
      PermissionExamples.jsx         # 📚 Příklady použití
```

## 🔧 Jak to funguje

### 1. Definice permissions (YAML)

```yaml
roles:
  CORE_ROLE_ADMIN:
    api_permissions:
      - "users:read:all"
      - "users:create:all"
      - "users:update:all"
      - "users:delete:all"
      - "tenants:*"  # wildcard = všechny akce
    
    menu_items:
      - id: "users"
        label: "Uživatelé"
        path: "/users"
        icon: "PeopleIcon"
    
    features:
      - "grafana_admin"
      - "export_data"
    
    data_scope: "all_tenants"
```

**Permission format:** `resource:action:scope`
- `resource` - entity (users, roles, tenants, ...)
- `action` - operace (read, create, update, delete, assign, ...)
- `scope` - rozsah (all, tenant, self)

**Příklady:**
- `users:read:all` - Číst všechny uživatele
- `users:create:tenant` - Vytvořit uživatele v tenantu
- `users:update:self` - Upravit sebe
- `roles:*` - Všechny operace s rolemi
- `users:*:tenant` - Všechny operace s uživateli v tenantu

### 2. Backend Service

```java
@Service
public class PermissionService {
    
    // Získá API permissions pro role
    public Set<String> getApiPermissions(List<String> roles);
    
    // Získá menu items pro role
    public List<MenuItem> getMenuItems(List<String> roles);
    
    // Získá features
    public Set<String> getFeatures(List<String> roles);
    
    // Zjistí data scope
    public String getDataScope(List<String> roles);
    
    // Zkontroluje permission (podporuje wildcards)
    public boolean hasPermission(List<String> roles, String permission);
}
```

### 3. API Endpoints

```bash
# Získat permissions aktuálního uživatele
GET /api/permissions/me
Response:
{
  "api_permissions": ["users:read:all", "users:create:all", ...],
  "menu_items": [
    { "id": "users", "label": "Uživatelé", "path": "/users", "icon": "PeopleIcon" }
  ],
  "features": ["grafana_admin", "export_data"],
  "data_scope": "all_tenants"
}

# Získat permissions pro konkrétní roli (admin UI)
GET /api/permissions/roles/CORE_ROLE_ADMIN
Response: { ... }

# Zkontrolovat permission
POST /api/permissions/check
Body: { "permission": "users:read:all" }
Response: { "permission": "users:read:all", "granted": true }
```

### 4. Frontend Hook

```javascript
import { usePermissions } from '../hooks/usePermissions';

function MyComponent() {
  const { 
    can,           // Obecná kontrola: can('users:read:all')
    canRead,       // Helper: canRead('users', 'all')
    canCreate,     // Helper: canCreate('users')
    canUpdate,     // Helper: canUpdate('users')
    canDelete,     // Helper: canDelete('users')
    hasMenu,       // Menu check: hasMenu('users')
    hasFeature,    // Feature check: hasFeature('grafana_admin')
    getDataScope,  // Scope: getDataScope() -> 'all_tenants'
    getMenuItems,  // Menu list
    loading,
  } = usePermissions();
  
  if (loading) return <Loader />;
  
  return (
    <div>
      {canCreate('users') && <Button>Přidat uživatele</Button>}
      {canDelete('users') && <IconButton><DeleteIcon /></IconButton>}
      {hasFeature('export_data') && <Button>Export</Button>}
    </div>
  );
}
```

## 📊 Use Cases

### UC1: Dynamické menu
```jsx
function Navigation() {
  const { getMenuItems } = usePermissions();
  
  return (
    <List>
      {getMenuItems().map(item => (
        <MenuItem key={item.id} to={item.path}>
          {item.label}
        </MenuItem>
      ))}
    </List>
  );
}
```

### UC2: Podmíněné tlačítka
```jsx
function UserActions({ user }) {
  const { canUpdate, canDelete } = usePermissions();
  
  return (
    <>
      {canUpdate('users') && <EditButton user={user} />}
      {canDelete('users') && <DeleteButton user={user} />}
    </>
  );
}
```

### UC3: Page protection
```jsx
function TenantPage() {
  const { hasMenu } = usePermissions();
  
  if (!hasMenu('tenants')) {
    return <Navigate to="/dashboard" />;
  }
  
  return <TenantManagement />;
}
```

### UC4: Granular permissions
```jsx
function UserForm() {
  const { can } = usePermissions();
  
  return (
    <form>
      <TextField name="name" />
      <TextField name="email" />
      
      {/* Tenant selector pouze pro all scope */}
      {can('users:create:all') && (
        <Select name="tenant">...</Select>
      )}
      
      {/* Role assignment pouze s permission */}
      {(can('roles:assign:all') || can('roles:assign:tenant')) && (
        <MultiSelect name="roles">...</MultiSelect>
      )}
    </form>
  );
}
```

### UC5: Backend permission check
```java
@Service
public class UserService {
    @Autowired
    private PermissionService permissionService;
    
    public List<User> getUsers(Authentication auth) {
        List<String> roles = getRoles(auth);
        String dataScope = permissionService.getDataScope(roles);
        
        if ("all_tenants".equals(dataScope)) {
            return userRepository.findAll();
        } else if ("own_tenant".equals(dataScope)) {
            String tenantKey = getTenantKey(auth);
            return userRepository.findByTenantKey(tenantKey);
        } else {
            String userId = getUserId(auth);
            return List.of(userRepository.findById(userId).orElseThrow());
        }
    }
}
```

## 🔄 Workflow: Přidání nového permission

### 1. Přidej do YAML
```yaml
# backend/src/main/resources/permissions.yml
CORE_ROLE_ADMIN:
  api_permissions:
    - "reports:read:all"      # ← NOVÝ
    - "reports:create:all"    # ← NOVÝ
    - "reports:export:all"    # ← NOVÝ
  
  menu_items:
    - id: "reports"           # ← NOVÝ
      label: "Reporty"
      path: "/reports"
      icon: "AssessmentIcon"
  
  features:
    - "advanced_reports"      # ← NOVÝ
```

### 2. Backend automaticky načte (restart)
```bash
# Restartujte backend
docker compose restart backend
```

### 3. Frontend automaticky použije
```jsx
// V komponentě
const { canRead, hasMenu, hasFeature } = usePermissions();

// Menu se zobrazí automaticky (pokud používáte getMenuItems())
{hasMenu('reports') && <MenuItem to="/reports">Reporty</MenuItem>}

// Nebo manuálně
{canRead('reports') && <ReportsPage />}
{hasFeature('advanced_reports') && <AdvancedFilters />}
```

### 4. Backend enforcing (optional)
```java
@PreAuthorize("@permissionService.hasPermission(" +
              "authentication.authorities, 'reports:read:all')")
@GetMapping("/api/reports")
public List<Report> getReports() { ... }
```

## 🎨 Admin UI: Role Detail s Permissions

Aktualizuj `RoleDetailDialog.jsx` pro načítání z API:

```jsx
const [permissionInfo, setPermissionInfo] = useState(null);

useEffect(() => {
  const loadPermissions = async () => {
    const response = await apiService.get(`/api/permissions/roles/${role.name}`);
    setPermissionInfo(response.data);
  };
  
  if (role) loadPermissions();
}, [role]);

// V UI:
<Tab label="Permissions" />

// V tab content:
<Typography variant="h6">API Permissions</Typography>
<List>
  {permissionInfo?.api_permissions.map(perm => (
    <ListItem key={perm}>
      <Chip label={perm} size="small" />
    </ListItem>
  ))}
</List>

<Typography variant="h6">Menu Items</Typography>
<List>
  {permissionInfo?.menu_items.map(item => (
    <ListItem key={item.id}>
      {item.label} ({item.path})
    </ListItem>
  ))}
</List>

<Typography variant="h6">Features</Typography>
<Stack direction="row" spacing={1}>
  {permissionInfo?.features.map(feat => (
    <Chip key={feat} label={feat} color="primary" />
  ))}
</Stack>
```

## 🧪 Testování

### Backend test
```java
@Test
public void testPermissionService() {
    List<String> roles = List.of("CORE_ROLE_ADMIN");
    
    assertTrue(permissionService.hasPermission(roles, "users:read:all"));
    assertTrue(permissionService.hasPermission(roles, "users:create:all"));
    
    // Wildcard
    assertTrue(permissionService.hasPermission(roles, "tenants:anything:all"));
    
    // Data scope
    assertEquals("all_tenants", permissionService.getDataScope(roles));
}
```

### Frontend test
```javascript
test('usePermissions hook', async () => {
  const { result, waitFor } = renderHook(() => usePermissions());
  
  await waitFor(() => !result.current.loading);
  
  expect(result.current.canRead('users')).toBe(true);
  expect(result.current.hasMenu('dashboard')).toBe(true);
  expect(result.current.hasFeature('grafana_admin')).toBe(true);
});
```

## 🚀 Migration Guide

### Stávající kód → Nový systém

**PŘED:**
```jsx
// Roles.jsx
const isCoreAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');

{isCoreAdmin && <Button>Přidat roli</Button>}
```

**PO:**
```jsx
const { canCreate } = usePermissions();

{canCreate('roles') && <Button>Přidat roli</Button>}
```

**PŘED:**
```jsx
// AppLayout.jsx
{composedRoles.includes('CORE_ROLE_ADMIN') && (
  <MenuItem>Správa tenantů</MenuItem>
)}
```

**PO:**
```jsx
const { hasMenu } = usePermissions();

{hasMenu('tenants') && <MenuItem>Správa tenantů</MenuItem>}

// Nebo ještě lépe - dynamicky:
const { getMenuItems } = usePermissions();

{getMenuItems().map(item => (
  <MenuItem key={item.id} to={item.path}>
    {item.label}
  </MenuItem>
))}
```

## 📝 Best Practices

### ✅ DO:
1. **Používej permissions, ne role** - `can('users:read')` místo `isAdmin`
2. **Používej helpers** - `canRead('users')` místo `can('users:read:all')`
3. **Centralizuj menu** - Použij `getMenuItems()` místo hardcoded podmínek
4. **Definuj v YAML** - Všechny permissions do `permissions.yml`
5. **Backend enforce** - Spring Security `@PreAuthorize` s `@permissionService`

### ❌ DON'T:
1. **Nekontroluj role přímo** - `if (role === 'ADMIN')` ❌
2. **Neduplikuj logiku** - FE i BE sdílejí stejná data
3. **Nehard-coduj menu** - Použij dynamic rendering
4. **Nezapomeň na wildcards** - `users:*` podporováno
5. **Nebypassuj permission check** - Vždy kontroluj

## 🎯 Závěr

Tento systém poskytuje:
- ✅ **Centralizovanou konfiguraci** permissions
- ✅ **Transparentní** vztah role → permissions
- ✅ **Synchronizaci** FE ↔ BE
- ✅ **Snadnou údržbu** (změna v jednom místě)
- ✅ **Type-safe** API (`resource:action:scope`)
- ✅ **Dynamic UI** (menu, buttons, features)

**Next steps:**
1. Zkompilovat backend s novými třídami
2. Načíst permissions v App.jsx (provider)
3. Aktualizovat AppLayout.jsx pro dynamic menu
4. Přidat permissions tab v RoleDetailDialog
5. Migrovat stávající permission checks

---

**Poznámka:** Tento systém je inspirován:
- Spring Security Authorities
- AWS IAM Policies
- Kubernetes RBAC
- Auth0 Permissions
