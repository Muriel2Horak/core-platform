# Admin UI - Verifikační test

**Datum:** 7. října 2025  
**Účel:** Ověření oprav všech problémů z ADMIN_UI_ISSUES_AND_FIXES.md

## ✅ Test 1: Vytvoření tenantu (Problém #12)

### Postup:
1. Přihlásit se jako admin
2. Přejít do Admin → Tenants
3. Kliknout "Create Tenant"
4. Vyplnit:
   - Key: `test-company`
   - Display Name: `Test Company s.r.o.`
5. Kliknout "Create"

### Očekávaný výsledek:
- ✅ Tenant vytvořen bez 403 chyby
- ✅ Nový realm vytvořen v Keycloak
- ✅ Tabulka tenantů se automaticky obnoví
- ✅ Nový tenant je viditelný v seznamu

### Status: **OPRAVENO** ✅ (7.10.2025)

---

## ⏳ Test 2: Auto-refresh po editaci (Problém #1)

### Role Management:
1. Vytvořit novou roli → **Zkontrolovat:** Tabulka se obnoví ✅
2. Upravit roli → **Zkontrolovat:** Změny se zobrazí ✅
3. Smazat roli → **Zkontrolovat:** Role zmizí ze seznamu ✅

### User Management:
1. Vytvořit uživatele → **Zkontrolovat:** Tabulka se obnoví ⏳
2. Upravit uživatele → **Zkontrolovat:** Změny se zobrazí ⏳
3. Smazat uživatele → **Zkontrolovat:** Uživatel zmizí ⏳

### Tenant Management:
1. Vytvořit tenant → **Zkontrolovat:** Tabulka se obnoví ⏳
2. Upravit tenant → **Zkontrolovat:** Změny se zobrazí ⏳
3. Smazat tenant → **Zkontrolovat:** Tenant zmizí ⏳

### Status: **ČÁSTEČNĚ (Role ✅, zbytek ⏳)**

---

## ⏳ Test 3: Kliknutí na řádek otevře detail (Problém #2)

### Postup:
1. **Roles:** Kliknout na řádek role → **Očekáváno:** Otevře EditRoleDialog ✅
2. **Users:** Kliknout na řádek uživatele → **Očekáváno:** Otevře EditUserDialog ⏳
3. **Tenants:** Kliknout na řádek tenantu → **Očekáváno:** Otevře EditTenantDialog ✅

### Status: **ČÁSTEČNĚ (Role ✅, Tenants ✅, Users ⏳)**

---

## ❌ Test 4: Composite role management (Problém #3, 4)

### Create Composite Role:
1. Kliknout "Create Role"
2. Zaškrtnout "Composite"
3. **Očekáváno:** Zobrazí se CompositeRoleBuilder ❌
4. Vybrat child roles
5. Kliknout "Create"

### Edit to Composite:
1. Upravit existing simple role
2. Změnit na composite
3. **Očekáváno:** Zobrazí se warning pokud má child roles ❌
4. Potvrdit změnu

### Status: **NEOPRAVENO** ❌

---

## ❌ Test 5: Zobrazit tenant u rolí/uživatelů (Problém #5)

### Postup:
1. Otevřít Roles tabulku
2. **Očekáváno:** Vidím sloupec "Tenant" s tenant key ❌

3. Otevřít Users tabulku
4. **Očekáváno:** Vidím sloupec "Tenant" s tenant key ❌

### Status: **NEOPRAVENO** ❌

---

## ❌ Test 6: Manager hierarchy (Problém #6)

### Postup:
1. Upravit uživatele
2. **Očekáváno:** Vidím pole "Manager" s autocomplete ❌
3. Vybrat managera ze seznamu
4. Uložit
5. **Očekáváno:** Manager se zobrazí v user detailu ❌

### Status: **NEOPRAVENO** ❌

---

## ❌ Test 7: Org chart (Problém #7)

### Postup:
1. Přejít do Users
2. **Očekáváno:** Vidím tlačítko "Org Chart" nebo "Organization" ❌
3. Kliknout na tlačítko
4. **Očekáváno:** Zobrazí se hierarchický tree view ❌

