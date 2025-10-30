# Environment & Configuration Analysis - Současný Stav

**Datum:** 27. října 2025  
**Účel:** Kompletní audit všech environment variables, konfigurací a jejich použití

---

## 🎯 PŘEHLED SOUČASNÉHO STAVU

### Buildovací Módy
```
┌─────────────────────────────────────────────────────────────┐
│ PRODUCTION BUILD (Jediný funkční mód)                       │
├─────────────────────────────────────────────────────────────┤
│ Příkazy:  make up / make rebuild / make clean / clean-fast │
│ Docker:   17+ kontejnerů (full stack)                      │
│ SSL:      ✅ HTTPS end-to-end                               │
│ Startup:  ~5-10 min (full), ~2-3 min (rebuild)             │
│ Hot reload: ❌ Není (rebuild nutný)                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ DEV CONTAINER (.devcontainer overlay) - NEFUNKČNÍ           │
├─────────────────────────────────────────────────────────────┤
│ Status:   ⚠️ OZNAČENO jako broken v copilot-instructions.md │
│ Problém:  Hot reload nefunguje spolehlivě                   │
│ Použití:  ❌ NEDOPORUČENO                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 ANALÝZA KONFIGURAČNÍCH SOUBORŮ

### 1. Environment Variables - `.env` (Root)

**Soubor:** `.env` (generovaný z `.env.template`)

```bash
# Kritické proměnné:
DATABASE_URL=jdbc:postgresql://core-db:5432/core
POSTGRES_DB=core
POSTGRES_USER=core
POSTGRES_PASSWORD=core

# Keycloak
KEYCLOAK_BASE_URL=https://admin.core-platform.local
KEYCLOAK_ADMIN_CLIENT_SECRET=<tajná hodnota>
OIDC_ISSUER_URI=https://admin.core-platform.local/realms/admin

# Domain
DOMAIN=core-platform.local

# SSL
SSL_CERT_PATH=./docker/ssl/server.crt.pem
SSL_KEY_PATH=./docker/ssl/server.key.pem
```

**Použití:**
- ✅ Docker Compose environment variables
- ✅ Nginx template substituce (`envsubst`)
- ✅ Keycloak realm template (`generate-realm.sh`)
- ✅ Backend Spring Boot (přes Docker env)

**Problémy:**
- ⚠️ `.env` je **generovaný**, ne commitnutý
- ⚠️ Workflow vyžaduje `make env-generate` před buildem
- ⚠️ Uživatelé často zapomínají regenerovat po změně `.env.template`

---

### 2. Spring Boot Configuration - Backend

#### `application.yml` (Production)
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:default}
  
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OIDC_ISSUER_URI}
```

**Klíčové hodnoty:**
- `DATABASE_URL` - Z `.env` přes Docker environment
- `OIDC_ISSUER_URI` - Z `.env` (HTTPS URL!)
- JWT validation vyžaduje **HTTPS issuer**

#### `application-reporting.yml` (Reporting Profile)
```yaml
spring:
  data:
    redis:
      timeout: 2000  # INTEGER milliseconds (OPRAVENO)
```

**Poznámka:** Původně bylo `2000ms` (string) → způsobovalo NumberFormatException

---

### 3. Docker Compose Stack

#### `docker-compose.yml` (Generated from Template)
```yaml
services:
  backend:
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - OIDC_ISSUER_URI=${OIDC_ISSUER_URI}
      - KEYCLOAK_ADMIN_CLIENT_SECRET=${KEYCLOAK_ADMIN_CLIENT_SECRET}
      # ... 30+ dalších env vars
```

**Environment sources:**
1. `.env` soubor (--env-file .env)
2. `docker-compose.template.yml` environment sekce
3. Keycloak realm-admin.json (generovaný)

#### `docker-compose.template.yml`
```yaml
# Template použitý pro generování docker-compose.yml
# envsubst nahradí ${VARIABLES} hodnotami z .env
```

---

### 4. Keycloak Configuration

#### Realm Generation Workflow
```
1. docker/keycloak/realm-admin.template.json (source)
   ├─ Obsahuje: ${DOMAIN}, ${KEYCLOAK_ADMIN_CLIENT_SECRET}
   
2. generate-realm.sh (automaticky při 'make kc-image')
   ├─ envsubst < realm-admin.template.json > realm-admin.json
   
3. realm-admin.json (generated)
   ├─ Import při Keycloak startu (--import-realm)
```

**Příklad substituce:**
```json
// Template:
{
  "redirectUris": ["https://${DOMAIN}/*"],
  "secret": "${KEYCLOAK_ADMIN_CLIENT_SECRET}"
}

// Vygenerováno:
{
  "redirectUris": ["https://core-platform.local/*"],
  "secret": "actual-secret-value"
}
```

#### Keycloak Docker Build
```dockerfile
FROM quay.io/keycloak/keycloak:24.0.4

# Zkopíruj VYGENEROVANÝ realm config
COPY docker/keycloak/realm-admin.json /opt/keycloak/data/import/

# Zkopíruj SSL certifikáty
COPY docker/ssl/*.pem /opt/keycloak/conf/

# Zkopíruj custom theme
COPY docker/keycloak/themes/core-material /opt/keycloak/themes/
```

