# 🔐 Mapování oprávnění rolí v aplikaci

## Přehled
Tento dokument definuje, jaké **backend API endpointy**, **external system přístupy** a **data scope** poskytuje každá role v systému. 

**⚠️ DŮLEŽITÉ:** Tento dokument **NENÍ** o viditelnosti menu položek ve frontend aplikaci. Menu se řídí podle `composedRoles` z JWT tokenu v `AppLayout.jsx`.

## Co tento dokument definuje

✅ **Backend API endpointy** - které REST API volání může role provádět  
✅ **External Systems** - přístup do Grafana, Loki, Keycloak  
✅ **Data Scope** - jaká data role vidí (vlastní tenant vs všechny)  

❌ **NENÍ o tom:**  
❌ Viditelnost menu položek v UI (to řídí composedRoles)  
❌ Frontend routing (React Router guards)  
❌ UI komponenty (ty se renderují podle role dynamicky)

## Core Role - Systémové role

### CORE_ROLE_ADMIN
**Popis:** Nejvyšší úroveň přístupu - správce celého systému

**Backend API oprávnění:**
- ✅ `GET /api/tenants` - Zobrazení všech tenantů
- ✅ `POST /api/tenants` - Vytvoření nového tenantu
- ✅ `PUT /api/tenants/{id}` - Aktualizace tenantu
- ✅ `DELETE /api/tenants/{id}` - Smazání tenantu
- ✅ `GET /api/users` - Všichni uživatelé (napříč tenanty)
- ✅ `POST /api/users` - Vytvoření uživatele v libovolném tenantu
- ✅ `GET /api/roles` - Všechny role (napříč tenanty)
- ✅ `POST /api/roles` - Vytvoření role v libovolném tenantu
- ✅ `DELETE /api/roles/*` - Smazání libovolné role

**External Systems:**
- 🟢 **Grafana:** Admin (všechny workspace včetně admin)
- 🟢 **Loki:** Přístup ke všem logům (všechny tenanty)
- 🟢 **Keycloak:** Admin konzole

**Data Scope:**
- 🌍 Všechny tenanty
- 🌍 Všichni uživatelé
- 🌍 Všechny role

### CORE_ROLE_TENANT_ADMIN
**Popis:** Administrátor konkrétního tenantu

**Backend API oprávnění:**
- ✅ `GET /api/users?tenant={myTenant}` - Uživatelé vlastního tenantu
- ✅ `POST /api/users` - Vytvoření uživatele (scope: vlastní tenant)
- ✅ `PUT /api/users/{id}` - Aktualizace uživatele (scope: vlastní tenant)
- ✅ `GET /api/roles?tenant={myTenant}` - Role vlastního tenantu
- ✅ `POST /api/roles` - Vytvoření role (scope: vlastní tenant)
- ✅ `DELETE /api/roles/{name}` - Smazání role (scope: vlastní tenant)
- ✅ `GET /api/groups?tenant={myTenant}` - Skupiny vlastního tenantu

**External Systems:**
- 🟡 **Grafana:** Editor (pouze tenant workspace)
- 🟡 **Loki:** Přístup k logům vlastního tenantu
- ❌ **Keycloak:** Žádný přístup do admin konzole

**Data Scope:**
- 🏢 Pouze vlastní tenant
- 👥 Pouze uživatelé vlastního tenantu
- 🎭 Pouze role vlastního tenantu

**Omezení:**
- ❌ Nemůže vytvářet nové tenanty
- ❌ Nemůže spravovat uživatele jiných tenantů
- ❌ Nemůže přistupovat k admin tenantu

### CORE_ROLE_USER_MANAGER
**Popis:** Správce uživatelů v tenantu

**Backend API oprávnění:**
- ✅ `GET /api/users?tenant={myTenant}` - Zobrazení uživatelů
- ✅ `POST /api/users` - Vytvoření uživatele (scope: vlastní tenant)
- ✅ `PUT /api/users/{id}` - Aktualizace uživatele (scope: vlastní tenant)
- ✅ `POST /api/users/{id}/roles` - Přiřazení existující role
- ✅ `DELETE /api/users/{id}/roles/{roleName}` - Odebrání role
- ✅ `GET /api/roles?tenant={myTenant}` - Čtení rolí (readonly)

**External Systems:**
- ❌ **Grafana:** Žádný přístup
- ❌ **Loki:** Žádný přístup
- ❌ **Keycloak:** Žádný přístup

**Data Scope:**
- 🏢 Pouze vlastní tenant
- 👥 Pouze uživatelé vlastního tenantu

**Omezení:**
- ❌ Nemůže vytvářet nebo upravovat role
- ❌ Nemůže spravovat tenanty
- ❌ Nemá přístup do Grafany nebo Loki

