# 🔍 Audit a Analýza Prostředí Core Platform

**Datum:** 25. října 2025  
**Autor:** GitHub Copilot  
**Účel:** Komplexní zmapování konfigurace, identifikace nekonzistencí a doporučení pro stabilitu

---

## 📋 Executive Summary

Tento dokument poskytuje hloubkovou analýzu prostředí **Core Platform**, zahrnující:
- ✅ Síťovou topologii a DNS routing
- ✅ SSL/TLS infrastrukturu
- ✅ Autentizační a autorizační mechanismy (JWT, OIDC)
- ✅ CORS konfiguraci napříč službami
- ✅ Nginx reverse proxy setup
- ⚠️ Identifikované nekonzistence a bezpečnostní rizika

---

## 1️⃣ Přehled Klíčových Konfiguračních Souborů

| Kategorie | Soubor | Popis | Kritičnost |
|-----------|--------|-------|------------|
| **Orchestrace** | `Makefile` | Hlavní entry point pro build/deploy/test operace | 🔴 Kritický |
| | `docker/docker-compose.yml` | Definice všech služeb, závislostí, portů, volumes | 🔴 Kritický |
| | `.devcontainer/docker-compose.devcontainer.yml` | Dev overrides (watch mode, hot reload) | 🟡 Důležitý |
| **Proměnné prostředí** | `docker/.env` | Centrální konfigurace (hesla, URL, JWT keys, CORS) | 🔴 Kritický |
| | `docker/.env.{development,staging,production}` | Environment-specific overrides | 🟡 Důležitý |
| **Nginx** | `docker/nginx/nginx-ssl.conf.template` | Reverse proxy template (envsubst rendering) | 🔴 Kritický |
| | `docker/nginx/start-nginx.sh` | Entrypoint script (template → conf) | 🟡 Důležitý |
| **Keycloak** | `docker/keycloak/realm-admin.json` | Realm definice (users, clients, roles) | 🔴 Kritický |
| | `docker/keycloak/Dockerfile` | Custom image s realm importem | 🟡 Důležitý |
| | `docker/keycloak/generate-realm.sh` | Template rendering pro multi-domain | 🟡 Důležitý |
| **Backend** | `backend/src/main/resources/application.properties` | Spring Boot konfigurace | 🔴 Kritický |
| | `backend/src/main/java/.../SecurityConfig.java` | Spring Security + CORS + JWT decoder | 🔴 Kritický |
| | `backend/pom.xml` | Maven dependencies | 🟡 Důležitý |
| **Frontend** | `frontend/src/services/keycloakService.js` | Keycloak JS adapter setup | 🟡 Důležitý |
| | `frontend/package.json` | NPM dependencies | 🟡 Důležitý |
| **Grafana** | `docker/grafana/grafana.ini` | Grafana config (JWT auth, OAuth, subpath) | 🟡 Důležitý |
| | `docker/grafana/Dockerfile` | SSL cert trust setup | 🟡 Důležitý |
| **SSL/TLS** | `docker/ssl/cert.pem`, `key.pem` | Wildcard self-signed certs | 🔴 Kritický |
| | `scripts/docker/dev-start-ssl.sh` | Automatické generování SSL certifikátů | 🟡 Důležitý |
| **Skripty** | `scripts/**/*.sh` | Automation (setup, testing, monitoring) | 🟢 Podpůrný |

---

## 2️⃣ Analýza Síťové Topologie a DNS

### 2.1. Docker Network Architektura

```
┌─────────────────────────────────────────────────────────────────┐
│                    HOST MACHINE (macOS)                         │
│  /etc/hosts:                                                    │
│    127.0.0.1  core-platform.local                              │
│    127.0.0.1  admin.core-platform.local                        │
│    127.0.0.1  *.core-platform.local (wildcard via DNS)        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼ (ports 80, 443, 8081, 5432, ...)
┌─────────────────────────────────────────────────────────────────┐
│              Docker Network: core-net (172.18.0.0/16)           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ nginx (172.18.0.20 - STATIC IP)                          │  │
│  │  - Ports: 80, 443                                        │  │
│  │  - Server names: $DOMAIN *.$DOMAIN                       │  │
│  │  - Upstreams: frontend:80, backend:8080, keycloak:8443  │  │
│  └──────────────────────────────────────────────────────────┘  │
│           │           │            │            │               │
│           ▼           ▼            ▼            ▼               │
│  ┌────────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────┐  │
│  │ frontend   │ │ backend  │ │ keycloak   │ │ grafana      │  │
│  │ :80        │ │ :8080    │ │ :8443      │ │ :3000        │  │
│  └────────────┘ └──────────┘ └────────────┘ └──────────────┘  │
│                      │              │                           │
│                      ▼              ▼                           │
│                 ┌──────────────────────┐                        │
│                 │ db (PostgreSQL)      │                        │
│                 │ :5432                │                        │
│                 └──────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2. DNS Resolution Flow

| Požadavek | Vstupní bod | Routing | Cílová služba |
|-----------|-------------|---------|---------------|
| `https://core-platform.local/` | Nginx:443 | `location /` → `proxy_pass http://frontend` | frontend:80 |
| `https://admin.core-platform.local/` | Nginx:443 | `location /` → `proxy_pass http://frontend` | frontend:80 |
| `https://admin.core-platform.local/api/health` | Nginx:443 | `location /api/` → `proxy_pass http://backend` | backend:8080 |
| `https://admin.core-platform.local/realms/admin` | Nginx:443 | `location /realms/` → `proxy_pass https://keycloak` | keycloak:8443 |
| `https://admin.core-platform.local/core-admin/monitoring/` | Nginx:443 | `location ^~ /core-admin/monitoring/` → `auth_request /_auth/grafana` → `proxy_pass http://grafana` | backend:8080 (auth) → grafana:3000 |

