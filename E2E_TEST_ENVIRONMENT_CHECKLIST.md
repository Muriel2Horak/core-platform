# E2E Test Environment Setup Checklist

**Datum:** 26. října 2025  
**Účel:** Verifikace prostředí před spuštěním E2E testů po clean rebuild

---

## 📋 Pre-Test Checklist

### 1. Docker Services Status ✅/❌

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

- [ ] `core-backend` - Status: `Up X minutes (healthy)`
- [ ] `core-frontend` - Status: `Up X minutes (healthy)`
- [ ] `core-postgres` (core-db) - Status: `Up X minutes (healthy)`
- [ ] `core-keycloak` - Status: `Up X minutes`
- [ ] `core-nginx` - Status: `Up X minutes`
- [ ] `core-kafka` - Status: `Up X minutes`
- [ ] `core-redis` - Status: `Up X minutes`
- [ ] `core-loki` - Status: `Up X minutes`

**Očekávaný výsledek:** Všechny služby `Up` a backend/frontend/postgres mají `(healthy)` status

---

### 2. Backend Health Check ✅/❌

```bash
# Základní health
curl -k https://admin.core-platform.local/actuator/health 2>/dev/null | jq .

# Database connection
curl -k https://admin.core-platform.local/actuator/health/db 2>/dev/null | jq .
```

**Očekávaný výsledek:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

- [ ] Backend health status: `UP`
- [ ] Database status: `UP`
- [ ] Response time < 2s

---

### 3. Frontend Loading ✅/❌

```bash
# Check HTML načítá
curl -k https://admin.core-platform.local/ 2>/dev/null | grep -i "<title>"

# Check JavaScript bundle
curl -k https://admin.core-platform.local/assets/ 2>/dev/null | grep "index"
```

**Očekávaný výsledek:**
```html
<title>Axiom - Enterprise Platform</title>
```

- [ ] HTML načítá správně
- [ ] Title tag přítomen
- [ ] Assets dostupné

---

### 4. Keycloak Admin Access ✅/❌

```bash
# Test Keycloak admin UI
curl -k https://admin.core-platform.local:8081/ 2>/dev/null | grep -i "keycloak"
```

**Manuální test:**
1. Otevři: https://admin.core-platform.local:8081/
2. Login: `admin` / `admin`
3. Zkontroluj realm: `admin`

- [ ] Keycloak admin UI načítá
- [ ] Login jako admin funguje
- [ ] Realm `admin` existuje

---

### 5. Database Schema & Data ✅/❌

```bash
# Check PostgreSQL
docker exec core-db psql -U axiom -d axiom -c "\dt" | grep -E "users|roles|tenants"

# Count records
docker exec core-db psql -U axiom -d axiom -c "SELECT COUNT(*) FROM users;"
docker exec core-db psql -U axiom -d axiom -c "SELECT COUNT(*) FROM roles;"
```

**Očekávaný výsledek:**
- [ ] Tabulka `users` existuje
- [ ] Tabulka `roles` existuje  
- [ ] Tabulka `tenants` existuje
- [ ] Tabulka `groups` existuje
- [ ] Liquibase changesets aplikovány

---

### 6. Test Users Existence ✅/❌

**KRITICKÉ:** E2E testy potřebují tyto uživatele!

```bash
# Check if test_admin exists
docker exec core-db psql -U axiom -d axiom -c "SELECT username, email FROM users WHERE username IN ('test_admin', 'test');"
```

**Očekávaný výsledek:**
```
  username   |        email        
-------------+---------------------
 test_admin  | test.admin@test.local
 test        | test@test.local
```

- [ ] `test_admin` existuje - Password: `Test.1234`
- [ ] `test` existuje - Password: `Test.1234`
- [ ] Role `CORE_ROLE_ADMIN` přiřazena `test_admin`
- [ ] Role `CORE_ROLE_USER_MANAGER` přiřazena `test_admin`
- [ ] Role `CORE_ROLE_USER` přiřazena `test`

**Pokud chybí:**
```bash
# Vytvoř test uživatele manuálně přes Keycloak admin UI
# NEBO spusť backend init script (pokud existuje)
```

---

### 7. Manual Login Test ✅/❌

**Browser test (DŮLEŽITÉ!):**

1. **Otevři:** https://admin.core-platform.local/
2. **Login jako test_admin:**
   - Username: `test_admin`
   - Password: `Test.1234`
3. **Ověř redirect:** Po loginu by měl být redirect na `/core-admin/dashboard` nebo `/dashboard`
4. **Zkontroluj menu:** Admin menu by mělo být viditelné
5. **Naviguj na:** https://admin.core-platform.local/core-admin/users
6. **Ověř:** Seznam uživatelů se načte (minimálně test_admin a test viditelní)

- [ ] Login funguje (bez redirect loop)
- [ ] Redirect na dashboard úspěšný
- [ ] Admin menu viditelné
- [ ] Users list načítá data
- [ ] Search box funguje
- [ ] Žádné 403/401/500 errors v konzoli

**Pokud selže:**
- Zkontroluj backend logy: `make logs-backend | tail -100`
- Zkontroluj frontend logy: `make logs-frontend | tail -100`
- Zkontroluj nginx logy: `docker logs core-nginx 2>&1 | tail -50`

---

### 8. API Endpoints Test ✅/❌

**Pro E2E testy jsou klíčové tyto endpointy:**

