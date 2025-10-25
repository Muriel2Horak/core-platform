# Grafana SSO & Provisioning Fix - Implementation Summary

## 🎯 Cíl
Opravit Grafana provisioning a SSO, aby uživatel viděl dashboardy bez loginu a bez 404.

## ✅ Definition of Done
1. ✅ Iframe loads dashboard bez login prompt, bez 404
2. ✅ Logy zobrazují 3 INFO kroky během provisioningu:
   - `🔎 [Provisioning] Step 1/3: ensureUser`
   - `👥 [Provisioning] Step 2/3: ensureOrgMembership`
   - `🔄 [Provisioning] Step 3/3: ensureActiveOrg`
3. ✅ `scripts/grafana/user-check.sh test_admin` ukazuje org 2 membership a current org 2
4. ✅ CI sanity test existuje a prochází: `make test-grafana-sso`

## 📋 Provedené změny

### 1. Backend: Fix provisioning flow (✅ Hotovo)

**Soubor:** `backend/src/main/java/cz/muriel/core/monitoring/AuthRequestController.java`

**Změny:**
- ✅ Upravil `ensureUser()` - používá `lookupUser()` API (vrací `Optional<UserLookupResponse>`)
- ✅ Upravil `ensureOrgMembership()` - lepší logování Step 2/3
- ✅ Upravil `ensureActiveOrg()` - lepší logování Step 3/3
- ✅ Přidal importy: `CreateUserResponse`, `UserLookupResponse`, `Optional`
- ✅ Hlavní provisioning flow má START/SUCCESS logování s emojis

**Logy (příklad):**
```
🚀 [Provisioning] START: user=test_admin, email=admin@example.com, orgId=2
🔎 [Provisioning] Step 1/3: ensureUser(email=admin@example.com)
   ├─ ✓ User exists: userId=3
👥 [Provisioning] Step 2/3: ensureOrgMembership(userId=3, orgId=2)
   ├─ ✓ User already member: role=Admin
🔄 [Provisioning] Step 3/3: ensureActiveOrg(userId=3, orgId=2)
   ├─ ✓ Active org set to orgId=2
✅ [Provisioning] SUCCESS: user=test_admin, userId=3, orgId=2
```

**Klíčové API volání:**
```java
grafanaAdminClient.setUserActiveOrg(userId, orgId);
// volá: POST /api/users/{userId}/using/{orgId} ✅ CORRECT (plural 'users')
```

### 2. Frontend: Odstranit orgId z URL (✅ Hotovo)

**Soubor:** `frontend/src/utils/grafanaUrl.ts`

**Změny:**
- ❌ ODSTRANIL: `url.searchParams.set('orgId', String(orgId));`
- ✅ orgId parametr ponechán kvůli zpětné kompatibilitě, ale nepoužívá se
- ✅ Backend (setUserActiveOrg) se postará o správnou org

**Důvod:**
- Orgid v URL může přepsat org nastavenou backendem
- JWT + setUserActiveOrg zajistí správnou org automaticky

### 3. Grafana: Fix JWT config (✅ Hotovo)

**Soubor:** `docker/grafana/grafana.ini`

**Změny:**
- ❌ PŘED: `jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json` (external HTTPS)
- ✅ PO: `jwk_set_url = http://backend:8080/.well-known/jwks.json` (internal Docker network)

**Důvod:**
- Grafana container komunikuje s backendem přes interní Docker network
- HTTP je OK uvnitř Docker network (není veřejný)
- Rychlejší (bez SSL overhead)

**Ověřená konfigurace:**
```ini
[auth.jwt]
enabled = true
header_name = X-Org-JWT
username_claim = preferred_username
email_claim = email
org_id_claim = orgId ✅
jwk_set_url = http://backend:8080/.well-known/jwks.json ✅
auto_sign_up = true ✅

[security]
allow_embedding = true ✅

[auth]
disable_login_form = true ✅
```

### 4. Nginx: Ověřena konfigurace (✅ Hotovo)

**Soubor:** `docker/nginx/nginx-ssl.conf.template`

**Ověřené nastavení:**
```nginx
# Auth request endpoint
location = /_auth/grafana {
    proxy_set_header Cookie $http_cookie; ✅ (řádek 92)
    # ... další headers
}

# Grafana proxy
location ^~ /core-admin/monitoring/ {
    auth_request /_auth/grafana; ✅
    proxy_set_header X-Org-JWT $grafana_token; ✅ (řádek 119)
    proxy_set_header X-Grafana-Org-Id $grafana_org_id; ✅ (řádek 120)
    # ... další headers
}
```