### 2.3. Port Mapping (Host ↔ Container)

| Služba | Container Port | Host Port | Protokol | Přístup |
|--------|---------------|-----------|----------|---------|
| **Nginx** | 80, 443 | 80, 443 | HTTP/S | Veřejný (gateway) |
| **Keycloak** | 8443 | 8081 | HTTPS | Veřejný (admin) |
| **Backend** | 8080 | - | HTTP | Pouze přes Nginx |
| **Frontend** | 80 | - | HTTP | Pouze přes Nginx |
| **Grafana** | 3000 | - | HTTP | Pouze přes Nginx (auth) |
| **PostgreSQL** | 5432 | 5432 | TCP | Localhost debug |
| **pgAdmin** | 80 | 5050 | HTTP | Localhost admin |
| **Prometheus** | 9090 | 9091 | HTTP | Localhost monitoring |
| **Loki** | 3100 | 3100 | HTTP | Localhost logs |
| **Kafka UI** | 8080 | 8090 | HTTP | Localhost streaming |
| **Cube** | 4000 | 4000 | HTTP | Localhost analytics |

### ⚠️ **NEKONZISTENCE #1: Nginx Static IP**

**Zjištění:**
- Nginx má pevně přiřazenou IP `172.18.0.20` v `docker-compose.yml`
- **Účel:** Stabilní DNS resolution z Grafana kontejneru pro JWKS URL
- **Problém:** Dokumentace chybí! Není jasné, proč je static IP nutná

**Doporučení:**
```markdown
1. Přidat komentář do docker-compose.yml:
   ```yaml
   networks:
     core-net:
       ipv4_address: 172.18.0.20  # CRITICAL: Static IP for Grafana JWKS resolution
                                    # Grafana requires stable DNS for jwk_set_url validation
   ```

2. Alternativně: Použít Docker DNS aliasy místo static IP:
   ```yaml
   networks:
     core-net:
       aliases:
         - nginx-stable
   ```

---

## 3️⃣ Analýza SSL/TLS Infrastruktury

### 3.1. Generování a Správa Certifikátů

**Skript:** `scripts/docker/dev-start-ssl.sh`

**Proces:**

1. **Kontrola existence certifikátů** v `docker/ssl/`
2. **Validace doménového pokrytí** pomocí `openssl x509 -text`
3. **Generování nových certifikátů** pokud chybí nebo jsou neplatné:

```bash
openssl req -x509 -nodes -days 365 \
    -newkey rsa:2048 \
    -keyout docker/ssl/key.pem \
    -out docker/ssl/cert.pem \
    -config <(
        echo '[dn]'
        echo 'CN=core-platform.local'
        echo '[req]'
        echo 'distinguished_name = dn'
        echo '[SAN]'
        echo 'subjectAltName=DNS:core-platform.local,DNS:*.core-platform.local,DNS:localhost,IP:127.0.0.1'
    ) \
    -extensions v3_req