### Status: **NEOPRAVENO** ❌

---

## ❌ Test 8: Menu structure (Problém #8)

### Postup:
1. Otevřít hlavní menu (sidebar)
2. **Očekáváno:** Admin sekce je na root level, ne zanořená ❌

### Aktuální struktura:
```
Dashboard
Directory
├── Users (?)
Admin (?)
├── Users (?)
├── Roles (?)
├── Tenants (?)
```

### Očekávaná struktura:
```
Dashboard
Directory
Users (Admin)
Roles (Admin)
Tenants (Admin)
Security (Admin)
Audit (Admin)
```

### Status: **NEOPRAVENO** ❌

---

## ⏳ Test 9: Search vrací 500 (Problém #9)

### Postup:
1. Otevřít User Directory
2. Zadat search query do vyhledávacího pole
3. **Očekáváno:** Výsledky bez 500 chyby ⏳

### Backend endpoint:
- `GET /api/users?username=xxx` ✅ Existuje
- `GET /api/users-directory?q=xxx` ✅ Existuje

### Status: **PRAVDĚPODOBNĚ OPRAVENO** ⏳

---

## ❌ Test 10: Tenant filtering (Problém #10)

### Postup (jako admin):
1. Otevřít User Directory
2. **Očekáváno:** Vidím tenant filter dropdown ❌
3. Vybrat tenant
4. **Očekáváno:** Zobrazí se pouze uživatelé z daného tenantu ❌

### Postup (jako tenant user):
1. Otevřít User Directory
2. **Očekáváno:** Vidím pouze uživatele ze svého tenantu ⏳

### Status: **NEOPRAVENO** ❌

---

## ❌ Test 11: Assign users to role (Problém #11)

### Postup:
1. Otevřít Roles
2. Kliknout na role menu (3 dots)
3. **Očekáváno:** Vidím "Assign Users" akci ❌
4. Kliknout "Assign Users"
5. **Očekáváno:** Otevře se dialog s multi-select uživatelů ❌
6. Vybrat uživatele a uložit

### Status: **NEOPRAVENO** ❌

---

## ⏳ Test 13: Fialový dialog (Problém #13)

### Postup:
1. Kliknout "Create Tenant"
2. **Očekáváno:** Dialog má glassmorphic design (bílý s blur) ⏳

### Aktuální v kódu:
```jsx
PaperProps={{
  sx: {
    background: 'rgba(255, 255, 255, 0.95)',
    backdropFilter: 'blur(10px)',
    borderRadius: 2,
  }
}}
```

### Status: **V KÓDU OPRAVENO, POTŘEBA OVĚŘIT** ⏳

---

## 📊 Celkový status

| Problém | Popis | Status | Priorita |
|---------|-------|--------|----------|
| #12 | Nelze vytvořit tenant (403) | ✅ OPRAVENO | P1 |
| #9 | Search vrací 500 | ⏳ POTŘEBA TEST | P1 |
| #1 | Chybí refresh po editaci | ⏳ ČÁSTEČNĚ | P1 |
| #2 | Click na row neotevře detail | ⏳ ČÁSTEČNĚ | P2 |
| #13 | Fialový dialog | ⏳ POTŘEBA TEST | P2 |
| #5 | Tenant column chybí | ❌ NEOPRAVENO | P2 |
| #10 | Tenant filtering chybí | ❌ NEOPRAVENO | P2 |
| #3,4 | Composite role management | ❌ NEOPRAVENO | P3 |
| #6 | Manager assignment | ❌ NEOPRAVENO | P3 |
| #7 | Org chart | ❌ NEOPRAVENO | P3 |
| #8 | Menu structure | ❌ NEOPRAVENO | P3 |
| #11 | Assign users to role | ❌ NEOPRAVENO | P3 |

### Počet oprav:
- ✅ Hotovo: 1
- ⏳ Potřeba test: 5
- ❌ Neopraveno: 6

### Doporučení:
1. **Nejdřív otestovat** všechny ⏳ položky
2. **Pak opravit** P2 priority (tenant column, filtering)
3. **Nakonec** P3 features (composite roles, org chart, manager)