**Startup command:**
```bash
start --optimized \
  --import-realm \
  --spi-import-if-exists=skip \
  --proxy=edge \
  --https-port=8443
```

**Klíčové body:**
- ✅ Keycloak běží v **production mode** (ne start-dev)
- ✅ HTTPS na portu 8443
- ✅ SSL certifikáty z `docker/ssl/`
- ✅ Realm import z `/opt/keycloak/data/import/`

---

### 5. Nginx Reverse Proxy

#### `nginx-ssl.conf.template`
```nginx
upstream backend {
    server backend:8080;
}

upstream keycloak {
    server keycloak:8080;  # Keycloak internal HTTP, external HTTPS
}

server {
    listen 443 ssl http2;
    server_name *.${DOMAIN} ${DOMAIN};
    
    ssl_certificate /etc/nginx/ssl/server.crt.pem;
    ssl_certificate_key /etc/nginx/ssl/server.key.pem;
    
    # Backend API
    location /api {
        proxy_pass http://backend;
    }
    
    # Keycloak
    location /realms {
        proxy_pass http://keycloak;
    }
    
    # Frontend (static files)
    location / {
        root /usr/share/nginx/html;
    }
}
```

**Generování:**
```bash
# V Makefile před 'docker compose up':
envsubst '${DOMAIN}' < nginx-ssl.conf.template > nginx-ssl.conf
```

---

## 🔍 DEPENDENCIES MEZI KOMPONENTAMI

### Komunikační Flow (Production)

```
Browser (HTTPS)
    ↓
Nginx (HTTPS :443)
    ├─→ /api/* → Backend (HTTP :8080 internal)
    │             ↓
    │             PostgreSQL (:5432)
    │             ↓
    │             Keycloak (HTTPS :8443 via Nginx)
    │                 ↓
    │                 JWT token validation
    │                 issuer: https://admin.core-platform.local
    │
    ├─→ /realms/* → Keycloak (HTTP :8080 internal → HTTPS external)
    │
    └─→ /* → Frontend (static files)
```

**SSL/HTTPS Závislosti:**
1. **Nginx** - SSL termination (443 → HTTP internal)
2. **Keycloak** - Vrací HTTPS issuer URL v JWT tokens
3. **Backend** - Validuje JWT tokens s HTTPS issuer
4. **Frontend** - Komunikuje přes HTTPS

**Kritické:**
- ❗ Backend **MUSÍ** vidět Keycloak přes HTTPS URL (issuer validation)
- ❗ JWT tokens obsahují `iss: "https://admin.core-platform.local/realms/admin"`
- ❗ Spring Security validuje issuer - pokud nesedí, token je rejected

---

## 📊 ENVIRONMENT VARIABLES MATICE

### Kde Se Co Používá

| Variable | .env | application.yml | docker-compose.yml | Keycloak realm | Nginx conf |
|----------|------|-----------------|-------------------|----------------|------------|
| DATABASE_URL | ✅ | ✅ | ✅ | ❌ | ❌ |
| POSTGRES_DB | ✅ | ❌ | ✅ | ❌ | ❌ |
| KEYCLOAK_BASE_URL | ✅ | ✅ | ✅ | ✅ | ❌ |
| OIDC_ISSUER_URI | ✅ | ✅ | ✅ | ❌ | ❌ |
| DOMAIN | ✅ | ❌ | ✅ | ✅ | ✅ |
| KEYCLOAK_ADMIN_CLIENT_SECRET | ✅ | ✅ | ✅ | ✅ | ❌ |
| SSL_CERT_PATH | ✅ | ❌ | ✅ | ❌ | ✅ |

### Duplicity a Konflikty

**Problém 1: DATABASE_URL vs POSTGRES_DB**
```bash
# .env má:
DATABASE_URL=jdbc:postgresql://core-db:5432/core_platform  # ❌ ŠPATNĚ!
POSTGRES_DB=core  # ✅ SPRÁVNĚ

# Init skripty vytváří databázi 'core', ale URL očekává 'core_platform'
# → Backend nemůže nastartovat!
```

**Řešení:**
```bash
DATABASE_URL=jdbc:postgresql://core-db:5432/core  # ✅ OPRAVENO
```

**Problém 2: Redis Timeout Type Mismatch**
```yaml
# application-reporting.yml PŮVODNĚ:
spring:
  data:
    redis:
      timeout: 2000ms  # ❌ String → NumberFormatException

# OPRAVENO:
      timeout: 2000  # ✅ Integer milliseconds
```

---

## 🚨 SOUČASNÉ PROBLÉMY

### 1. Template System Overhead
```
Workflow:
1. Edituj .env.template
2. Zavolej 'make env-generate'  ← ČASTO ZAPOMENUTO!
3. Edituj docker-compose.template.yml
4. Zavolej 'make compose-generate'  ← TAKÉ ZAPOMENUTO!
5. Build docker images
```

**Trade-off:**
- ✅ Flexibilita (různá prostředí)
- ❌ Složitost (extra kroky)
- ❌ Chyby (zapomenutý generate)

