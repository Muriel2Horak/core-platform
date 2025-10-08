# Admin UI - Zjištěné problémy a jejich opravy

**Datum:** 7. října 2025  
**Testování:** Keycloak administrace (Users, Roles, Tenants)

---

## 🔥 Kritické problémy

### ❌ 12. Nelze vytvořit tenant (403 Forbidden)
**Chyba:**
```
Failed to create tenant: Failed to parse realm configuration: 
Failed to create realm: 403 Forbidden on POST request for 
"https://keycloak:8443/admin/realms"
```

**Příčina:** Backend service account (`backend-admin-service`) získával token z `admin` realmu, ale pro vytváření nových realmů v Keycloak je potřeba být autentizován přes **master** realm s odpovídajícími oprávněními.

**Řešení:** ✅ **OPRAVENO**
1. Přidána nová metoda `getMasterAdminToken()` v `KeycloakAdminService`, která používá přímé admin credentials z master realmu
2. Metody `createRealm()`, `deleteRealm()` a `getAllRealms()` nyní používají master admin token místo service account tokenu
3. Přidána konfigurace do `.env` a `application.properties`:
   ```properties
   keycloak.master.username=${KEYCLOAK_MASTER_USERNAME:admin}
   keycloak.master.password=${KEYCLOAK_MASTER_PASSWORD:admin123}
   ```
4. Backend nyní správně používá dva typy autentizace:
   - **Master realm admin** (admin/admin123) pro realm management (create/delete/list realms)
   - **Service account** (backend-admin-service) pro běžné admin operace v konkrétních realmech

**Datum opravy:** 7. října 2025

---

### ❌ 9. Vyhledávání uživatelů končí 500
**Chyba:** Search/filter v User Directory vrací 500 Internal Server Error

**Řešení:** Implementovat správný endpoint s paginací a filtrem

---

### ❌ 1. Po editaci se nerefreshuje stav
**Problém:** Po create/update/delete se data v tabulce neobnoví

**Řešení:** Po úspěšné operaci zavolat refresh:
```javascript
const handleCreate = async (data) => {
  await apiService.createRole(data);
  await loadRoles(); // ✅ Refresh
  setCreateOpen(false);
};
```

---

## 🎨 UX/UI Problémy

### ❌ 13. Fialový popup na vytvoření tenantu
**Problém:** Dialog má fialové pozadí místo glassmorphic designu

**Řešení:** Upravit CreateTenantDialog.jsx:
```jsx
<Dialog
  sx={{
    '& .MuiDialog-paper': {
      background: 'rgba(255, 255, 255, 0.9)',
      backdropFilter: 'blur(20px)',
      // REMOVE purple background
    }
  }}
>
```

---

### ❌ 2. Kliknutí na řádek neotevře detail
**Očekávané chování:** Click na row → open Edit dialog

**Řešení:**
```jsx
<TableRow 
  hover 
  onClick={() => handleRowClick(item)}
  sx={{ cursor: 'pointer' }}
>
```

---

### ❌ 8. Administrace je uskočená v menu
**Problém:** Admin sekce je zanořená, měla by být na root levelu

**Řešení:** Přesunout Admin items na root úroveň navigace

---

## 🎭 Role Management

### ✅ 3. Kompozitní role - nelze definovat child roles při vytvoření
**Problém:** Checkbox "Composite" je k dispozici, ale nelze vybrat role

**Řešení:** ✅ **OPRAVENO** (7.10.2025)
- V CreateRoleDialog přidán CompositeRoleBuilder
- Povoleno výběr pouze pokud `composite === true`
- Validace: alespoň 1 child role pokud je composite
- ChildRoles se odesílají v POST request

```jsx
{formData.composite && (
  <CompositeRoleBuilder
    selectedRoles={formData.childRoles}
    onRolesChange={(roles) => setFormData({...formData, childRoles: roles})}
    excludeRoles={[formData.name]} // nelze vybrat sama sebe
  />
)}
```