### CORE_ROLE_USER
**Popis:** Základní uživatel aplikace

**Backend API oprávnění:**
- ✅ `GET /api/users/me` - Zobrazení vlastního profilu
- ✅ `PUT /api/users/me` - Aktualizace vlastního profilu
- ✅ `GET /api/tenants/me` - Informace o svém tenantu

**External Systems:**
- 🟢 **Grafana:** Viewer (omezené dashboardy podle role)
- ❌ **Loki:** Žádný přístup
- ❌ **Keycloak:** Žádný přístup

**Data Scope:**
- 👤 Pouze vlastní data
- 👤 Pouze vlastní profil

**Omezení:**
- ❌ Žádné admin funkce
- ❌ Nemůže spravovat jiné uživatele
- ❌ Nemůže vytvářet nebo upravovat data mimo své scope

## Composite Role - Sdružené role

### Příklad: TENANT_FULL_ACCESS (composite)
**Obsahuje:**
- CORE_ROLE_TENANT_ADMIN
- CORE_ROLE_USER_MANAGER
- CORE_ROLE_USER

**Oprávnění:** Kombinace všech vnořených rolí

## Tenant-specific Role

Každý tenant může mít vlastní role s prefixem `{TENANT_KEY}_ROLE_*`

### Příklad: COMPANY_A_ROLE_MANAGER
**Popis:** Custom role pro tenant company_a

**Oprávnění:**
- Definované administrátorem tenantu
- Scope omezený na tenant company_a

## Grafana Integration Mapping

| Role | Grafana Role | Workspace Access | Co to znamená |
|------|-------------|------------------|---------------|
| CORE_ROLE_ADMIN | Admin | Všechny (včetně admin) | Plná správa včetně vytváření dashboardů a datasources |
| CORE_ROLE_TENANT_ADMIN | Editor | Tenant workspace | Tvorba a úprava dashboardů v tenant workspace |
| CORE_ROLE_USER_MANAGER | - | Žádný přístup | Nemá přístup do Grafany |
| CORE_ROLE_USER | Viewer | Tenant workspace (readonly) | Pouze zobrazení dashboardů |

## Loki (Logging) Integration Mapping

| Role | Loki Access | Log Scope | Poznámka |
|------|------------|-----------|----------|
| CORE_ROLE_ADMIN | Full | Všechny tenanty | Vidí logy všech aplikací a tenantů |
| CORE_ROLE_TENANT_ADMIN | Filtered | Vlastní tenant | Filtrováno podle tenant_key labelu |
| CORE_ROLE_USER_MANAGER | None | Žádný přístup | Nemá přístup k logům |
| CORE_ROLE_USER | None | Žádný přístup | Nemá přístup k logům |

## Frontend Menu Visibility

**⚠️ POZOR:** Menu položky ve frontend aplikaci se **NEŘÍDÍ** tímto dokumentem!

Menu se renderuje dynamicky podle **composedRoles** v JWT tokenu:
- Logika v: `frontend/src/components/layout/AppLayout.jsx`
- Používá: `user.composedRoles` (array rolí včetně zděděných z composite)
- Podmínky: `if (composedRoles.includes('CORE_ROLE_ADMIN')) { ... }`

**Příklady:**
```jsx
// Dashboard - všichni
<MenuItem>Dashboard</MenuItem>

// Správa Keycloak - pouze CORE_ADMIN nebo TENANT_ADMIN
{composedRoles.includes('CORE_ROLE_ADMIN') || composedRoles.includes('CORE_ROLE_TENANT_ADMIN') && (
  <MenuItem>Správa Keycloak</MenuItem>
)}

// Můj profil - všichni
<MenuItem>Můj profil</MenuItem>
```

## Backend API Endpoint Protection

Každý endpoint je chráněn Spring Security annotations:

### Tenant Management
```java
@PreAuthorize("hasRole('CORE_ROLE_ADMIN')")
@GetMapping("/api/tenants")
```

### User Management
```java
@PreAuthorize("hasAnyRole('CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_USER_MANAGER')")
@GetMapping("/api/users")
// + tenant scope filter v metodě
```

### Role Management
```java
@PreAuthorize("hasAnyRole('CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN')")
@GetMapping("/api/roles")
// + tenant scope filter v metodě
```

### Self-service
```java
@PreAuthorize("isAuthenticated()")
@GetMapping("/api/users/me")
// Každý přihlášený uživatel
```

## Backend API Endpoints - Kompletní seznam