```

**Vlastnosti generovaných certifikátů:**

- **Typ:** Self-signed (vlastnoručně podepsaný)
- **Platnost:** 365 dní
- **CN (Common Name):** `core-platform.local`
- **SAN (Subject Alternative Names):**
  - `core-platform.local`
  - `*.core-platform.local` (wildcard pro subdomény)
  - `localhost`
  - `127.0.0.1`

### 3.2. SSL Termination v Nginx

**Konfigurace:** `docker/nginx/nginx-ssl.conf.template` (lines 49-66)

```nginx
server {
    listen 443 ssl http2;
    server_name $DOMAIN *.$DOMAIN;  # core-platform.local *.core-platform.local
    
    # SSL Configuration
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

**SSL Termination Architektura:**

```
Browser (HTTPS) → Nginx:443 (SSL termination) → Backend/Frontend/Keycloak (HTTP)
                       ↓
                  [cert.pem, key.pem]
```

- **Externí komunikace:** HTTPS (šifrováno)
- **Interní Docker síť:** HTTP (nešifrováno, ale izolovaná síť)
- **Výhoda:** Jednodušší konfigurace backendu, menší overhead

### 3.3. Keycloak HTTPS Komunikace

**⚠️ NEKONZISTENCE #2: Keycloak běží na HTTPS uvnitř Docker sítě**

**Zjištění z docker-compose.yml:**

```yaml
keycloak:
  ports:
    - "8081:8443"  # HTTPS port
  # ...
```

**Zjištění z nginx-ssl.conf.template:**

```nginx
upstream keycloak {
    server keycloak:8443;  # HTTPS connection
}

location /realms/ {
    proxy_pass https://keycloak;  # ❗️ HTTPS within Docker network
    proxy_ssl_verify off;         # ❗️ Self-signed cert ignored
}
```

**Problém:**

- Keycloak běží na HTTPS i uvnitř Docker sítě (nestandardní)
- Nginx musí deaktivovat SSL verifikaci (`proxy_ssl_verify off`)
- Přidává zbytečnou režii při komunikaci Nginx ↔ Keycloak

**Doporučení:**

```markdown
### Varianta A: Přepnout Keycloak na HTTP uvnitř Docker sítě
**Změny:**
1. docker-compose.yml:
   ```yaml
   keycloak:
     ports:
       - "8081:8080"  # HTTP port místo 8443
   ```

2. nginx-ssl.conf.template:
   ```nginx
   upstream keycloak {
       server keycloak:8080;  # HTTP connection
   }
   
   location /realms/ {
       proxy_pass http://keycloak;  # HTTP within Docker network
       # proxy_ssl_verify off;  # ODSTRANIT
   }
   ```

3. backend/application.properties:
   ```properties
   keycloak.admin.base-url=http://keycloak:8080  # Již OK
   ```

**Výhody:** Jednodušší konfigurace, vyšší výkon, standardní praxe

### Varianta B: Zachovat HTTPS ale s vlastní CA
**Změny:**
1. Vytvořit internal CA certifikát
2. Přidat CA do trusted store v Nginx kontejneru
3. Aktivovat `proxy_ssl_verify on` s `proxy_ssl_trusted_certificate`

**Výhody:** End-to-end šifrování (i uvnitř Docker sítě)
**Nevýhody:** Složitější setup, vyšší režie
```

### 3.4. Grafana SSL Trust Chain

**Konfigurace:** `docker/grafana/Dockerfile`

```dockerfile
# Copy SSL certificates for JWKS validation
COPY ssl/cert.pem /usr/local/share/ca-certificates/core-platform.crt
RUN update-ca-certificates
```

**Účel:**

- Grafana potřebuje důvěřovat Nginx SSL certifikátu
- `jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json` vyžaduje HTTPS
- Self-signed certifikát musí být v trusted store

**Flow:**

```
Grafana JWT Verification
  ↓
jwk_set_url (HTTPS request)
  ↓
Nginx SSL (cert.pem)
  ↓
✅ Trusted (díky update-ca-certificates)
```

### ⚠️ **NEKONZISTENCE #3: Chybějící SSL certifikát po `make clean`**

**Zjištění:**

- Při `make clean` nejsou automaticky regenerovány SSL certifikáty
- Nginx selže na startu s chybou: `host not found in upstream "grafana:3000"`
- **Root cause:** Nginx startuje před Grafanou kvůli chybě v depends_on

**Aktuální stav:**

```yaml
nginx:
  depends_on:
    grafana:
      condition: service_healthy  # ✅ Správně
```

**Problém:**

- Pokud `docker/ssl/` neexistuje, `dev-start-ssl.sh` ho vygeneruje
- Ale `Makefile` → `make clean` nevolá `dev-start-ssl.sh`!
- Uživatel musí manuálně spustit `scripts/docker/dev-start-ssl.sh`

**Doporučení:**

```makefile
# Makefile
clean: validate-env
    @echo "🧹 Cleaning environment..."
    @$(MAKE) generate-ssl-certs  # ✅ PŘIDAT
    docker compose -f docker/docker-compose.yml --env-file .env down -v
    # ...

generate-ssl-certs:
    @echo "🔐 Generating SSL certificates..."
    @bash scripts/docker/dev-start-ssl.sh --certs-only  # ✅ NOVÁ FUNKCE
```

---

## 4️⃣ Analýza Autentizace a JWT Flow

### 4.1. Keycloak jako Identity Provider

**Realm:** `admin` (definováno v `docker/keycloak/realm-admin.json`)

**Klienti:**

| Client ID | Grant Type | Redirect URIs | Účel |
|-----------|------------|---------------|------|
| `web` | Authorization Code + PKCE | `https://core-platform.local/*`, `https://admin.core-platform.local/*` | Frontend SPA |
| `backend-admin-service` | Client Credentials | N/A | Backend → Keycloak Admin API |
| `grafana` | Authorization Code | `https://*.core-platform.local/core-admin/monitoring/login/generic_oauth` | Grafana OAuth2 |

**Uživatelé (z realm-admin.json):**

| Username | Email | Roles | Heslo |
|----------|-------|-------|-------|
| `test` | `test.user@example.com` | `CORE_ROLE_USER` | `Test.1234` |
| `test_admin` | `test.admin@example.com` | `CORE_ROLE_ADMIN` | `Test.1234` |

✅ **Email je správně přítomen v realm-admin.json** (line 91)

### 4.2. JWT Token Structure

**Issuer URL Pattern:**

```
https://{subdomain}.core-platform.local/realms/{realm}
```

**Příklady:**

- `https://admin.core-platform.local/realms/admin`
- `https://tenant-a.core-platform.local/realms/tenant-a` (budoucí)

**Token Claims (z Keycloak):**

```json
{
  "iss": "https://admin.core-platform.local/realms/admin",
  "aud": "api",
  "sub": "e1f569ed-314f-4497-b344-81220c93c875",
  "preferred_username": "test",
  "email": "test.user@example.com",
  "email_verified": true,
  "realm_access": {
    "roles": ["CORE_ROLE_USER"]
  },
  "tenant": "admin",  // Custom claim
  "allowed-origins": [
    "https://admin.core-platform.local",
    "https://core-platform.local"
  ]
}
```

### 4.3. Backend JWT Validation (Dynamic Issuer)

**Konfigurace:** `backend/src/main/java/.../DynamicJwtDecoder.java`

**Mechanismus:**

1. **Request přijde s JWT tokenem**
2. **DynamicJwtDecoder extrahuje `iss` claim**
3. **Stáhne JWKS z `{iss}/.well-known/openid-configuration`**
4. **Validuje token proti JWKS**

**Výhoda:** Podporuje multi-tenant architekturu (každý tenant má vlastní realm)

**Zjištěné URL z application.properties:**

```properties
# ❌ STARÁ KONFIGURACE (zakomentováno):
# spring.security.oauth2.resourceserver.jwt.issuer-uri=...

# ✅ NOVÁ KONFIGURACE (dynamic):
security.oauth2.base-domain=core-platform.local
security.oauth2.audience=api
```

### 4.4. CORS Konfigurace v SecurityConfig

**Soubor:** `backend/src/main/java/.../SecurityConfig.java` (lines 152-175)

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    List<String> origins = Arrays.stream(corsOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    // ✅ Použití setAllowedOriginPatterns() pro wildcard support
    cfg.setAllowedOriginPatterns(origins);
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
}
```

**Klíčové vlastnosti:**

- **`setAllowedOriginPatterns()`** místo `setAllowedOrigins()` → podporuje wildcards
- **`setAllowCredentials(true)`** → povoluje cookies/auth headers
- **Čte z:** `@Value("${cors.origins}")` → `docker/.env` → `CORS_ORIGINS`

### 4.5. CORS Proměnné Prostředí

**docker/.env (line 50):**

```properties
CORS_ORIGINS=http://localhost:3000,https://core-platform.local,https://*.core-platform.local,https://your-production-domain.com
```

✅ **OPRAVENO:** Wildcard `https://*.core-platform.local` je přítomen

**docker-compose.yml (backend environment):**

```yaml
backend:
  environment:
    - CORS_ORIGINS=${CORS_ORIGINS}
```

**application.properties (backend):**

```properties
cors.origins=${CORS_ORIGINS:https://*.${DOMAIN:core-platform.local},https://${DOMAIN:core-platform.local},http://localhost}
```

**⚠️ NEKONZISTENCE #4: Duplicitní CORS definice**

**Problém:**

- CORS je definován na 2 místech:
  1. `docker/.env` → explicitní hodnota
  2. `application.properties` → fallback default

**Riziko:**

- Pokud `.env` chybí nebo je prázdný, použije se default z `application.properties`
- Default **NEOBSAHUJE** `https://your-production-domain.com`!
- Při deployment do produkce může selhat CORS!

**Doporučení:**

```markdown
### Řešení: Unifikovat CORS konfiguraci

1. **Odstranit fallback** z application.properties:
   ```properties
   cors.origins=${CORS_ORIGINS}  # Bez defaultu!
   ```

2. **Vynucovat přítomnost** CORS_ORIGINS v .env:
   ```bash
   # Přidat do validate-env v Makefile
   @test -n "$$CORS_ORIGINS" || (echo "❌ CORS_ORIGINS is not set!" && exit 1)
   ```

3. **Dokumentovat** expected format:
   ```properties
   # docker/.env
   # CORS_ORIGINS: Comma-separated list of allowed origins
   # Supports wildcards: https://*.domain.com
   # Example: https://app.com,https://*.app.com,http://localhost:3000
   CORS_ORIGINS=https://core-platform.local,https://*.core-platform.local
   ```
```

### 4.6. Grafana JWT Authentication Flow

**Konfigurace:** `docker/grafana/grafana.ini` (lines 13-33)

```ini
[auth.jwt]
enabled = true
header_name = X-Org-JWT
username_claim = preferred_username
email_claim = email
org_id_claim = orgId  # ❗️ Custom claim z BFF
jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json
cache_ttl = 60m
auto_sign_up = true
role_attribute_path = contains(realm_access.roles[*], 'CORE_ROLE_ADMIN') && 'Admin' || 'Viewer'
```

**Flow:**

```
1. User → Frontend (React) → Login via Keycloak
   ↓
2. Frontend získá Access Token (JWT)
   ↓
3. Frontend → Nginx /core-admin/monitoring/
   ↓
4. Nginx → auth_request /_auth/grafana
   ↓
5. Backend (AuthRequestController):
   - Validuje JWT
   - Provisions user v Grafan (GET /api/users/lookup/email)
   - Přiřadí org (POST /api/user/using/{orgId})
   - Vygeneruje Grafana JWT s orgId claim
   ↓
6. Nginx → X-Org-JWT: <grafana_jwt>
   ↓
7. Grafana:
   - Validuje JWT proti jwk_set_url
   - Extrahuje orgId claim
   - Nastaví user.OrgID = orgId
   ↓
8. Grafana → Dashboard (org-isolated)
```

**⚠️ NEKONZISTENCE #5: JWKS URL používá subdomain `admin`**

**Zjištění:**

```ini
jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json
```

**Problém:**

- Hardcoded `admin` subdomain!
- Co když user přistupuje z `https://tenant-a.core-platform.local`?
- JWKS by měl být dostupný přes **statickou doménu** nebo **relativní URL**

**Důsledek:**

- Grafana nemůže validovat JWT pokud user není na `admin.` subdoméně
- Multi-tenant setup je broken

**Doporučení:**

```markdown
### Řešení #1: Použít Nginx static IP
**Aktuální stav:** Nginx má static IP `172.18.0.20`

**Změna v grafana.ini:**
```ini
jwk_set_url = https://172.18.0.20/.well-known/jwks.json
```

**Problém:** SSL certifikát neobsahuje IP v SAN!

---

### Řešení #2: Použít Docker DNS název
**Změna v grafana.ini:**
```ini
jwk_set_url = https://nginx/.well-known/jwks.json
```

**Problém:** Grafana nemá trust pro `nginx` hostname (SSL cert je pro `*.core-platform.local`)

---

### Řešení #3: Backend JWKS endpoint s HTTP
**Nejlepší řešení:**

1. Přidat HTTP JWKS endpoint v backendu:
   ```java
   @GetMapping("/.well-known/jwks.json")
   public Map<String, Object> getJwks() {
       return jwtService.getPublicJwks();
   }
   ```

2. Grafana.ini:
   ```ini
   jwk_set_url = http://backend:8080/.well-known/jwks.json
   ```

3. Výhody:
   - ✅ Žádné SSL problémy (interní HTTP)
   - ✅ Funguje pro všechny subdomény
   - ✅ Backend má již JWKS logiku
```

---

## 5️⃣ Analýza Nginx Reverse Proxy Konfigurace

### 5.1. Template Rendering Mechanismus

**Entrypoint:** `docker/nginx/start-nginx.sh`

```bash
#!/bin/sh
set -e

echo "🚀 Starting Nginx with envsubst template rendering..."
echo "📝 DOMAIN: ${DOMAIN}"

# Render template with envsubst
envsubst '$DOMAIN' < /etc/nginx/templates/nginx-ssl.conf.template > /etc/nginx/nginx.conf

echo "📋 Generated nginx.conf (first 40 lines):"
head -40 /etc/nginx/nginx.conf

echo "🌐 Starting Nginx..."
exec nginx -g 'daemon off;'
```

**Proces:**

1. Docker spustí kontejner s environment variable `DOMAIN=core-platform.local`
2. `envsubst` nahradí všechny výskyty `$DOMAIN` v template
3. Výsledný `nginx.conf` je uložen do `/etc/nginx/nginx.conf`
4. Nginx startuje s vygenerovanou konfigurací

### 5.2. Upstream Definitions

**nginx-ssl.conf.template (lines 29-45):**

```nginx
# Upstream definitions
upstream frontend {
    server frontend:80;
}

upstream backend {
    server backend:8080;
}

upstream keycloak {
    server keycloak:8443;  # ❗️ HTTPS
}

upstream grafana {
    server grafana:3000;
}
```

**Zjištění:**

- Všechny služby používají Docker DNS názvy (`frontend`, `backend`, ...)
- Pouze `keycloak` používá HTTPS port (8443)
- Ostatní jsou HTTP (80, 8080, 3000)

### 5.3. Location Blocks Priority (Nginx Matching Order)

**Priorita v Nginx:**

1. **`=` (exact match)** → Nejvyšší
2. **`^~` (prefix match, no regex)** → Vysoká
3. **`~` nebo `~*` (regex)** → Střední
4. **Prefix match** → Nízká

**Aktuální konfigurace:**

| Priority | Location | Typ | Proxy Pass |
|----------|----------|-----|------------|
| 1 | `= /.well-known/jwks.json` | Exact | `http://backend/.well-known/jwks.json` |
| 2 | `= /core-admin/monitoring` | Exact | `return 301 /core-admin/monitoring/` |
| 3 | `= /_auth/grafana` | Exact (internal) | `http://backend/internal/auth/grafana` |
| 4 | `^~ /core-admin/monitoring/public/` | Prefix | `http://grafana` (no auth) |
| 5 | `^~ /core-admin/monitoring/` | Prefix | `http://grafana` (with auth_request) |
| 6 | `/realms/` | Prefix | `https://keycloak` |
| 7 | `/admin/` | Prefix | `https://keycloak` |
| 8 | `/resources/` | Prefix | `https://keycloak` |
| 9 | `/themes/` | Prefix | `https://keycloak` |
| 10 | `/js/` | Prefix | `https://keycloak` |
| 11 | `/auth/` | Prefix (rewrite) | `https://keycloak` |
| 12 | `/internal/` | Prefix | `http://backend` |
| 13 | `/api/` | Prefix | `http://backend` |
| 14 | `~ ^/(bundle\.js\|bundle\.css)` | Regex | `http://frontend` (cached) |
| 15 | `= /` | Exact | `http://frontend` (no cache) |
| 16 | `= /index.html` | Exact | `http://frontend` (no cache) |
| 17 | `/` | Catch-all | `http://frontend` |

### ⚠️ **NEKONZISTENCE #6: Chybějící Grafana Dependency Check**

**Aktuální nginx depends_on:**

```yaml
nginx:
  depends_on:
    frontend:
      condition: service_healthy  # ✅
    backend:
      condition: service_started  # ⚠️ Mělo by být service_healthy
    keycloak:
      condition: service_started  # ⚠️ Mělo by být service_healthy
    grafana:
      condition: service_healthy  # ✅
```

**Problém:**

- Při `make clean` Nginx startuje, ale Grafana ještě není ready
- Nginx má `upstream grafana { server grafana:3000; }`
- Pokud Grafana neexistuje → Nginx error: `host not found in upstream "grafana:3000"`

**Důsledek:**

- Celý build selže kvůli Nginx startup error
- Uživatel musí manuálně restartovat Nginx po startu Grafany

**Doporučení:**

```yaml
nginx:
  depends_on:
    frontend:
      condition: service_healthy
    backend:
      condition: service_healthy  # ✅ ZMĚNIT
    keycloak:
      condition: service_healthy  # ✅ ZMĚNIT (pokud má healthcheck)
    grafana:
      condition: service_healthy
```

### 5.4. Grafana SSO Auth Flow (auth_request)

**Konfigurace (lines 94-152):**

```nginx
# 📊 GRAFANA PROXY with SSO via BFF JWT
location ^~ /core-admin/monitoring/ {
    # Authenticate via BFF
    auth_request /_auth/grafana;
    auth_request_set $grafana_token $upstream_http_grafana_jwt;
    auth_request_set $grafana_org_id $upstream_http_x_grafana_org_id;
    
    # Pass JWT and Org ID to Grafana
    proxy_set_header X-Org-JWT $grafana_token;
    proxy_set_header X-Grafana-Org-Id $grafana_org_id;
    
    # ... (WebSocket, timeouts, CSP)
    
    proxy_pass http://grafana;
}

# 📊 GRAFANA SSO: Internal auth bridge endpoint
location = /_auth/grafana {
    internal;
    proxy_pass http://backend/internal/auth/grafana;
    proxy_set_header Cookie $http_cookie;  # ❗️ CRITICAL
    proxy_set_header X-Original-URI $request_uri;
    proxy_pass_request_body off;
    proxy_set_header Content-Length "";
}
```

**Flow:**

```
1. User → GET /core-admin/monitoring/d/dashboard-123
   ↓
2. Nginx → auth_request /_auth/grafana
   ↓
3. Backend /internal/auth/grafana:
   - Čte Cookie header z original request
   - Validuje JWT z cookie "at"
   - Provisions user v Grafaně
   - Vygeneruje Grafana JWT
   - Vrátí HTTP 200 + headers:
     * Grafana-JWT: <token>
     * X-Grafana-Org-Id: 2
   ↓
4. Nginx uloží do proměnných:
   - $grafana_token = <token>
   - $grafana_org_id = 2
   ↓
5. Nginx → proxy_pass http://grafana
   + X-Org-JWT: <token>
   + X-Grafana-Org-Id: 2
   ↓
6. Grafana:
   - Validuje JWT proti jwk_set_url
   - Nastaví user.OrgID = 2
   - Vrátí dashboard data (org-filtered)
```

**⚠️ KRITICKÉ:**

- `proxy_set_header Cookie $http_cookie` je **NUTNÝ**!
- Bez toho auth_request nedostane JWT cookie
- Backend pak vrátí 401 Unauthorized

### 5.5. Cookie Security Settings

**Backend API location (lines 301-328):**

```nginx
location /api/ {
    proxy_pass http://backend;
    
    # 🔐 Cookie security hardening for iFrame SSO
    # SameSite=None required for cross-site iFrame embedding
    # Secure flag ensures HTTPS-only transmission
    proxy_cookie_flags ~ secure samesite=none;
    
    # Cookie domain configuration for multi-tenant support
    # Rewrite domain to match current subdomain (admin.*, ten.*, etc.)
    proxy_cookie_domain backend $host;
    
    # Ensure cookie path is scoped correctly
    proxy_cookie_path / /;
}
```

**Vlastnosti:**

- **`samesite=none`:** Povoluje cross-site cookie (pro iframe embedding)
- **`secure`:** Cookie pouze přes HTTPS
- **`proxy_cookie_domain backend $host`:** Přepisuje domain na aktuální subdoménu

**Příklad:**

```
Original Set-Cookie: at=<token>; Domain=backend; Path=/
Rewritten:           at=<token>; Domain=admin.core-platform.local; Path=/; Secure; SameSite=None
```

### 5.6. Caching Strategy

**Frontend assets:**

```nginx
# Cache-busted assets (bundle.js?v=timestamp, bundle.css?v=timestamp)
location ~ ^/(bundle\.js|bundle\.css) {
    proxy_pass http://frontend;
    proxy_cache_valid 200 1y;
    add_header Cache-Control "public, max-age=31536000, immutable" always;
}

# index.html - NEVER cache (always fetch fresh)
location = / {
    proxy_pass http://frontend;
    add_header Cache-Control "no-store, no-cache, must-revalidate" always;
}
```

**Strategie:**

- **Versioned assets** (`bundle.js?v=abc123`) → cache 1 rok
- **index.html** → no-cache (vždy fresh)
- **Keycloak static** (`/resources/`, `/themes/`) → cache 1 hodinu

---

## 6️⃣ Souhrn Nálezů a Kritických Doporučení

### 🔴 Kritické Problémy (Vyžadují okamžitou akci)

#### ❗️ #1: Nginx selhává při `make clean` (chybí Grafana)

**Symptom:** `nginx: [emerg] host not found in upstream "grafana:3000"`

**Root Cause:**
- Nginx startuje před Grafanou
- `depends_on: grafana: condition: service_healthy` je správně, ALE
- Grafana není spuštěna vůbec (není v docker-compose.yml default profile?)

**Řešení:**

```bash
# 1. Ověřit že Grafana je v docker-compose.yml:
grep -A 5 "grafana:" docker/docker-compose.yml

# 2. Přidat explicit start v Makefile:
clean: validate-env
    docker compose up -d grafana  # ✅ PŘIDAT
    docker compose up -d nginx
```

#### ❗️ #2: CORS selhává po `make clean` (realm mismatch)

**Symptom:** `Body: Invalid CORS request` na `/api/auth/session`

**Root Cause:**
- `.env` měl `OIDC_ISSUER_URI=.../realms/core-platform`
- Ale realm se jmenuje `admin` (realm-admin.json)

**Aktuální stav:** ✅ **OPRAVENO** v `.env` (line 45-46):

```properties
OIDC_ISSUER_URI=https://${DOMAIN}/realms/admin
OIDC_JWK_SET_URI=https://keycloak:8443/realms/admin/protocol/openid-connect/certs
```

**Verifikace:**

```bash
# Zkontrolovat že .env obsahuje správný realm:
grep "OIDC.*URI" docker/.env | grep -q "admin" && echo "✅ OK" || echo "❌ WRONG"
```

#### ❗️ #3: Grafana JWKS URL je hardcoded na `admin` subdoménu

**Symptom:** JWT validation selže pro non-admin subdomény

**Root Cause:**

```ini
# grafana.ini
jwk_set_url = https://admin.core-platform.local/.well-known/jwks.json
```

**Problém:** Multi-tenant setup broken

**Doporučení:** Použít backend HTTP endpoint

```ini
jwk_set_url = http://backend:8080/.well-known/jwks.json
```

**Implementace:**

```java
// JwksController.java
@RestController
@RequestMapping("/.well-known")
public class JwksController {
    
    @Autowired
    private JwtService jwtService;
    
    @GetMapping("/jwks.json")
    public Map<String, Object> getJwks() {
        return jwtService.getPublicJwks();
    }
}
```

### 🟡 Důležité Problémy (Měly by být opraveny brzy)

#### ⚠️ #4: Keycloak běží na HTTPS uvnitř Docker sítě

**Neefektivita:**
- Zbytečná režie SSL handshake mezi Nginx a Keycloak
- Nutnost `proxy_ssl_verify off` (bezpečnostní riziko)

**Doporučení:** Přepnout na HTTP

```yaml
# docker-compose.yml
keycloak:
  ports:
    - "8081:8080"  # HTTP místo 8443
```

```nginx
# nginx-ssl.conf.template
upstream keycloak {
    server keycloak:8080;  # HTTP
}

location /realms/ {
    proxy_pass http://keycloak;  # HTTP
}
```

#### ⚠️ #5: Duplicitní CORS konfigurace

**Riziko:** Fallback default v `application.properties` neobsahuje production domain

**Řešení:**

```properties
# application.properties - ODSTRANIT fallback:
cors.origins=${CORS_ORIGINS}  # Bez ":https://..."
```

```makefile
# Makefile - přidat validaci:
validate-env:
    @test -n "$$CORS_ORIGINS" || (echo "❌ CORS_ORIGINS required!" && exit 1)
```

#### ⚠️ #6: SSL certifikáty nejsou regenerovány při `make clean`

**Symptom:** Nginx selže pokud `docker/ssl/` chybí

**Doporučení:**

```makefile
clean: generate-ssl-certs validate-env
    # ...

generate-ssl-certs:
    @test -f docker/ssl/cert.pem || bash scripts/docker/dev-start-ssl.sh --certs-only
```

### 🟢 Doporučení pro Zlepšení (Nice-to-have)

1. **Dokumentace Static IP:**
   - Přidat komentář do docker-compose.yml proč Nginx má `172.18.0.20`

2. **Unifikace healthchecks:**
   - Backend, Keycloak: přidat `condition: service_healthy` v depends_on

3. **Monitoring alerting:**
   - Přidat Prometheus alert pro CORS errors (4xx s "Invalid CORS request")

4. **Automatické testování:**
   - E2E test pro CORS z různých subdomén
   - E2E test pro Grafana JWT flow

---

## 📊 Souhrn Stavu Prostředí

| Komponenta | Status | Poznámky |
|------------|--------|----------|
| **SSL/TLS** | 🟡 Částečně OK | Certifikáty fungují, ale nejsou automaticky generovány |
| **DNS Routing** | ✅ OK | Nginx správně routuje všechny subdomény |
| **Keycloak Realm** | ✅ OK | Realm `admin` správně nakonfigurován, email přítomen |
| **CORS** | ✅ OPRAVENO | Wildcard pattern přidán do `.env` |
| **JWT Validation** | 🟡 Částečně OK | Dynamic decoder funguje, ale JWKS URL hardcoded |
| **Grafana SSO** | 🔴 BROKEN | JWKS URL hardcoded na `admin` subdoménu |
| **Nginx Proxy** | 🟡 Částečně OK | Funguje, ale Keycloak HTTPS je neefektivní |
| **Docker Dependencies** | 🔴 BROKEN | Nginx startuje před Grafanou |

---

## 🎯 Akční Plán (Priority)

### Fáze 1: Kritické Opravy (1-2 hodiny)

1. ✅ **Fix Nginx startup** - přidat explicit Grafana start do Makefile
2. ✅ **Fix JWKS URL** - změnit na `http://backend:8080/.well-known/jwks.json`
3. ✅ **Implementovat JWKS endpoint** v backendu

### Fáze 2: Důležité Optimalizace (2-4 hodiny)

4. 🔧 **Přepnout Keycloak na HTTP** uvnitř Docker sítě
5. 🔧 **Odstranit CORS fallback** z application.properties
6. 🔧 **Přidat SSL cert generování** do `make clean`

### Fáze 3: Vylepšení (1-2 dny)

7. 📝 **Dokumentovat Static IP** v docker-compose.yml
8. 🧪 **Přidat E2E testy** pro CORS a Grafana JWT
9. 📊 **Přidat Prometheus alerting** pro CORS errors

---

**Konec auditu** | Vygenerováno: 25. října 2025