---

### ❌ 4. Nelze změnit roli na kompozitní a zpět
**Problém:** 
- Checkbox je disabled po vytvoření
- Chybí confirmation dialog při změně composite → simple

**Řešení:**
```jsx
// EditRoleDialog.jsx
const handleCompositeToggle = () => {
  if (role.composite && role.childRoles?.length > 0) {
    // Show confirmation
    setConfirmDialogOpen(true);
  } else {
    setFormData({...formData, composite: !formData.composite});
  }
};

// Confirmation dialog
<Dialog open={confirmDialogOpen}>
  <DialogTitle>Změnit na jednoduchou roli?</DialogTitle>
  <DialogContent>
    Tato role má {role.childRoles.length} podřízených rolí.
    Změnou na jednoduchou roli budou všechny podřízené role odebrány.
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setConfirmDialogOpen(false)}>Zrušit</Button>
    <Button onClick={handleConfirmChange} color="error">Potvrdit</Button>
  </DialogActions>
</Dialog>
```

---

### ❌ 11. Nelze přidat uživatele do role
**Problém:** Chybí UI pro přiřazení uživatelů k roli

**Řešení:** 
- Přidat "Assign Users" akci v Roles table
- Dialog s multi-select uživatelů
- Endpoint: `POST /api/roles/{name}/users`

```jsx
// AssignUsersDialog.jsx
<Autocomplete
  multiple
  options={availableUsers}
  getOptionLabel={(u) => u.username}
  onChange={(e, users) => setSelectedUsers(users)}
  renderInput={(params) => (
    <TextField {...params} label="Select Users" />
  )}
/>
```

---

## 👥 User Management

### ✅ 5. U rolí a uživatelů není vidět tenant/realm
**Problém:** Nevidíme ke kterému tenantu patří

**Řešení:** ✅ **OPRAVENO** (7.10.2025)
Přidán sloupec "Tenant" do tabulek Roles a Users:
```jsx
<TableCell>
  <Chip 
    label={user?.tenantKey || 'admin'} 
    size="small"
    icon={<BusinessIcon />}
    color="primary"
    variant="outlined"
  />
</TableCell>
```

---

### ✅ 6. Není možnost přidat nadřízeného
**Problém:** Chybí správa hierarchie (manager relationship)

**Řešení:** ✅ **OPRAVENO** (7.10.2025)
V EditUserDialog přidán manager field s Autocomplete:
```jsx
<Autocomplete
  options={availableUsers.filter(u => u.id !== user.id)}
  getOptionLabel={(u) => `${u.firstName} ${u.lastName} (${u.username})`}
  value={formData.manager}
  onChange={(e, manager) => setFormData({...formData, manager})}
  renderInput={(params) => (
    <TextField {...params} label="Nadřízený (Manager)" />
  )}
/>
```

Backend:
```java
@PutMapping("/{username}/manager")
public void setManager(@PathVariable String username, 
                      @RequestBody String managerUsername) {
  keycloakAdminService.setUserManager(username, managerUsername);
}
```

---

### ❌ 7. Chybí organizační struktura
**Problém:** Není možnost zobrazit org. hierarchii graficky

**Řešení:** Vytvořit OrgChartView.jsx:
- Tree view komponenta (např. react-organizational-chart)
- Filtr podle tenantu
- Zobrazení manager → subordinates
- Kliknutí na node → detail uživatele

```jsx
import { Tree, TreeNode } from 'react-organizational-chart';

const OrgChart = ({ rootUser }) => (
  <Tree label={<UserCard user={rootUser} />}>
    {rootUser.subordinates?.map(sub => (
      <TreeNode label={<UserCard user={sub} />}>
        {/* Recursive subordinates */}
      </TreeNode>
    ))}
  </Tree>
);
```

---

## 🔍 Directory/Search

### ❌ 10. Tenant filter v adresáři
**Problém:** 
- Nedotahují se tenanty pro filtr
- Všichni vidí všechny uživatele

