# 📝 Aktualizace Permission Mapping dokumentace

**Datum:** 8. října 2025  
**Důvod:** Uživatelská nejasnost ohledně významu "permissions"

## ❌ Původní problém

Uživatel se zeptal:
> "Není mi jasné to mapování permission na role. Dozvím se z toho že role ROLE_CORE_USER vidí adresář s svůj profil a role TENANT management vidí zase něco v menu Administrace?"

**Analýza:**
- ❌ Dokumentace byla **matoucí**
- ❌ Směšovala **Backend API** s **Frontend UI**
- ❌ Nebylo jasné, že menu se řídí `composedRoles` v AppLayout, ne tímto dokumentem
- ❌ "Frontend přístup" sekce byla **misleading**

## ✅ Co bylo opraveno

### 1. Jasné rozlišení scope dokumentu

**Nová úvodní sekce:**
```markdown
## Co tento dokument definuje

✅ Backend API endpointy - které REST API volání může role provádět  
✅ External Systems - přístup do Grafana, Loki, Keycloak  
✅ Data Scope - jaká data role vidí (vlastní tenant vs všechny)  

❌ NENÍ o tom:  
❌ Viditelnost menu položek v UI (to řídí composedRoles)  
❌ Frontend routing (React Router guards)  
❌ UI komponenty (ty se renderují podle role dynamicky)
```

### 2. Přepsané permission listy

**PŘED (matoucí):**
```markdown
**Oprávnění:**
- ✅ Přístup do admin tenantu
- ✅ Správa všech tenantů
- ✅ Grafana Admin přístup

**Frontend přístup:**
- Dashboard (admin tenant)
- Správa tenantů
- Grafana Admin UI
```

**PO (jasné):**
```markdown
**Backend API oprávnění:**
- ✅ GET /api/tenants - Zobrazení všech tenantů
- ✅ POST /api/tenants - Vytvoření nového tenantu
- ✅ PUT /api/tenants/{id} - Aktualizace tenantu

**External Systems:**
- 🟢 Grafana: Admin (všechny workspace včetně admin)
- 🟢 Loki: Přístup ke všem logům (všechny tenanty)

**Data Scope:**
- 🌍 Všechny tenanty
- 🌍 Všichni uživatelé
```

### 3. Přidána sekce o Frontend menu

**Nová sekce:**
```markdown
## Frontend Menu Visibility

⚠️ POZOR: Menu položky ve frontend aplikaci se NEŘÍDÍ tímto dokumentem!

Menu se renderuje dynamicky podle composedRoles v JWT tokenu:
- Logika v: frontend/src/components/layout/AppLayout.jsx
- Používá: user.composedRoles (array rolí včetně zděděných)
- Podmínky: if (composedRoles.includes('CORE_ROLE_ADMIN')) { ... }

## ℹ️ Jak zjistit, které menu vidí uživatel?

Odpověď: Podívejte se do AppLayout.jsx
```

### 4. Rozšířená Backend API tabulka

**Nová kompletní tabulka:**
```markdown
| Endpoint | HTTP Method | Required Role(s) | Tenant Scope |
|----------|-------------|------------------|--------------|
| /api/tenants | GET | CORE_ROLE_ADMIN | All |
| /api/users | GET | ADMIN, TENANT_ADMIN, USER_MANAGER | Filtered |
| /api/users/me | GET | Any authenticated | Own data |
| /api/roles/{name}/composites | POST | ADMIN, TENANT_ADMIN | Filtered |
...
```

### 5. Aktualizované permission v RoleDetailDialog

**V kódu (`RoleDetailDialog.jsx`):**
```jsx
const rolePermissions = {
  'CORE_ROLE_ADMIN': [
    'API: Přístup ke všem /api/* endpointům',
    'API: Správa tenantů (POST/PUT/DELETE /api/tenants)',
    'Grafana: Admin úroveň (všechny workspace)',
    'Loki: Přístup ke všem logům (všechny tenanty)',
    'Keycloak: Admin konzole',
  ],
  'CORE_ROLE_USER': [
    'API: Čtení vlastního profilu (GET /api/users/me)',
    'API: Aktualizace vlastního profilu (PUT /api/users/me)',
    'Grafana: Viewer úroveň (omezené dashboardy)',
    'Data scope: Pouze vlastní data',
  ],
};
```

**Nyní je jasné:**
- ✅ "API:" = Backend REST endpoint
- ✅ "Grafana:" = Externí systém přístup
- ✅ "Data scope:" = Rozsah viditelných dat
- ✅ NENÍ to o menu položkách!

