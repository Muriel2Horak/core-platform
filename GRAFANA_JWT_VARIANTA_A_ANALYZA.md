# Detailní analýza změn od "Cestou A" promptu

**Datum:** 25. října 2025  
**Kontext:** Implementace JWKS přes Nginx HTTPS (Varianta A)

---

## 🎯 Zadání - Varianta A (JWKS přes Nginx, HTTPS uvnitř docker sítě)

**Cíl:** 
- Nginx proxy pro JWKS přes HTTPS
- Grafana volá `https://admin.core-platform.local/.well-known/jwks.json`
- Backend expose JWKS na HTTP `http://backend:8080/.well-known/jwks.json`
- Použít `extra_hosts` pro Grafana → statická IP Nginxu
- Provisioning idempotentní, fail-fast, hlučný (INFO logs)
- Frontend nikdy nehádá orgId, vždy z `/api/me`

---

## ✅ Co jsem ÚSPĚŠNĚ implementoval

### 1. **Grafana JWT konfigurace** (`docker/grafana/grafana.ini`)

**Změna:** `jwk_set_file` → `jwk_set_url` s HTTPS

```diff
[auth.jwt]
enabled = true
header_name = X-Org-JWT
- jwk_set_file = /etc/grafana/jwks.json
+ jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json
auto_sign_up = true
username_claim = preferred_username
email_claim = email
- email_attribute_path = email
+ org_id_claim = orgId
```

**Důvod:** Podle zadání - Grafana má volat HTTPS JWKS přes Nginx, ne lokální soubor.

---

### 2. **Backend JWT Service - numerický orgId claim**

**Soubor:** `backend/src/main/java/cz/muriel/core/monitoring/bff/jwt/JwtService.java`

**Změna:** `orgId` jako top-level INTEGER claim (ne string)

```diff
Map<String, Object> claims = new HashMap<>();
claims.put("sub", sub);
claims.put("preferred_username", username);
claims.put("email", email);
- claims.put("role", role != null ? role : "Admin");
+ claims.put("orgId", orgId); // ← NUMERICKÝ, top-level
claims.put("iat", issuedAt.toEpochSecond());
claims.put("exp", expiresAt.toEpochSecond());
claims.put("jti", jti);
```

**Důvod:** Grafana očekává `org_id_claim = orgId` jako číslo (podle zadání).

---

### 3. **Provisioning - INFO logging**

**Soubor:** `backend/src/main/java/cz/muriel/core/monitoring/bff/provisioning/GrafanaProvisioningService.java`

**Přidáno 15+ INFO log statements:**

```java
log.info("🔍 [Provisioning] Ensuring Grafana user exists: email={}", email);
log.info("➕ [Provisioning] Creating new Grafana user: email={}, name={}", email, fullName);
log.info("✅ [Provisioning] User created: email={}, userId={}", email, userId);
log.info("👥 [Provisioning] Ensuring user is member: userId={}, email={}, orgId={}, role={}", ...);
log.info("➕ [Provisioning] Adding user to org: userId={}, email={}, orgId={}, role={}", ...);
log.info("✅ [Provisioning] User added to org: userId={}, orgId={}", userId, orgId);
log.info("🔄 [Provisioning] Setting active org: userId={}, orgId={}", userId, orgId);
log.info("✅ [Provisioning] Active org set: userId={}, orgId={}", userId, orgId);
log.info("✅ [Provisioning] Complete: user={}, userId={}, orgId={}", username, userId, orgId);
```

**Důvod:** Fail-fast, hlučný provisioning (podle zadání).

---

### 4. **Auth Bridge - 500 při selhání**

**Soubor:** `backend/src/main/java/cz/muriel/core/monitoring/bff/auth/AuthRequestController.java`

**Změna:** Provisioning selhání → HTTP 500 (ne 403)

```diff
try {
    provisioningService.ensureUserProvisioned(username, email, fullName, tenantOrgId);
} catch (Exception e) {
-   log.error("Provisioning failed for user={}, orgId={}", username, tenantOrgId, e);
-   return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
+   log.error("❌ [Auth Bridge] Provisioning failed for user={}, orgId={}: {}", 
+       username, tenantOrgId, e.getMessage());
+   return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
+       .header("X-Error-Reason", "Provisioning failed")
+       .build();
}
```