**Závěr:** Nginx konfigurace je **správná**, žádné změny potřeba.

### 5. Diagnostic CLI skripty (✅ Hotovo)

Vytvořeny 3 skripty pro debugging:

#### a) `scripts/grafana/user-check.sh <login>`
**Účel:** Zkontrolovat uživatele, seznam orgs, current org

**Použití:**
```bash
GRAFANA_ADMIN_PASSWORD=admin123 ./scripts/grafana/user-check.sh test_admin
```

**Výstup:**
```
🔍 Grafana User Diagnostics
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 1/3: Looking up user 'test_admin'
✅ User found:
  ID: 3
  Email: admin@example.com
  Name: Test Admin
  Login: test_admin

Step 2/3: Fetching user's organizations
✅ User is member of these organizations:
  • Org 1: Main Org. (role: Admin)
  • Org 2: Tenant Admin Org (role: Admin)

Step 3/3: Checking current active organization
✅ Current active organization:
  Org ID: 2

Summary:
  User ID: 3
  Current Org: 2
  Total Orgs: 2

✅ PASS: test_admin current org is 2 (expected for tenant admin)
```

#### b) `scripts/grafana/user-set-org.sh <login> <orgId>`
**Účel:** Manuálně nastavit active org pro uživatele

**Použití:**
```bash
GRAFANA_ADMIN_PASSWORD=admin123 ./scripts/grafana/user-set-org.sh test_admin 2
```

**Výstup:**
```
🔄 Grafana Set Active Organization
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 1/4: Looking up user 'test_admin'
✅ User found: ID=3

Step 2/4: Checking current active org
Current org: 1

Step 3/4: Verifying user is member of org 2
✅ User is member of org 2

Step 4/4: Setting active org to 2
✅ SUCCESS: Active org set to 2

Summary:
  User ID: 3
  Old Org: 1
  New Org: 2

💡 Verify: ./scripts/grafana/user-check.sh test_admin
```

#### c) `scripts/grafana/sanity-test.sh`
**Účel:** CI sanity test - ověří provisioning

**Použití:**
```bash
GRAFANA_ADMIN_PASSWORD=admin123 ./scripts/grafana/sanity-test.sh
```

**Testy:**
1. ✅ test_admin exists
2. ✅ test_admin is member of org 2 AND current org is 2
3. ✅ Dashboard accessible (HTTP 200, not 302/401)

### 6. CI Integration (✅ Hotovo)

**Soubor:** `Makefile`

**Přidáno:**
```makefile
.PHONY: test-grafana-sso
test-grafana-sso:
	@echo "🔐 GRAFANA SSO SANITY TEST"
	@bash scripts/grafana/sanity-test.sh
```

**Použití:**
```bash
# Po startu prostředí
make dev-up
make test-grafana-sso

# Nebo v CI pipeline
make up
make test-grafana-sso
```

**Výstup v help:**
```
make help-advanced
  ...
  📊 Monitoring & Grafana:
    test-grafana-sso            - Grafana SSO sanity test (provisioning + org)
  ...
```

## 🔍 Debugging Guide

### Scenario 1: Iframe ukazuje login page (302 redirect)

**Diagnóza:**
```bash
# 1. Zkontroluj JWT validaci
docker compose logs grafana | grep -i jwt

# 2. Zkontroluj backend auth request
docker compose logs backend | grep "Provisioning"

# 3. Zkontroluj cookies v browseru
# DevTools → Application → Cookies → at, rt
```

**Možné příčiny:**
- JWT není platný (špatný JWKS URL)
- Cookies se nepředávají (Nginx config)
- Backend provisioning failuje (check logs)

### Scenario 2: Iframe ukazuje 404

**Diagnóza:**
```bash
# 1. Zkontroluj org membership
./scripts/grafana/user-check.sh test_admin

# 2. Zkontroluj dashboard v org 2
curl -u admin:admin123 http://localhost:3000/api/dashboards/uid/axiom_sys_overview

# 3. Zkontroluj current org
./scripts/grafana/user-check.sh test_admin | grep "Current Org"
```

**Možné příčiny:**
- Dashboard je v org 1, user je v org 2
- setUserActiveOrg nevolá správný endpoint
- Dashboard neexistuje (provisioning failed)

### Scenario 3: User má wrong org po loginu

