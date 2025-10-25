# Grafana JWT SSO - Debug Summary

**Datum:** 23. října 2025  
**Problém:** Dashboard 404 - uživatel v nesprávné Grafana organizaci  
**Status:** 🔴 Nefunkční - vyžaduje debug user provisioning flow

---

## 🎯 Co řešíme

Snažíme se zprovoznit **Grafana JWT SSO autentizaci** pro multi-tenant prostředí. Uživatelé by se měli dostat do správné Grafana organizace (dle tenantu) a vidět dashboardy bez login stránky.

## 🏗️ Architektura

```
┌──────────┐     ┌───────┐     ┌─────────────┐     ┌─────────┐
│ Frontend │────▶│ Nginx │────▶│ Backend BFF │────▶│ Grafana │
│  iframe  │     │ auth_ │     │ /internal/  │     │ JWT val │
│          │     │request│     │auth/grafana │     │         │
└──────────┘     └───────┘     └─────────────┘     └─────────┘
```

### Authentication Flow:

1. **Frontend**: Uživatel přihlášen přes Keycloak (realm `admin`, user `test_admin`)
2. **Frontend**: Zobrazí iframe s Grafana URL:  
   `/core-admin/monitoring/d/axiom_sys_overview/system-overview?orgId=2`
3. **Nginx**: Zachytí request, volá `auth_request /_auth/grafana`
4. **Backend** endpoint `/internal/auth/grafana` (`AuthRequestController`):
   - Extrahuje Keycloak JWT z cookie `at`
   - Resolvuje tenant → Grafana Org (admin → Org 2)
   - Mintuje Grafana JWT s `orgId=2` claimem (RS256)
   - Vrací hlavičky: `Grafana-Jwt`, `X-Grafana-Org-Id: 2`
5. **Nginx**: Předá hlavičky do Grafany jako `X-Org-JWT`, `X-Grafana-Org-Id`
6. **Grafana**: Validuje JWT přes JWKS endpoint a zobrazí dashboard

---

## ✅ Co funguje

### Backend
- ✅ Backend **správně resolvuje** uživatele:
  ```
  Log: "✅ Resolved user test_admin to Grafana org 2"
  ```
- ✅ Backend mintuje **JWT s orgId=2** claimem
- ✅ Nginx **správně volá** backend auth endpoint (vidíme logy)
- ✅ Backend vrací hlavičky `Grafana-Jwt` a `X-Grafana-Org-Id: 2`

### Grafana konfigurace
- ✅ JWT autentizace správně nastavena:
  ```ini
  # docker/grafana/grafana.ini
  [auth.jwt]
  enabled = true
  header_name = X-Org-JWT
  username_claim = preferred_username
  email_claim = email
  org_id_claim = orgId              # ← Klíčové!
  jwk_set_file = /var/lib/grafana/jwks.json
  auto_sign_up = true
  
  [auth]
  disable_login_form = true
  ```

### Uživatel
- ✅ Uživatel se **dostane do Grafany** (není login page)
- ✅ Vidí svůj profil v Grafaně

---

## ❌ Co NEFUNGUJE

### 🔴 Hlavní problém: Dashboard 404

```http
GET /core-admin/monitoring/d/axiom_sys_overview/system-overview?orgId=2
→ 404 Not Found
```

### 🔍 Root Cause: Uživatel je ve špatné organizaci

#### Aktuální stav:
```bash
docker exec core-grafana curl -s -u admin:admin \
  "http://localhost:3000/api/users/lookup?loginOrEmail=test_admin" | jq

{
  "id": 2,
  "email": "test_admin",
  "name": "test_admin",
  "orgId": 1,              # ← ❌ Main Org (mělo by být 2!)
  "isGrafanaAdmin": false,
  ...
}
```

**Screenshot potvrzuje**: V Grafana profilu je `Organizations: Main Org (Current)`

#### Očekávaný stav:
```json
{
  "orgId": 2  // ← "Tenant: admin" organizace
}
```

### 📊 Dashboardy JSOU v Org 2

Dashboard existuje, ale v jiné organizaci:

```sql
-- Verified via SQL:
SELECT id, uid, title, org_id 
FROM dashboard 
WHERE uid = 'axiom_sys_overview';

-- Result:
-- id=43, uid='axiom_sys_overview', org_id=2
```

**Závěr**: Dashboard je správně, uživatel je ve špatné org.

---

## 🐛 Debug: User Provisioning Flow

### Kód v AuthRequestController

```java
// backend/.../AuthRequestController.java (lines 163-175)

// Resolve tenant → Grafana org mapping
TenantBinding binding = tenantOrgService.resolve(jwt);
Long grafanaOrgId = binding.orgId();  // ← Returns 2 ✅

log.debug("✅ Resolved user {} to Grafana org {}", username, grafanaOrgId);

// 🆕 IDEMPOTENT USER PROVISIONING FLOW
try {
    Long userId = ensureUser(email, name != null ? name : username);
    ensureOrgMembership(userId, grafanaOrgId, "Admin", email);
    ensureActiveOrg(userId, grafanaOrgId);  // ← Should set Org 2!
    
    log.debug("✅ User {} fully provisioned for org {}", username, userId, grafanaOrgId);
} catch (Exception e) {
    log.warn("⚠️ Failed to provision user {} for org {}: {}", 
             username, grafanaOrgId, e.getMessage());
}

// Mint JWT with orgId claim
String grafanaJwt = jwtService.mintGrafanaJwtFromKeycloakJwt(jwt);

return ResponseEntity.ok()
    .header("Grafana-Jwt", grafanaJwt)
    .header("X-Grafana-Org-Id", String.valueOf(grafanaOrgId))
    .build();
```

### ⚠️ Problém: Žádné logy z provisioning metod

Backend loguje:
```
✅ Resolved user test_admin to Grafana org 2  ← Vidíme
(nothing here)                                 ← Nevidíme žádné logy!
```

**Očekávali bychom vidět**:
```
🔍 Ensuring Grafana user exists: test_admin
✅ User test_admin already exists (id: 2)
👥 Ensuring user 2 is member of org 2 with role Admin
✅ User 2 already member of org 2 with role Admin
🔄 Ensuring active org 2 for user 2
✅ Active org set to 2 for user 2
✅ User test_admin (id: 2) fully provisioned for org 2
```

**Nebo ERROR/WARN**:
```
⚠️ Failed to provision user test_admin for org 2: [error message]
```

### 🤔 Možné příčiny

1. **Metody vůbec neběží**
   - Exception vyvolaná PŘED voláním `ensureUser()`
   - Nějaká podmínka přeskočí celý blok

2. **Exception zachycena a zalogována jinak**
   - `grafanaAdminClient` metody házejí exception
   - Catch blok ji spolkne bez logu

3. **Log level není správný**
   - Metody používají `log.debug()` ale produkce má INFO level
   - Potřebujeme změnit na `log.info()` pro debug

4. **Race condition**
   - Grafana vytvoří uživatele při prvním JWT auth
   - `ensureActiveOrg()` volá API na uživatele, který ještě neexistuje
   - API call selže tiše

---

## 🛠️ Opravené problémy během session

| # | Problém | Řešení | Status |
|---|---------|--------|--------|
| 1 | Backend 403 na Keycloak | Opravena URL: http→https, port 8080→8443 | ✅ |
| 2 | Duplicitní endpoint `/internal/auth/grafana` | Smazán `GrafanaAuthController.java` | ✅ |
| 3 | Nginx nesprávná proměnná | `$upstream_http_grafana_org_id` → `$upstream_http_x_grafana_org_id` | ✅ |
| 4 | Backend compile error | Opraveny dependencies | ✅ |
| 5 | Změny ztraceny po rebuildu | Commitnuty do gitu (commit `a5ceb61`) | ✅ |

---

## 🔧 Co je potřeba vyřešit

### 1️⃣ Debug user provisioning (PRIORITA!)

