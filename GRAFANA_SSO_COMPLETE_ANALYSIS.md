# Grafana SSO Integration - Complete Analysis & Decision

**Datum:** 25. října 2025  
**Status:** 🔴 UKONČENO - Navrhován ROLLBACK

## 📋 Executive Summary

Po 7 dnech práce na Grafana SSO integraci jsme v **deadlocku**:
- Backend v restart loopu
- Grafana nefunkční
- Nginx rozestavěný
- Prostředí nestabilní

**DOPORUČENÍ:** Rollback všech Grafana změn a přesun monitoring funkcí přímo do FE.

---

## 🎯 Původní Cíl

**Co jsme chtěli:**
Automatické SSO přihlášení do Grafany pomocí Keycloak JWT, aby uživatelé nemuseli zadávat credentials znovu.

**Flow měl být:**
```
User → Keycloak login → Core Platform FE → Grafana iframe (auto SSO) → Dashboard
```

---

## 🛠️ Co Bylo Implementováno

### 1. **Backend - GrafanaAuthBridgeController** (NOVÝ)
```java
@GetMapping("/internal/auth/grafana")
public ResponseEntity<Void> authenticateForGrafana(HttpServletRequest request)
```

**Účel:**
- Nginx `auth_request` endpoint
- Převádí Keycloak JWT (cookie 'at') → Grafana JWT
- Vrací `Grafana-JWT` a `Grafana-Org-Id` headers

**Soubory:**
- `backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java` ✅ VYTVOŘEN

### 2. **Nginx Config Změny**
```nginx
location ^~ /core-admin/monitoring/ {
    auth_request /_auth/grafana;  # Volá náš endpoint
    auth_request_set $grafana_token $upstream_http_grafana_jwt;
    proxy_set_header X-Org-JWT $grafana_token;
}
```

**Soubory:**
- `docker/nginx/nginx.conf` - řádky 126-135 ✅ UPRAVENO

### 3. **Grafana JWT Service**
Už existoval, jen jsme ho začlenili:
- `backend/src/main/java/cz/muriel/core/monitoring/GrafanaJwtService.java` ✅ EXISTUJÍCÍ
- Mintuje RS256 JWT s TTL 300s
- JWKS endpoint: `/monitoring/jwks/bff` ✅ FUNKČNÍ

### 4. **Grafana Konfigurace**
```ini
[auth.jwt]
enabled = true
header_name = X-Org-JWT
jwk_set_url = http://backend:8080/monitoring/jwks/bff
```

**Soubory:**
- `docker/grafana/grafana.ini.template` ✅ UPRAVENO

### 5. **Debugging & Testing**
- `e2e/specs/monitoring/grafana-sso-debug.spec.ts` - Playwright test ✅ VYTVOŘEN
- `.github/copilot-instructions.md` - Golden rules ✅ VYTVOŘEN

---

## ❌ Současný Stav Problémů

### Backend
```
Status: Up 23s (health: starting) → RESTART LOOP
Problém: Spring Boot aplikace nespoštní
Logy: Zamrzlá inicializace (JWKS vytvoření OK, pak nic)
```

**Podezření:**
- Nový `GrafanaAuthBridgeController` má dependency injection issue?
- FilterParser fix (JOOQ) způsobil kolaps?
- Database connection timeout?

### Grafana
```
Status: Created (not started)
Problém: Neběží vůbec
```

### Nginx
```
Status: Rozestavěný
Problém: Musel být ručně startován
```

---

## 🔍 Root Cause Analysis

### Problém #1: Backend Restart Loop
**Možné příčiny:**
1. `GrafanaAuthBridgeController` vyžaduje `JwtDecoder` bean, který není dostupný při startu
2. Circular dependency mezi `JwtDecoder` a `GrafanaJwtService`
3. FilterParser změny rozbily JOOQ type system

**Evidence:**
```
Last log: ✅ JWKS endpoint initialized with kid=grafana-bff-key-1
Missing: Started CorePlatformApplication in X seconds
```