**Diagnóza:**
```bash
# 1. Zkontroluj provisioning logy
docker compose logs backend | grep "ensureActiveOrg"

# 2. Manuálně zkontroluj current org
./scripts/grafana/user-check.sh test_admin

# 3. Pokus o manuální fix
./scripts/grafana/user-set-org.sh test_admin 2
```

**Možné příčiny:**
- setUserActiveOrg failuje (check GrafanaAdminClient logs)
- Race condition (user přepne org manuálně po provisioningu)
- JWT neobsahuje správný orgId claim

## 📊 Architektura SSO Flow

```
┌─────────────┐
│   Browser   │
│  (Cookies:  │
│   at, rt)   │
└──────┬──────┘
       │ GET /core-admin/monitoring/d/abc
       ▼
┌──────────────────┐
│      Nginx       │
│  auth_request    │
│ /_auth/grafana   │
└──────┬───────────┘
       │ proxy_set_header Cookie $http_cookie
       ▼
┌──────────────────────────────────┐
│  Backend AuthRequestController   │
│  /internal/auth/grafana          │
│                                  │
│  1. Validate JWT from cookie     │
│  2. Extract email, orgId         │
│  3. PROVISION USER:              │
│     a) ensureUser (lookup/create)│
│     b) ensureOrgMembership       │
│     c) ensureActiveOrg ← KEY!    │
│  4. Generate Grafana-JWT         │
│  5. Return 200 + JWT header      │
└──────┬───────────────────────────┘
       │ Response: Grafana-JWT: eyJ...
       ▼
┌──────────────────┐
│      Nginx       │
│  auth_request_set│
│  $grafana_token  │
└──────┬───────────┘
       │ proxy_set_header X-Org-JWT $grafana_token
       ▼
┌──────────────────┐
│     Grafana      │
│  JWT validation  │
│  via JWKS        │
│  (orgId claim)   │
└──────────────────┘
```

**Klíčové body:**
1. **Cookies** (`at`, `rt`) se předávají do `/_auth/grafana` přes `proxy_set_header Cookie`
2. **Backend provisioning** zajistí user exists + org membership + **active org = orgId**
3. **JWT** obsahuje `orgId` claim (numeric)
4. **Grafana** validuje JWT přes JWKS a čte `orgId` claim
5. **setUserActiveOrg** volá `POST /api/users/{userId}/using/{orgId}` (plural 'users' = admin endpoint)

## 🧪 Testování

### Ruční test (lokální)
```bash
# 1. Start prostředí
make dev-up

# 2. Počkej na health checks
make dev-check

# 3. Spusť sanity test
GRAFANA_ADMIN_PASSWORD=admin123 make test-grafana-sso

# 4. Otevři browser
open https://admin.core-platform.local/monitoring

# 5. Login jako test_admin
# Username: test_admin
# Password: TempAdminPass123!

# 6. Měl bys vidět dashboard, ne login page
```

### CI test (automatický)
```bash
# V CI pipeline (např. GitHub Actions)
make up
make test-grafana-sso  # fail-fast pokud provisioning nefunguje
```

## 🎯 Výsledek

### Před fix:
❌ Iframe ukazuje login page (302 redirect)  
❌ 404 Not Found (dashboard v jiné org)  
❌ User má current org = 1, měl by 2  

### Po fix:
✅ Iframe loads dashboard bez login prompt  
✅ Logy ukazují 3 provisioning kroky  
✅ `user-check.sh test_admin` ukazuje org 2, current org 2  
✅ CI sanity test prochází  

## 📝 Soubory změněny

1. ✅ `backend/src/main/java/cz/muriel/core/monitoring/AuthRequestController.java`
2. ✅ `frontend/src/utils/grafanaUrl.ts`
3. ✅ `docker/grafana/grafana.ini`
4. ✅ `scripts/grafana/user-check.sh` (nový)
5. ✅ `scripts/grafana/user-set-org.sh` (nový)
6. ✅ `scripts/grafana/sanity-test.sh` (nový)
7. ✅ `Makefile` (přidán target `test-grafana-sso`)

## 🔗 Reference

- [Grafana JWT Auth](https://grafana.com/docs/grafana/latest/setup-grafana/configure-security/configure-authentication/jwt/)
- [Grafana Admin API](https://grafana.com/docs/grafana/latest/developers/http_api/admin/)
- [ENVIRONMENT_AUDIT.md](../ENVIRONMENT_AUDIT.md) - Audit odhalil problém s JWKS URL

---

**Autor:** AI Assistant  
**Datum:** 2025-01-XX  
**Status:** ✅ Kompletní - všechny 6 úkoly hotovy