**Akce:**
```java
// Změnit log level na INFO v ensureUser(), ensureOrgMembership(), ensureActiveOrg()
log.info("🔍 Ensuring Grafana user exists: {}", email);  // ← DEBUG → INFO
log.info("✅ User {} already exists (id: {})", email, userId);
log.info("👥 Ensuring user {} is member of org {}", userId, orgId);
// ... atd
```

**Proč:**
- Vidět, jestli metody vůbec běží
- Odhalit, kde přesně to selže
- Ověřit response z Grafana Admin API

### 2️⃣ Možná řešení

#### Option A: Fix provisioning flow

```java
// Možné problémy v ensureActiveOrg():
// 1. API call je nesprávný
// 2. Grafana ignoruje změnu aktivní org při JWT auth
// 3. User ještě neexistuje (race condition)
// 4. API vyžaduje jiný formát (userId vs loginOrEmail)

private void ensureActiveOrg(Long userId, Long orgId) {
    log.info("🔄 Setting active org {} for user {}", orgId, userId);
    
    try {
        grafanaAdminClient.setUserActiveOrg(userId, orgId);
        log.info("✅ Active org set successfully");
    } catch (Exception e) {
        log.error("❌ Failed to set active org: {}", e.getMessage(), e);
        throw e;  // Re-throw to see in outer catch
    }
}
```

#### Option B: Inicializační script

```bash
#!/bin/bash
# docker/grafana/init-users.sh
# Předvytvořit uživatele v Org 2 před prvním použitím

GRAFANA_URL="http://localhost:3000"
ADMIN_USER="admin"
ADMIN_PASS="${GRAFANA_PASSWORD:-admin}"

# Add user to Org 2
curl -u "$ADMIN_USER:$ADMIN_PASS" \
  -X POST "$GRAFANA_URL/api/orgs/2/users" \
  -H "Content-Type: application/json" \
  -d '{
    "loginOrEmail": "test_admin",
    "role": "Admin"
  }'

# Set Org 2 as default for user
USER_ID=$(curl -s -u "$ADMIN_USER:$ADMIN_PASS" \
  "$GRAFANA_URL/api/users/lookup?loginOrEmail=test_admin" | jq -r .id)

curl -u "$ADMIN_USER:$ADMIN_PASS" \
  -X POST "$GRAFANA_URL/api/users/$USER_ID/using/2"
```

#### Option C: JWT auto-provisioning enhancement

```ini
# docker/grafana/grafana.ini
[auth.jwt]
enabled = true
header_name = X-Org-JWT
username_claim = preferred_username
email_claim = email
org_id_claim = orgId           # ✅ Already set
org_role_claim = role          # ← Přidat?
auto_sign_up = true
```

**Možná potřebujeme**:
- Zkontrolovat, že Grafana skutečně používá `orgId` claim z JWT
- Ověřit pořadí operací: auto_sign_up → JWT validation → org selection

---

## 📁 Filesystem stav

### Modified files (committed):

```
✅ docker/nginx/nginx-ssl.conf.template
   - Fixed auth_request_set variable name
   - Line 117: $upstream_http_x_grafana_org_id

❌ backend/.../GrafanaAuthController.java
   - Deleted (duplicate, conflicted with AuthRequestController)
```

### Key files:

```
backend/src/main/java/cz/muriel/core/monitoring/
├── AuthRequestController.java      # Main auth logic (needs debug logs!)
├── GrafanaJwtService.java          # JWT minting with orgId claim
├── GrafanaTenantRegistry.java      # Tenant→Org mapping: admin=2
└── grafana/
    └── GrafanaAdminClient.java     # Grafana Admin API client

docker/
├── grafana/
│   ├── grafana.ini                 # JWT config: org_id_claim = orgId
│   └── entrypoint.sh               # Creates orgs at startup
└── nginx/
    └── nginx-ssl.conf.template     # auth_request config (fixed)
```

---

## 🧪 Jak testovat

### 1. Check user organization:
```bash
docker exec core-grafana curl -s -u admin:admin \
  "http://localhost:3000/api/users/lookup?loginOrEmail=test_admin" | jq .orgId

# Expected: 2
# Actual: 1 ❌
```