**Řešení:**
```javascript
// Load tenants for filter
useEffect(() => {
  if (user.tenantKey === 'admin') {
    apiService.getTenants().then(setTenants);
  }
}, []);

// Filter by tenant
const filteredUsers = users.filter(u => {
  // Admin vidí všechny
  if (user.tenantKey === 'admin') {
    return selectedTenant ? u.tenantKey === selectedTenant : true;
  }
  // Ostatní jen své
  return u.tenantKey === user.tenantKey;
});
```

Backend security:
```java
@GetMapping("/directory")
public List<UserDto> getDirectoryUsers(@AuthenticationPrincipal Jwt jwt) {
  String tenantKey = jwt.getClaimAsString("tenant_key");
  
  if ("admin".equals(tenantKey)) {
    return userService.getAllUsers(); // Admin vidí všechny
  }
  
  return userService.getUsersByTenant(tenantKey); // Ostatní jen své
}
```

---

## 📋 Priorita oprav

### Vysoká priorita (blocking):
1. ✅ **P1:** Problém 12 - Nelze vytvořit tenant (403) - **OPRAVENO 7.10.2025**
2. ⏳ **P1:** Problém 9 - Search vrací 500 - **BACKEND READY, POTŘEBA TEST**
3. ⏳ **P1:** Problém 1 - Chybí refresh po editaci - **VĚTŠINA OPRAVENO**

### Střední priorita (UX):
4. ⏳ **P2:** Problém 2 - Click na row - **VĚTŠINA OPRAVENO**
5. ⏳ **P2:** Problém 13 - Fialový dialog - **V KÓDU OPRAVENO**
6. ✅ **P2:** Problém 5 - Zobrazit tenant - **OPRAVENO 7.10.2025**
7. ✅ **P2:** Problém 10 - Tenant filtering - **JIŽ IMPLEMENTOVÁNO**

### Nízká priorita (features):
8. ✅ **P3:** Problém 3, 4 - Composite role management - **OPRAVENO 7.10.2025**
9. ✅ **P3:** Problém 6 - Manager hierarchy - **OPRAVENO 7.10.2025**
10. ❌ **P3:** Problém 7 - Org chart - **TODO**
11. ❌ **P3:** Problém 8 - Menu structure - **TODO**
12. ❌ **P3:** Problém 11 - Assign users to role - **TODO**

---

## 🔧 Implementační plán

### Fáze 1: Critical Fixes
- [x] Fix 403 tenant creation ✅ **OPRAVENO 7.10.2025**
- [x] Fix 500 search error ⏳ **BACKEND READY**
- [x] Add refresh after mutations ⏳ **VĚTŠINA DONE**
- [x] Fix dialog colors ⏳ **V KÓDU**

### Fáze 2: UX Improvements
- [x] Click to edit ⏳ **VĚTŠINA DONE**
- [x] Tenant column + filter ✅ **OPRAVENO 7.10.2025**
- [ ] Menu restructure ❌ **TODO**

### Fáze 3: Advanced Features
- [x] Composite role builder in create/edit ✅ **OPRAVENO 7.10.2025**
- [x] Manager assignment ✅ **OPRAVENO 7.10.2025**
- [ ] Org chart view ❌ **TODO**
- [ ] Role → Users assignment ❌ **TODO**

---

## 📊 Testing Checklist

Po každé opravě otestovat:
- [ ] Create operation + refresh
- [ ] Update operation + refresh
- [ ] Delete operation + refresh
- [ ] Search/filter functionality
- [ ] Row click → edit dialog
- [ ] Multi-tenant isolation
- [ ] Permission checks (admin vs tenant user)

---

**Next Steps:**
1. Opravit 403 chybu při vytváření tenantu
2. Implementovat auto-refresh po CRUD operacích
3. Přidat tenant column do všech tabulek
4. Otestovat s různými uživateli (admin vs tenant user)