**Důvod:** Nginx nesmí pustit request do Grafany s rozbitou session (podle zadání).

---

## ❌ Co jsem NEUDĚLAL (ale měl podle zadání)

### 1. **Nginx statická IP + Grafana extra_hosts**

**Podle zadání:**
```yaml
services:
  nginx:
    networks:
      core-net:
        ipv4_address: 172.18.0.10
  
  grafana:
    extra_hosts:
      - "admin.core-platform.local:172.18.0.10"
```

**Stav:** ❌ **NEIMPLEMENTOVÁNO**  
**Důvod:** Nezměnil jsem docker-compose, protože jsem teprve zjistil jiný problém (viz níže).

---

### 2. **Diagnostické příkazy (make monitoring-doctor)**

**Podle zadání - 5 kontrol:**

```bash
# 1. JWKS dostupnost z Grafany
docker exec core-grafana curl -sk https://admin.core-platform.local/.well-known/jwks.json | jq .keys[0].kid

# 2. Auth bridge mintage
curl -skI https://admin.core-platform.local/_auth/grafana -H "Cookie: at=..." | grep -E "Grafana-Jwt|X-Grafana-Org-Id"

# 3. Grafana JWT ověření
docker logs core-grafana | grep -i "jwt\|jwk"

# 4. Aktivní org uživatele
curl -su admin:admin http://core-grafana:3000/api/users/lookup?loginOrEmail=test_admin | jq .orgId

# 5. Dashboard bez 404
curl -skI "https://admin.core-platform.local/core-admin/monitoring/d/<UID>?orgId=2"
```

**Stav:** ❌ **NEIMPLEMENTOVÁNO**  
**Důvod:** Ještě jsem nedostal systém do funkčního stavu.

---

## 🐛 Co jsem ZJISTIL (nový problém)

### **ROOT CAUSE: Backend se nemůže připojit ke Keycloaku**

**Chyba v logs:**
```json
{
  "message": "Failed to get roles",
  "stack_trace": "Caused by: org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found on POST request for \"https://keycloak:8443/realms/core-platform/protocol/openid-connect/token\": {\"error\":\"Realm does not exist\"}"
}
```

**Problém:** Backend hledá realm **`core-platform`**, ale správný realm je **`admin`**.

**Environment proměnné v backendu:**
```bash
KEYCLOAK_TARGET_REALM=core-platform  ❌ ŠPATNĚ
KEYCLOAK_ADMIN_REALM=core-platform   ❌ ŠPATNĚ
OIDC_ISSUER_URI=https://admin.core-platform.local/realms/admin  ✅ SPRÁVNĚ
OIDC_JWK_SET_URI=https://keycloak:8443/realms/admin/protocol/openid-connect/certs  ✅ SPRÁVNĚ
```

**Zdroj problému:**

- **`docker/.env`** má:
  ```properties
  KEYCLOAK_ADMIN_REALM=core-platform     ❌ ŠPATNĚ
  KEYCLOAK_TARGET_REALM=core-platform    ❌ ŠPATNĚ
  ```

- **Kořenový `.env`** má správně:
  ```properties
  KEYCLOAK_TARGET_REALM=admin  ✅ SPRÁVNĚ
  ```

---

## ⚠️ HARDCODED IP ADRESY?

**Odpověď:** ❌ **NE, žádné hardcoded IP jsem nepřidal.**

**Jediná zmínka o IP byla v zadání:**
- Nginx statická IP: `172.18.0.10`
- Grafana `extra_hosts`: `admin.core-platform.local:172.18.0.10`

**Ale tyto změny jsem NEIMPLEMENTOVAL** - žádný docker-compose soubor jsem neupravil.

---

## 🔄 Manuální workaroundy (které NEJSOU v kódu)

### 1. **Keycloak email fix**

**Provedeno ručně v terminálu:**
```bash
docker exec core-keycloak sh -c '/opt/keycloak/bin/kcadm.sh config credentials \
  --server https://keycloak:8443 --realm master --user admin --password admin && \
  /opt/keycloak/bin/kcadm.sh update users/1610c31d-6f9f-4080-b929-8eb50005ca43 \
  -r admin -s email=test.admin@example.com -s firstName=Test -s lastName=Administrator'
```

**Důvod:** Keycloak realm import (`realm-admin.json`) nepřenesl `email`, `firstName`, `lastName`.