### 2. Verify dashboard exists in Org 2:
```bash
docker exec core-db psql -U postgres -d grafana -c \
  "SELECT org_id, uid, title FROM dashboard WHERE uid='axiom_sys_overview';"

# Expected: org_id | uid                  | title
#           2      | axiom_sys_overview   | System Overview
```

### 3. Check backend logs:
```bash
docker logs core-backend --since 5m 2>&1 | grep -i "provision\|ensure"

# Expected: DEBUG/INFO logs from ensureUser, ensureOrgMembership, ensureActiveOrg
# Actual: NOTHING ❌
```

### 4. Manual browser test:
1. Login as `test_admin` / `admin123`
2. Navigate to: https://admin.core-platform.local/core-admin/monitoring
3. Should see: **Dashboard** ✅
4. Actually see: **404 Error** ❌

### 5. Check JWT contains orgId:
```bash
# Capture JWT from browser DevTools → Network → Headers → Grafana-Jwt
# Decode at jwt.io
# Should contain: "orgId": 2
```

### 6. Test Grafana JWT validation:
```bash
# Check JWKS endpoint works:
curl -s http://localhost:8080/.well-known/jwks.json | jq

# Should return RSA public key with kid="grafana-bff-key-1"
```

---

## 🚀 Next Steps (pro nový chat)

### Immediate (Debug):
1. **Add INFO logging** to provisioning methods in `AuthRequestController`:
   - `ensureUser()` - start, found/created, userId
   - `ensureOrgMembership()` - check, add, result
   - `ensureActiveOrg()` - API call, response
   
2. **Rebuild backend** and test:
   ```bash
   cd backend && ./mvnw clean package -DskipTests
   docker cp target/backend-*.jar core-backend:/app/app.jar
   docker restart core-backend
   ```

3. **Check logs** for provisioning flow:
   ```bash
   docker logs core-backend --follow | grep -i "ensuring\|provisioned"
   ```

### If provisioning works but still 404:
4. **Verify JWT claim** is actually used by Grafana
5. **Test manual org switch** via Grafana Admin API
6. **Check Grafana logs** for JWT validation errors

### If provisioning fails:
7. **Implement init script** (Option B above)
8. **Debug GrafanaAdminClient** API calls
9. **Check Grafana Admin API** permissions/responses

---

## 📝 Notes

### Tenant → Org Mapping
```java
// GrafanaTenantRegistry.java
tenantToOrgId.put("admin", 2);       // Tenant: admin
tenantToOrgId.put("test-tenant", 3); // Tenant: test-tenant  
tenantToOrgId.put("company-b", 4);   // Tenant: company-b
```

### Grafana Orgs
```
Org 1: Main Org (default, system)
Org 2: Tenant: admin (dashboards here! ✅)
Org 3: Tenant: test-tenant
Org 4: Tenant: company-b
```

### Git Commit
```
Commit: a5ceb61
Message: fix(grafana-sso): Fix nginx template auth_request variable name
Files: 
  - M docker/nginx/nginx-ssl.conf.template
  - D backend/.../GrafanaAuthController.java
```

---

## 🆘 Debug Commands

```bash
# Quick health check:
docker ps --filter name=core- --format "{{.Names}}\t{{.Status}}"

# Check backend is processing auth requests:
docker logs core-backend --tail 100 | grep "Grafana auth request"

# See JWT being minted:
docker logs core-backend --tail 100 | grep "Minted RS256 Grafana JWT"

# Verify nginx is calling backend:
docker logs core-nginx --tail 100 | grep "/internal/auth/grafana"

# Check Grafana received JWT:
docker logs core-grafana --tail 100 | grep -i "jwt\|auth"

# Full diagnostic:
docker exec core-grafana curl -s -u admin:admin http://localhost:3000/api/users | jq
docker exec core-grafana curl -s -u admin:admin http://localhost:3000/api/orgs/2/users | jq
```

---

**Status**: 🔴 Blocked - Need to debug why user provisioning is not running/logging  
**Priority**: 🔥 High - Core functionality broken  
**Estimated fix**: 2-4 hours (add logging → rebuild → test → implement fix)