## 📊 Porovnání - Before/After

### CORE_ROLE_USER permissions

**PŘED (nejasné):**
```
Oprávnění:
- ✅ Základní přístup do aplikace
- ✅ Zobrazení vlastního profilu
- ✅ Grafana Viewer přístup

Frontend přístup:
- Dashboard (základní view)
- Můj profil
- Grafana Viewer UI (omezený)
```
👎 Co je "základní přístup"? Co je "základní view"?

**PO (jasné):**
```
Backend API oprávnění:
- ✅ GET /api/users/me - Zobrazení vlastního profilu
- ✅ PUT /api/users/me - Aktualizace vlastního profilu
- ✅ GET /api/tenants/me - Informace o svém tenantu

External Systems:
- 🟢 Grafana: Viewer (omezené dashboardy podle role)

Data Scope:
- 👤 Pouze vlastní data
- 👤 Pouze vlastní profil
```
👍 Přesné HTTP metody a endpointy!

## 📁 Změněné soubory

1. **`docs/ROLE_PERMISSIONS_MAPPING.md`**
   - Přepsán celý dokument s jasnou strukturou
   - Odstraněna sekce "Frontend přístup"
   - Přidána sekce "Frontend Menu Visibility" s vysvětlením
   - Rozšířená Backend API tabulka
   - Přidány příklady Spring Security annotations

2. **`frontend/src/components/Roles/RoleDetailDialog.jsx`**
   - Aktualizován `rolePermissions` objekt
   - Nové formátování: "API:", "Grafana:", "Data scope:"
   - Přesnější popis (HTTP metody, endpoint paths)

## 🎯 Výsledek

### Pro uživatele:
- ✅ **Jasné pochopení** co role umožňuje z pohledu API
- ✅ **Explicitní rozdělení** Backend vs External Systems vs Data
- ✅ **Návod** kde hledat info o menu (AppLayout.jsx)

### Pro vývojáře:
- ✅ **Přesná specifikace** endpoint permissions
- ✅ **Dokumentované** Spring Security annotations
- ✅ **Jasný scope** každého endpointu (All tenants vs Filtered)

## 📝 Doporučení pro budoucnost

### 1. Centralizovaný Menu Config
```javascript
// frontend/src/config/menuPermissions.js
export const MENU_ITEMS = {
  dashboard: {
    label: 'Dashboard',
    path: '/dashboard',
    requiredRoles: [], // všichni
  },
  users: {
    label: 'Uživatelé',
    path: '/users',
    requiredRoles: ['CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_USER_MANAGER'],
  },
  roles: {
    label: 'Role',
    path: '/roles',
    requiredRoles: ['CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN'],
  },
};
```

Pak v AppLayout:
```jsx
{MENU_ITEMS.users.requiredRoles.some(r => composedRoles.includes(r)) && (
  <MenuItem component={Link} to={MENU_ITEMS.users.path}>
    {MENU_ITEMS.users.label}
  </MenuItem>
)}
```

### 2. Permission Display Endpoint
```java
// Backend
@GetMapping("/api/roles/{name}/permissions")
public PermissionDto getRolePermissions(@PathVariable String name) {
  return PermissionDto.builder()
    .apiEndpoints(List.of(
      ApiPermission.builder()
        .method("GET")
        .path("/api/users/me")
        .description("Zobrazení vlastního profilu")
        .build()
    ))
    .externalSystems(Map.of(
      "grafana", "Viewer",
      "loki", "None"
    ))
    .dataScope("Own data only")
    .menuItems(List.of("Dashboard", "Můj profil"))
    .build();
}
```

### 3. UI Permission Viewer
V RoleDetailDialog by se mohlo zobrazovat:
```jsx
<Tabs>
  <Tab label="API Permissions" />
  <Tab label="External Systems" />
  <Tab label="Menu Items" />  // Nový tab!
  <Tab label="Data Scope" />
</Tabs>
```

## 🎉 Závěr

Dokumentace permissions byla **kompletně přepsána** pro jasnost:

**Změny:**
- ✅ Jasné rozlišení Backend API vs Frontend UI
- ✅ Přesné HTTP metody a endpoint paths
- ✅ Vysvětlení kde hledat menu visibility
- ✅ Rozšířené příklady a tabulky

**Uživatelská hodnota:**
- 🎯 Eliminována confusion
- 📚 Jasná dokumentace
- 🔍 Snadné hledání informací

**Next steps:**
- [ ] Implementovat centralizovaný menu config
- [ ] Vytvořit backend endpoint pro permissions
- [ ] Přidat UI pro zobrazení menu items podle role
