# 🔍 Debugging Guide - Atributy rolí v tabulce

## 📊 Stav implementace

### ✅ Backend - HOTOVO
1. **RoleDto** - přidáno pole `attributes: Map<String, Object>`
2. **KeycloakAdminService** - všechny metody načítají atributy z Keycloaku:
   - `getAllRoles()` ✅
   - `getRoleByName()` ✅
   - `createRole()` ✅
   - `getRoleCompositesList()` ✅
   - `getRolesByTenant()` ✅

### ✅ Frontend - HOTOVO  
1. **Sloupce v tabulce**:
   - Název role ✅
   - Popis ✅
   - **Tenant** ✅ (pro CORE_ADMIN)
   - **Typ** ✅ (Composite/Basic)
   - **Uživatelé** ✅ (počet uživatelů s rolí)
   - **Akce** ✅ (menu s akcemi)

## ❌ Problém - Data se nezobrazují

### Možné příčiny:

1. **Backend error 500** - při volání `/api/roles` 
   - Log ukazuje: `Malformed token` a `Missing dot delimiter(s)`
   - Token není správně předáván nebo parsován

2. **Frontend** - může volat nesprávný endpoint
   - Pro CORE_ADMIN: `/api/admin/roles/tenant/${tenantKey}` 
   - Pro ostatní: `/api/roles`

## 🔧 Debugging kroky:

### 1. Zkontrolovat browser console (F12)

```javascript
// V browser console spustit:
console.log('Token:', localStorage.getItem('auth_token'));
```

### 2. Zkontrolovat Network tab
- Otevřít DevTools (F12) → Network
- Načíst stránku "Správa Keycloak" → "Role"
- Najít požadavek na `/api/roles` nebo `/api/admin/roles/tenant/admin`
- Zkontrolovat:
  - Request Headers → Authorization header
  - Response → Status code a data

### 3. Test API přímo

Otevřít test soubor:
```bash
open /Users/martinhorak/Projects/core-platform/test-roles-api.html
```

Kliknout na "Test API" a zkontrolovat výstup.

### 4. Zkontrolovat backend logy

```bash
docker logs core-backend --tail 100 | grep -i "role\|error\|exception"
```

## 🎯 Co funguje správně:

1. ✅ Backend zkompilován bez chyb
2. ✅ Frontend zkompilován bez chyb  
3. ✅ Endpointy existují:
   - `GET /api/roles`
   - `GET /api/admin/roles/tenant/{tenantKey}`
4. ✅ UI komponenty jsou správně implementované
5. ✅ Keycloak atributy rolí se načítají do RoleDto

## 🐛 Co NEFUNGUJE:

1. ❌ Token autentikace - backend hlásí "Malformed token"
2. ❌ Data se nezobrazují v tabulce

## 💡 Řešení:

### Možnost 1: Problém s tokenem
- Zkontrolovat, jestli se token správně ukládá a načítá
- Ověřit, že Keycloak vrací validní JWT token

### Možnost 2: CORS / Proxy problém
- Zkontrolovat nginx konfiguraci
- Ověřit, že proxy předává správné headers

### Možnost 3: Frontend nepředává token
- Zkontrolovat axios interceptor v `api.js`
- Ověřit, že `setToken()` se volá po přihlášení

## 🔍 Další kroky:

1. Přihlásit se do aplikace jako `test_admin`
2. Otevřít Browser DevTools (F12)
3. Přejít na "Správa Keycloak" → "Role"
4. V Console zkontrolovat logy začínající "📥", "📦", "✅" nebo "❌"
5. V Network tab najít požadavek na `/api/roles` nebo `/api/admin/roles/tenant/admin`
6. Zkontrolovat Response - pokud je chyba, zkopírovat celou response
7. Zkopírovat také Request Headers, zejména `Authorization` header

## 📝 Co hlásit:

Pokud problém přetrvává, potřebuji:
1. Screenshot Browser Console (všechny logy)
2. Screenshot Network tab - požadavek na /api/roles
3. Screenshot Response z tohoto požadavku
4. Output z backend logů: `docker logs core-backend --tail 50`