**Stav:** ✅ DOČASNĚ OPRAVENO (ale není trvalé - zmizí po `make clean`).

---

### 2. **Grafana Docker image rebuild**

**Provedeno:**
```bash
docker compose build grafana
docker compose up -d grafana
```

**Důvod:** Grafana Dockerfile už měl CA certifikát (`COPY ssl/cert.pem + update-ca-certificates`), ale byl potřeba rebuild aby se aplikoval.

**Výsledek:** ✅ Grafana nyní věří self-signed SSL certu pro `admin.core-platform.local`.

---

## 📊 Současný stav systému

### ✅ Funguje:
- Grafana kontejner: `healthy`
- Grafana JWKS SSL: Věří `admin.core-platform.local` certifikátu
- Backend provisioning logika: Správně loguje INFO (když Keycloak funguje)
- JWT Service: Generuje numerický `orgId` claim

### ❌ Nefunguje:
- **Backend nemůže autentizovat ke Keycloaku** → `KEYCLOAK_TARGET_REALM=core-platform` místo `admin`
- Backend vrací **500 Internal Server Error** kvůli Keycloak auth selhání
- Frontend dostává **500** z `/api/auth/session`
- Nginx vrací **403 Forbidden** pro `/api/frontend-logs` (auth bridge failuje)

### ⏸️ Neotestováno:
- Nginx statická IP + Grafana `extra_hosts` (neimplementováno)
- JWKS dostupnost z Grafany přes HTTPS
- Grafana JWT validace
- Diagnostické příkazy

---

## 🎯 Co je potřeba dodělat (podle zadání A)

### 1. **Opravit Keycloak realm v .env** (BLOKUJÍCÍ)
```diff
# docker/.env
- KEYCLOAK_ADMIN_REALM=core-platform
+ KEYCLOAK_ADMIN_REALM=admin

- KEYCLOAK_TARGET_REALM=core-platform
+ KEYCLOAK_TARGET_REALM=admin
```

**Otázka:** Je `docker/.env` generovaný ze šablony? Mám ho editovat přímo nebo přes šablonu?

---

### 2. **Nginx statická IP + Grafana extra_hosts**

**V docker-compose:**
```yaml
services:
  nginx:
    networks:
      core-net:
        ipv4_address: 172.18.0.10
  
  grafana:
    extra_hosts:
      - "admin.core-platform.local:172.18.0.10"
```

---

### 3. **Diagnostické příkazy (make monitoring-doctor)**

Vytvořit Makefile target s 5 kontrolami ze zadání.

---

### 4. **Opravit Keycloak realm-admin.json import**

Aby `email`, `firstName`, `lastName` přežily `make clean`.

---

## 📝 Souhrn změněných souborů

### Kód (commitable):
1. ✅ `docker/grafana/grafana.ini` - JWT konfigurace (jwk_set_url, org_id_claim)
2. ✅ `backend/.../JwtService.java` - Numerický orgId claim
3. ✅ `backend/.../GrafanaProvisioningService.java` - INFO logging (15+ log statements)
4. ✅ `backend/.../AuthRequestController.java` - 500 místo 403 při provisioning selhání

### Manuální (netrvalé):
1. ⚠️ Keycloak user email - `kcadm.sh` příkaz (zmizí po restart)
2. ⚠️ Grafana Docker image - rebuild (je v cache, ale není v Dockerfile změna)

### Neupravené (mělo by být podle zadání):
1. ❌ `docker-compose.yml` - Nginx statická IP
2. ❌ `docker-compose.yml` - Grafana extra_hosts
3. ❌ `docker/.env` - Keycloak realm opravy
4. ❌ `Makefile` - monitoring-doctor target
5. ❌ `keycloak/realm-admin.json` - Email import fix

---

## 🚨 Aktuální blokující problém

**Backend nemůže se připojit ke Keycloaku kvůli špatnému realm.**

**Potřebuji vědět:**
1. Je `docker/.env` generovaný? Kde je šablona?
2. Mám editovat `docker/.env` přímo, nebo přes šablonu a regenerovat?
3. Po opravě .env restartovat jen backend, nebo celý compose?

---

**Čekám na instrukce jak správně opravit .env soubor, pak můžu pokračovat s implementací zbytku Varianty A.**