### 2. Keycloak Issuer URL Lock-in

**Problém:**
Keycloak vrací HTTPS issuer i když je volaný přes HTTP:
```bash
curl http://localhost:8081/realms/admin/.well-known/openid-configuration
# → {"issuer": "https://admin.core-platform.local/realms/admin"}
```

**Důvod:**
- Realm config má `"sslRequired": "external"`
- Keycloak detekuje hostname z HTTP headers
- Database už má realm config s HTTPS URLs

**Důsledek:**
- Spring Boot nemůže volat Keycloak přes HTTP
- JWT tokens musí být validovány proti HTTPS issuer

### 3. Dev Container Nefunguje

**Současný stav:**
- `.devcontainer/docker-compose.devcontainer.yml` existuje
- `Dockerfile.dev` pro backend/frontend s volumes
- Hot reload **měl** fungovat, ale **nefunguje**

**Proč:**
- Spring Boot DevTools v Dockeru = unreliable
- Vite watch mode v Dockeru = pomalý
- Network overhead mezi host a container

---

## 💡 CO FUNGUJE DOBŘE

### 1. Production Build Flow ✅
```bash
make rebuild     # S cache - 2-3 min (RYCHLÉ!)
make clean-fast  # Bez E2E - 5-10 min
make clean       # S E2E - 30-40 min (CI/CD)
```

### 2. SSL/HTTPS Setup ✅
- Wildcard certifikáty (`*.core-platform.local`)
- Automatický import do Java truststore
- Nginx SSL termination
- Keycloak HTTPS mode

### 3. Build Doctor ✅
- Diagnostika při buildu
- JSON reporty
- Crashloop detection
- Health checks

### 4. Loki Centralized Logging ✅
```bash
make logs              # Všechny logy
make logs-backend      # Backend (strukturované)
make logs-errors       # ERROR only
```

---

## 🎯 DOPORUČENÍ

### Možnost A: Zůstat U Production Mode
**Strategie:** Použít `make rebuild` místo `make clean-fast`

**Výhody:**
- ✅ Všechno už funguje
- ✅ Rebuild s cache = 2-3 min (akceptovatelné?)
- ✅ Žádná nová complexity

**Nevýhody:**
- ❌ Stále 2-3 min wait po změně kódu
- ❌ Není instant hot reload

### Možnost B: Opravit Dev Container
**Strategie:** Fix `.devcontainer` overlay aby hot reload fungoval

**Výhody:**
- ✅ Infrastructure už existuje
- ✅ Stejný SSL/HTTPS setup

**Nevýhody:**
- ❌ Spring Boot DevTools v Dockeru = unreliable
- ❌ Vite watch mode = pomalý přes volumes

### Možnost C: Hybridní Přístup (NEREALIZOVAT)
**Strategie:** Backend/Frontend nativně, infra v Dockeru

**Výhody:**
- ✅ Rychlý hot reload
- ✅ Native debugging

**Nevýhody:**
- ❌ SSL complexity (právě jsi zastavil)
- ❌ Keycloak issuer problems
- ❌ Divergence od produkce

---

## 📋 AKTUÁLNÍ STAV PROJEKTU

### Co Máš K Dispozici
```bash
# Build commands
make up                 # Production build + start
make rebuild            # Rebuild s cache (2-3 min)
make rebuild-backend    # Backend only
make rebuild-frontend   # Frontend only
make clean-fast         # Clean bez E2E (5-10 min)

# Logs
make logs-backend       # Backend logy (Loki)
make logs-errors        # ERROR logy

# Testing
make test-backend       # Unit tests (2-5 min)
make test-e2e-pre       # Smoke tests (5-7 min)
```

### Co Funguje
- ✅ Production build (2-3 min s cache)
- ✅ SSL/HTTPS end-to-end
- ✅ Keycloak auth flow
- ✅ Multi-tenant subdomain routing
- ✅ Centralized logging (Loki)
- ✅ Build Doctor diagnostics

### Co Nefunguje
- ❌ Dev Container hot reload (broken)
- ❌ Instant reload při změně kódu
- ⚠️ Database URL bug (core_platform → core) - OPRAVENO

---

## 🔧 NEJBLIŽŠÍ KROKY (NA TVÉM ROZHODNUTÍ)

### Scénář 1: Stačí Ti Production Mode?
```bash
# Používej:
make rebuild  # 2-3 min místo 5-10 min
```

**Ano?** → Hotovo, nic měnit.  
**Ne?** → Pokračuj na Scénář 2.

### Scénář 2: Chceš Opravit Dev Container?
```bash
# Cíl: .devcontainer overlay s funkčním hot reload
```

**Ano?** → Analyzuji `.devcontainer` setup.  
**Ne?** → Pokračuj na Scénář 3.

### Scénář 3: Něco Úplně Jiného?
**Popis co potřebuješ:**
- Jak rychlý má být startup?
- Musí být hot reload?
- Musí být SSL/HTTPS?
- Kolik času můžeš čekat na změnu kódu?

---

**Co chceš dělat?**