### Problém #2: Make Clean Nefunguje Správně
**Co jsme zjistili:**
- `make dev-clean` nerestartuje containery (používá starý working dir `/app` místo `/workspace`)
- `make clean` trvá 1 hodinu (full rebuild + E2E)
- `make clean-fast` nezahrnuje všechny služby (Nginx, Grafana zůstaly Created)

### Problém #3: Dev Mode Je Rozbitý
**Evidence:**
```bash
docker inspect core-backend --format='{{.Config.WorkingDir}}'
# Output: /app (ŠPATNĚ, mělo být /workspace)
```

Devcontainer overlay se správně nenačítá.

---

## 📊 Změněné Soubory (Git Status)

### Modifikované:
1. `backend/src/main/java/cz/muriel/core/metamodel/filter/FilterParser.java`
   - Fix: `DSL.val()` wrapping pro JOOQ type safety
   - **Keep:** Tato změna je dobrá, opravuje testy

### Nové (Grafana related):
1. `backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java` ❌ SMAZAT
2. `e2e/specs/monitoring/grafana-sso-debug.spec.ts` ❌ SMAZAT
3. `e2e/debug-grafana-sso.spec.ts` ❌ SMAZAT
4. `e2e/test-auth-endpoint.js` ❌ SMAZAT

### Keep:
1. `.github/copilot-instructions.md` ✅ KEEP (golden rules jsou užitečné)
2. FilterParser fix ✅ KEEP (opravuje broken tests)

---

## 🎬 Rollback Plán

### Krok 1: Smazat Nové Soubory
```bash
rm backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java
rm e2e/specs/monitoring/grafana-sso-debug.spec.ts
rm e2e/debug-grafana-sso.spec.ts
rm e2e/test-auth-endpoint.js
rm diagnostics/build-*.txt
rm diagnostics/tests/error-summary-*.md
```

### Krok 2: Revert Nginx Config
```bash
git checkout docker/nginx/nginx.conf
```

**Nebo ruční editace:**
- Smazat `auth_request /_auth/grafana;` bloky (řádky 126-135)
- Vrátit původní simple proxy bez auth

### Krok 3: Revert Grafana Config
```bash
git checkout docker/grafana/grafana.ini.template
```

**Nebo ruční editace:**
- Vypnout `[auth.jwt]` sekci
- Vrátit `disable_login_form = false` (uživatelé se přihlásí ručně)

### Krok 4: Keep FilterParser Fix
```bash
# FilterParser.java změny NECHÁT - opravují broken tests
git add backend/src/main/java/cz/muriel/core/metamodel/filter/FilterParser.java
```

### Krok 5: Clean Rebuild
```bash
make clean-fast
# Nebo pokud selže:
docker-compose down -v
docker system prune -f
make clean-fast
```

---

## 🚀 Alternativní Řešení (Doporučené)

### Varianta A: Přesunout Grafana do FE (DOPORUČENO)
**Místo iframe SSO → API proxy**

```typescript
// frontend/src/services/monitoring.ts
export async function getGrafanaDashboard(dashboardId: string) {
  // Backend proxy na Grafana API
  const response = await fetch(`/api/monitoring/dashboards/${dashboardId}`);
  return response.json();
}
```

**Backend endpoint:**
```java
@GetMapping("/api/monitoring/dashboards/{id}")
public Dashboard getGrafanaDashboard(@PathVariable String id, Authentication auth) {
  // 1. Ověř Keycloak JWT
  // 2. Zavolej Grafana API s admin credentials
  // 3. Vrať data do FE
  return grafanaClient.getDashboard(id);
}
```

**Výhody:**
- ✅ Žádné iframe security issues
- ✅ Úplná kontrola nad UI
- ✅ React komponenty místo embedded dashboardů
- ✅ Jednodušší autentizace (jen backend ↔ Grafana)

### Varianta B: Grafana s Basic Auth (Jednoduchá)
**Fallback na credentials:**
- Uživatelé dostanou Grafana credentials při onboardingu
- Iframe zobrazí Grafana login form
- Po přihlášení session cookie