```bash
# Get JWT token (simulace Playwright auth)
TOKEN=$(curl -k -X POST "https://admin.core-platform.local/realms/admin/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web" \
  -d "grant_type=password" \
  -d "username=test_admin" \
  -d "password=Test.1234" \
  -d "scope=openid" 2>/dev/null | jq -r .access_token)

echo "Token: ${TOKEN:0:50}..."

# Test API endpoints
curl -k -H "Authorization: Bearer $TOKEN" https://admin.core-platform.local/api/users | jq .
curl -k -H "Authorization: Bearer $TOKEN" https://admin.core-platform.local/api/roles | jq .
curl -k -H "Authorization: Bearer $TOKEN" https://admin.core-platform.local/api/groups | jq .
curl -k -H "Authorization: Bearer $TOKEN" https://admin.core-platform.local/api/admin/tenants | jq .
```

**Očekávaný výsledek:**
- [ ] `/api/users` vrací pole uživatelů (minimálně test_admin, test)
- [ ] `/api/roles` vrací pole rolí (CORE_ROLE_ADMIN, CORE_ROLE_USER, ...)
- [ ] `/api/groups` vrací pole skupin (může být prázdné)
- [ ] `/api/admin/tenants` vrací pole tenantů (může být prázdné nebo default tenant)
- [ ] Žádné 403/401 errors
- [ ] Response time < 2s

---

### 9. Loki Logs Check ✅/❌

```bash
# Check Loki je dostupný
make logs-backend | tail -20

# NEBO přímý test
curl -k http://localhost:3100/ready 2>/dev/null
```

- [ ] Loki běží a odpovídá
- [ ] Backend logy viditelné přes `make logs-backend`
- [ ] Žádné ERROR logy při startu

---

### 10. Environment Variables ✅/❌

```bash
# Check .env soubor
cat .env | grep -E "POSTGRES_|KEYCLOAK_|FRONTEND_"

# Check backend env
docker exec core-backend env | grep -E "SPRING_|DB_"
```

- [ ] `.env` soubor existuje (vygenerován z `.env.template`)
- [ ] PostgreSQL credentials správně nastaveny
- [ ] Keycloak URLs správně nastaveny
- [ ] Backend environment variables načteny

---

## 🚀 Spuštění E2E Testů

**Pokud všechny checklisty ✅:**

```bash
cd e2e

# 1. Jen admin CRUD testy (45 testů - měly by projít 100%)
npx playwright test specs/admin/ --reporter=list

# 2. Konkrétní suite pro debugging
npx playwright test specs/admin/users-crud.spec.ts --reporter=list

# 3. S headless=false pro debugging
npx playwright test specs/admin/users-crud.spec.ts --headed --workers=1

# 4. Debug mode
npx playwright test specs/admin/users-crud.spec.ts --debug
```

---

## 📊 Očekávané výsledky

### Admin CRUD testy (po refaktoringu):
- **users-crud.spec.ts:** 10/10 passed ✅
- **roles-crud.spec.ts:** 11/11 passed ✅
- **groups-crud.spec.ts:** 11/11 passed ✅
- **tenants-crud.spec.ts:** 13/13 passed ✅

**Celkem:** 45/45 passed (100%)

### Známé problémy (očekávané):
- **keycloak-sync.spec.ts:** 10/10 failed ❌ - Backend endpoint chybí
  - **Řešení:** Skip nebo implementace `/api/admin/keycloak-sync/status`

---

## ❌ Co dělat pokud testy selhávají

### Problém: Auth redirect loop
```
TimeoutError: page.waitForURL: Timeout 15000ms exceeded
```

**Řešení:**
1. Zkontroluj že test_admin uživatel existuje v databázi
2. Zkontroluj že Keycloak realm `admin` je správně nastaven
3. Zkontroluj backend logy: `make logs-backend | grep -i error`

### Problém: Element not found
```
Error: locator('[role=button][name=edit]') not found
```

**Řešení:**
1. Spusť test s `--headed` pro vizuální debugging
2. Zkontroluj screenshot v `test-results/`
3. Ověř že frontend načítá správně (F12 → Network tab)

### Problém: 403 Forbidden
```
Error: Request failed with status 403
```

**Řešení:**
1. Zkontroluj že uživatel má správné role
2. Zkontroluj backend RBAC konfiguraci
3. Zkontroluj JWT token v Network tab

### Problém: 500 Internal Server Error
```
Error: Request failed with status 500
```

**Řešení:**
1. Zkontroluj backend logy: `make logs-backend | grep -i "500\|error"`
2. Zkontroluj databázi: `make logs | grep postgres`
3. Možná chybí Liquibase migrations

---

## 🔍 Debugging Commands

```bash
# Backend logy (poslední 100 řádků)
make logs-backend | tail -100

# Frontend logy
make logs-frontend | tail -100

# Všechny ERROR logy
make logs-errors

# Nginx logy (auth errors)
docker logs core-nginx 2>&1 | grep -i "auth\|403\|401"

# PostgreSQL logy
docker logs core-db 2>&1 | tail -50

# Keycloak logy
make logs-keycloak | tail -100

# Health check všech služeb
make verify
```

---

## ✅ Final Checklist

Před commitem výsledků:

- [ ] Všechny Docker služby běží a jsou healthy
- [ ] Backend health check: UP
- [ ] Frontend načítá správně
- [ ] Test uživatelé existují (test_admin, test)
- [ ] Manual login funguje bez redirect loop
- [ ] Admin UI přístupné a funkční
- [ ] API endpointy odpovídají správně
- [ ] **45/45 admin CRUD testů prošlo ✅**
- [ ] Dokumentace aktualizována
- [ ] Git commit s výsledky

---

**Připraveno pro:** E2E test run  
**Očekávaný čas:** ~15-20 minut (45 testů)  
**Target pass rate:** 100% (45/45)  
**Status:** ✅ READY TO TEST