| Endpoint | HTTP Method | Required Role(s) | Tenant Scope |
|----------|-------------|------------------|--------------|
| `/api/tenants` | GET | CORE_ROLE_ADMIN | All |
| `/api/tenants` | POST | CORE_ROLE_ADMIN | N/A |
| `/api/tenants/{id}` | PUT | CORE_ROLE_ADMIN | All |
| `/api/tenants/{id}` | DELETE | CORE_ROLE_ADMIN | All |
| `/api/users` | GET | ADMIN, TENANT_ADMIN, USER_MANAGER | Filtered by tenant |
| `/api/users` | POST | ADMIN, TENANT_ADMIN, USER_MANAGER | Filtered by tenant |
| `/api/users/{id}` | PUT | ADMIN, TENANT_ADMIN, USER_MANAGER | Filtered by tenant |
| `/api/users/{id}` | DELETE | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/users/me` | GET | Any authenticated | Own data |
| `/api/users/me` | PUT | Any authenticated | Own data |
| `/api/roles` | GET | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles` | POST | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles/{name}` | PUT | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles/{name}` | DELETE | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles/{name}/composites` | GET | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles/{name}/composites` | POST | ADMIN, TENANT_ADMIN | Filtered by tenant |
| `/api/roles/{name}/users` | GET | ADMIN, TENANT_ADMIN | Filtered by tenant |

## Impact Analysis při smazání role

### Pokud smažete CORE_ROLE_ADMIN:
⚠️ **KRITICKÉ:**
- Ztráta přístupu do admin tenantu
- Nikdo nebude moci spravovat tenanty
- Ztráta Grafana Admin přístupu
- Systém může přestat být spravovatelný

### Pokud smažete CORE_ROLE_TENANT_ADMIN:
⚠️ **VYSOKÝ DOPAD:**
- Tenant admins ztratí přístup ke správě tenantu
- Nemožnost spravovat uživatele v tenantu
- Ztráta Grafana Editor přístupu

### Pokud smažete CORE_ROLE_USER_MANAGER:
⚠️ **STŘEDNÍ DOPAD:**
- User manageři ztratí možnost spravovat uživatele
- Administrace uživatelů pouze pro TENANT_ADMIN a CORE_ADMIN

### Pokud smažete CORE_ROLE_USER:
⚠️ **NÍZKÝ DOPAD (ale důležité):**
- Uživatelé ztratí základní přístup
- Nutné přiřadit jinou roli

## Best Practices

### ✅ Doporučené:
1. **Nikdy nemažte core role** (`CORE_ROLE_*`) bez velmi dobrého důvodu
2. Vytvářejte **custom tenant role** pro specifické potřeby
3. Používejte **composite role** pro snadnější správu
4. Pravidelně **auditujte přiřazení rolí**
5. Používejte **least privilege principle** - dávejte minimální nutná oprávnění

### ❌ Nedoporučené:
1. Mazání core rolí
2. Přímé přiřazování ADMIN role běžným uživatelům
3. Vytváření rolí bez jasného účelu
4. Příliš granulární role (lepší použít composite)

## Změny a historie

### 2025-10-08
- ✅ Implementován tabbovaný detail role s přehledem oprávnění
- ✅ Přidána "Nebezpečná zóna" s varováním před smazáním
- ✅ Zobrazení impact analysis při smazání role

## ℹ️ Jak zjistit, které menu vidí uživatel s danou rolí?

**Odpověď:** Podívejte se do `frontend/src/components/layout/AppLayout.jsx`

Menu položky se renderují podle logiky:
```jsx
const composedRoles = user?.composedRoles || [];

// Příklad - Dashboard vidí všichni
<MenuItem component={Link} to="/dashboard">
  Dashboard
</MenuItem>

// Správa Keycloak - pouze ADMIN a TENANT_ADMIN
{(composedRoles.includes('CORE_ROLE_ADMIN') || 
  composedRoles.includes('CORE_ROLE_TENANT_ADMIN')) && (
  <MenuItem>Správa Keycloak</MenuItem>
)}

// Správa uživatelů - ADMIN, TENANT_ADMIN, USER_MANAGER
{(composedRoles.includes('CORE_ROLE_ADMIN') || 
  composedRoles.includes('CORE_ROLE_TENANT_ADMIN') ||
  composedRoles.includes('CORE_ROLE_USER_MANAGER')) && (
  <MenuItem>Uživatelé</MenuItem>
)}
```

**Klíčové:**
- Používá se `composedRoles` (obsahuje i zděděné role z composite)
- Logika je přímo v JSX kódu AppLayout
- Není centralizovaný mapping (každá MenuItem má vlastní podmínku)

### Budoucí rozšíření
- [ ] API endpoint pro získání všech permissions pro roli
- [ ] Dynamické načítání permissions z backendu
- [ ] Permission builder v UI
- [ ] Role templates pro rychlé vytvoření
- [ ] Import/Export rolí mezi tenanty
- [ ] Centralizovaný menu visibility mapping (aby nebylo v JSX)