**Výhody:**
- ✅ Funguje okamžitě
- ✅ Žádné změny kódu
- ✅ Grafana out-of-the-box

**Nevýhody:**
- ❌ Uživatelé musí pamatovat další credentials
- ❌ Dvojí autentizace (Keycloak + Grafana)

### Varianta C: Odložit SSO (Quick Win)
**Prozatímní řešení:**
1. Vypnout iframe embedding zcela
2. Grafana link v FE → otevře nové okno
3. Uživatelé se přihlásí do Grafany samostatně

**Výhody:**
- ✅ Zero effort
- ✅ Funguje ihned
- ✅ Žádné security risks

---

## 📈 Co Jsme Se Naučili

### 1. Nginx auth_request Je Tricky
- Vyžaduje synchronní odpověď (< 100ms)
- Headers forwarding je křehký
- JWT validation v auth_request = latency

### 2. Grafana JWT Auth Je Komplikovaná
- JWKS endpoint musí být dostupný z Grafany
- TTL musí být krátký (security) ale ne moc (UX)
- Claims mapping (`sub`, `email`, `org_id`) je strict

### 3. Make Clean Není Dostatečně Robustní
- Devcontainer overlay se nenačítá správně
- Volume mounts se nerefreshují
- Služby zůstávají v "Created" stavu

### 4. Backend Dependency Injection Je Křehká
- Přidání nového controlleru může rozbít startup
- JwtDecoder bean má circular dependency risk
- SecurityConfig + @RestController timing issues

---

## 💡 Závěr & Doporučení

### Immediate Action (DNES):
```bash
# 1. Rollback Grafana změn
git checkout docker/nginx/nginx.conf
git checkout docker/grafana/grafana.ini.template
rm backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java

# 2. Keep FilterParser fix
git add backend/src/main/java/cz/muriel/core/metamodel/filter/FilterParser.java
git commit -m "fix: JOOQ FilterParser type handling"

# 3. Clean rebuild
make clean-fast

# 4. Test že backend běží
curl http://localhost:8080/actuator/health
```

### Long Term (PŘÍŠTÍ SPRINT):
**Implementovat Varianta A: API Proxy**
- Frontend komponenty pro dashboardy
- Backend proxy endpoint
- Grafana API client
- **ETA:** 2-3 dny (vs. 7 dní na broken SSO)

### Co Zahodit:
- ❌ Iframe SSO approach
- ❌ Nginx auth_request middleware
- ❌ JWT header forwarding magic
- ❌ Stovky debug MD souborů

### Co Zachovat:
- ✅ `.github/copilot-instructions.md` (golden rules)
- ✅ `GrafanaJwtService.java` (může být užitečný i pro API proxy)
- ✅ JWKS endpoint (funguje správně)
- ✅ FilterParser fix (opravuje testy)

---

## 🔧 Rollback Commands

```bash
cd /Users/martinhorak/Projects/core-platform

# Smazat Grafana SSO soubory
rm backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java
rm e2e/specs/monitoring/grafana-sso-debug.spec.ts
rm e2e/debug-grafana-sso.spec.ts
rm e2e/test-auth-endpoint.js

# Revert config změny
git checkout docker/nginx/nginx.conf
git checkout docker/grafana/grafana.ini.template

# Commitnout FilterParser fix (ten je dobrý)
git add backend/src/main/java/cz/muriel/core/metamodel/filter/FilterParser.java
git add .github/copilot-instructions.md
git commit -m "fix: JOOQ FilterParser type handling + golden rules"

# Clean rebuild
docker-compose down -v
make clean-fast

# Verify
docker ps
curl http://localhost:8080/actuator/health
```

---

**Shrnutí:** 7 dní → 0 progress → backend broken → ROLLBACK → přesun do FE API proxy (2-3 dny)

**Next Steps:** Tvoje rozhodnutí - rollback nebo pokračovat v debuggingu?
