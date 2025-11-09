# EPIC-007: Infrastructure & Deployment

**Status:** 🔵 **IN PROGRESS**  
**Priority:** P0 (CRITICAL - Foundation)  
**Effort:** ~40 hodin (core scope)  
**Owner:** DevOps + Platform Team

---

## 🎯 Cíl EPICU

**EPIC-007 je JEDINÝ zdroj pravdy pro lokální/prod-like prostředí core-platform.**

Řeší:
- ✅ Jak spustit celé prostředí lokálně
- ✅ Jak ho mít konzistentní, deterministické a blízké produkci
- ✅ Jak ho ověřit jednoduchým smoke testem
- ✅ Jak mít základní bezpečné zacházení s konfigurací a secrety

**Nic víc.** Vše ostatní (Vault, AI, E2E, feature moduly) patří do jiných EPICů.

---

## 🏛️ Design Decision: n8n jako Core Platform Component

**n8n is a mandatory core component of Virelio/Core Platform.**

### Rationale

1. **Primary Integration & Orchestration Engine:**
   - n8n slouží jako centrální hub pro všechny integrace s externími systémy (Jira, Confluence, Trello, M365, Google Workspace, Slack)
   - Eliminuje potřebu custom integračního kódu - 400+ built-in nodes pokrývají majority use-cases
   - AI workflow orchestration přes MCP/LLM gateway (EPIC-016)

2. **Security & Governance:**
   - **ALWAYS deployed behind Keycloak SSO** (admin realm only)
   - **ALWAYS protected by Nginx gateway** (TLS termination, rate limiting)
   - **ALWAYS observed via Loki/Prometheus** (audit logs, metrics)
   - **NO direct database access** - pouze přes Core Platform API/events

3. **Architectural Principles:**
   - n8n UI accessible only at `https://admin.${DOMAIN}/n8n`
   - Authentication: OIDC/SSO against admin realm (roles: `CORE_PLATFORM_ADMIN`, `INTEGRATION_ADMIN`)
   - All n8n → Core Platform interactions go through official REST API or event bus (Kafka)
   - n8n logs shipped to Loki with labels: `{service="n8n", tenant="admin"}`
   - n8n metrics exposed to Prometheus: `/metrics` endpoint

4. **Deployment Requirements:**
   - Docker/K8s deployment as core service (same tier as Backend, Keycloak, PostgreSQL)
   - Separate PostgreSQL database: `n8n` (isolated from `core`, `keycloak`)
   - Health checks: `/healthz` endpoint monitored by orchestrator
   - Volume persistence: workflow data, credentials (encrypted at rest)

### Non-Goals (Out of Scope)

- ❌ **No public n8n endpoints** outside Nginx/Keycloak protection
- ❌ **No per-tenant n8n instances** - single admin-realm instance for entire platform
- ❌ **No direct DB access from n8n** - API/events only

---

## 📋 Definition of Done

EPIC-007 je **HOTOVO**, pokud:

### 1. Lokální prostředí funguje bez manuální akce

```bash
make clean && make up
```

**Ověření:**
- ✅ Všechny kontejnery v `docker ps` jsou `healthy` nebo `running` (bez restart loopu)
- ✅ Žádná ruční akce není potřeba (import DB, seed data, SSL setup)
- ✅ První start po `git clone` je plně automatický

### 2. Domény a SSL fungují konzistentně

**Ověření:**
- ✅ `https://admin.core-platform.local` - Backend/FE přístupný
- ✅ `https://<tenant>.core-platform.local` - Tenant subdomény fungují
- ✅ Prohlížeč se připojí bez chyb konfigurace (self-signed cert je OK pro dev)
- ✅ SSL certifikáty jsou konzistentní (wildcard `*.core-platform.local`)

### 3. Autentizace funguje end-to-end

**Ověření:**
- ✅ Keycloak běží na `https://admin.core-platform.local/auth`
- ✅ FE + BE používají Keycloak jako IdP
- ✅ Základní login flow: přihlášení → JWT token → chráněné API volání
- ✅ Test user `test_admin` / `Test.1234` funguje out-of-the-box

### 4. Observabilita je dostupná

**Ověření:**
- ✅ Prometheus: `http://localhost:9090` - metriky z backendu a n8n dostupné
- ✅ Loki: logy z klíčových služeb (nginx, backend, keycloak, n8n) sbírány
- ✅ Grafana: minimálně 1 dashboard pro zdraví systému nebo popis jak ověřit metriky/logy

### 5. n8n Integration Hub je funkční

**Ověření:**
- ✅ n8n dostupné na `https://admin.core-platform.local/n8n`
- ✅ SSO přes Keycloak admin realm funguje (OIDC login flow)
- ✅ n8n UI se načte a zobrazí workflow editor
- ✅ PostgreSQL database `n8n` existuje a je funkční
- ✅ n8n logy viditelné v Loki s labelem `{service="n8n"}`
- ✅ n8n metriky scrapované Promethem (pokud dostupné)

### 6. Konfigurace a secrety jsou čisté

**Ověření:**
- ✅ **Žádné hardcoded hodnoty** v `application.properties` (DB URL, hesla)
- ✅ `.env` není v Gitu (v `.gitignore`)
- ✅ `.env.example` existuje s bezpečnými placeholdery
- ✅ Všechny důležité hodnoty (DB host, jména DB, hesla, doména, Keycloak klienti, n8n config) řízeny přes env proměnné

**Konfigurační hodnoty v `.env.example`:**
- `DOMAIN` - doména systému
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `KEYCLOAK_BASE_URL`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_ADMIN_CLIENT_SECRET`
- `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`
- `N8N_ENCRYPTION_KEY`, `N8N_USER_MANAGEMENT_JWT_SECRET`
- SSL cert paths
- Service URLs (Loki, Prometheus, Grafana, n8n)

### 7. Smoke test validuje prostředí

```bash
make smoke-test-env
# nebo
bash scripts/smoke-test-env.sh
```

**Ověření:**
- ✅ Skript existuje a je funkční
- ✅ Kontroluje:
  1. Backend health: `https://admin.core-platform.local/api/actuator/health` → 200
  2. Frontend load: `https://admin.core-platform.local` → 200 (HTML response)
  3. Keycloak: `https://admin.core-platform.local/auth/realms/admin/.well-known/openid-configuration` → 200
  4. n8n UI: `https://admin.core-platform.local/n8n` → 200 (requires auth)
  5. Loki: `http://loki:3100/ready` → 200 (internal)
  6. Prometheus: `http://prometheus:9090/-/ready` → 200 (internal)
  7. Realm initialized: Keycloak client `admin-client`, `n8n-client` existují
- ✅ Smoke test je součástí README s příklady

---

## 🏗️ Architektura

### Služby v prostředí

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND LAYER                                              │
├─────────────────────────────────────────────────────────────┤
│  Nginx (SSL Termination & Gateway)                          │
│    ├─→ https://admin.core-platform.local → Frontend SPA    │
│    ├─→ https://admin.core-platform.local/api → Backend     │
│    ├─→ https://admin.core-platform.local/auth → Keycloak   │
│    └─→ https://admin.core-platform.local/n8n → n8n UI      │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ APPLICATION LAYER                                           │
├─────────────────────────────────────────────────────────────┤
│  Backend (Spring Boot 3.2, Java 21)                         │
│    ├─→ REST API                                             │
│    ├─→ OAuth2 Resource Server (JWT validation)             │
│    └─→ Actuator endpoints (/health, /metrics)              │
│                                                             │
│  Frontend (React 18, TypeScript, Vite)                      │
│    ├─→ OAuth2 Client (Authorization Code Flow)             │
│    └─→ Static assets served by Nginx                       │
│                                                             │
│  n8n (Workflow Automation & Integration Hub)               │
│    ├─→ External integrations (Jira, M365, Slack)           │
│    ├─→ AI workflow orchestration (MCP/LLM)                 │
│    ├─→ ETL/batch processing                                │
│    └─→ 400+ built-in nodes (no custom code)                │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ AUTH & DATA LAYER                                           │
├─────────────────────────────────────────────────────────────┤
│  Keycloak (Auth Server)                                     │
│    ├─→ Realm: admin                                         │
│    ├─→ Client: admin-client, n8n-client                     │
│    └─→ Users: test_admin, test_user                         │
│                                                             │
│  PostgreSQL 16                                              │
│    ├─→ Database: core (main app)                            │
│    ├─→ Database: keycloak (auth data)                       │
│    ├─→ Database: grafana (dashboards)                       │
│    └─→ Database: n8n (workflow executions)                  │
└─────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────┐
│ OBSERVABILITY LAYER                                         │
├─────────────────────────────────────────────────────────────┤
│  Loki (Log Aggregation)                                     │
│    └─→ Collects from: nginx, backend, keycloak, n8n        │
│                                                             │
│  Prometheus (Metrics)                                       │
│    └─→ Scrapes: backend actuator, n8n metrics, node-exp    │
│                                                             │
│  Grafana (Dashboards)                                       │
│    └─→ Data sources: Loki, Prometheus                      │
└─────────────────────────────────────────────────────────────┘
```

### Network Topology

```
Browser
  ↓ HTTPS (443)
Nginx (SSL Termination & Reverse Proxy)
  ↓ HTTP (internal)
├─→ Backend (8080) ← JWT validation → Keycloak (8443)
│     ↓
│   PostgreSQL (5432)
│     ├─ core database
│     ├─ keycloak database
│     ├─ grafana database
│     └─ n8n database
│
├─→ n8n (5678) ← SSO auth → Keycloak (8443)
│     ├─→ Backend API (8080) - integrations only
│     └─→ External APIs (Jira, M365, Slack, etc.)
│
└─→ Observability Stack
    ├─ Loki (3100) ← logs from: nginx, backend, keycloak, n8n
    └─ Prometheus (9090) ← metrics from: backend, n8n
```

---

## 📖 Konfigurační standardy (Infrastructure)

### Principy

#### 1. Jedna pravda pro konfiguraci

- **Runtime hodnoty:** Z environment proměnných
- **Templates:** Generované z env při startu (Keycloak realm, Nginx conf)
- **Žádné duplicity:** DB URL jen jednou (v env), ne v properties i env

#### 2. `.env` management

- ✅ `.env` **MUSÍ být** v `.gitignore`
- ✅ `.env.example` slouží jako šablona **BEZ skutečných secretů**
- ✅ Názvy proměnných dokumentované v README (viz tabulka níže)

#### 3. Database konfigurace

- ❌ **Žádné** `spring.datasource.url` napevno v `application.properties`
- ✅ Používat `${DATABASE_URL}` v `application.yml`

**Špatně:**
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://db:5432/core  # ❌ HARDCODED!
```

**Správně:**
```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://core-db:5432/core}  # ✅ ENV VAR
    username: ${DATABASE_USERNAME:core}
    password: ${DATABASE_PASSWORD}
```

#### 4. Secrets

- ❌ **Nikdy necommituj** skutečné heslo, `client_secret`, private key
- ✅ Pro lokál klidně jednoduché hodnoty (`Test.1234`), ale **JEN v `.env`**, ne ve zdrojáku
- ✅ Production: use Vault nebo Docker Secrets (viz EPIC-012)

#### 5. Nginx konfigurace

- ✅ Používá env template (`envsubst`)
- ✅ Domény: vše odvozené z `${DOMAIN}`

**Template:**
```nginx
server {
    server_name admin.${DOMAIN};
    ssl_certificate /etc/nginx/ssl/server.crt.pem;
}
```

#### 6. Rozdíl lokál vs budoucí prod

- ✅ **Stejný naming** a struktura env proměnných
- ✅ Přechod na Kubernetes je mechanický (stejné env vars, jiný orchestrátor)
- ✅ EPIC-007 řeší **"prod-like local infra"**, ne plný production K8s stack

---

## 📊 Environment Variables Reference

### Kompletní seznam proměnných

| Variable | Purpose | Example | Required | Security |
|----------|---------|---------|----------|----------|
| **Domain & SSL** |
| `DOMAIN` | Base domain | `core-platform.local` | ✅ | 🟢 Public |
| `SSL_CERT_PATH` | SSL certificate | `./docker/ssl/server.crt.pem` | ✅ | 🟢 Public |
| `SSL_KEY_PATH` | SSL private key | `./docker/ssl/server.key.pem` | ✅ | 🔴 SECRET |
| **Database** |
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://core-db:5432/core` | ✅ | 🟡 Internal |
| `DATABASE_USERNAME` | DB user | `core` | ✅ | 🔴 SECRET |
| `DATABASE_PASSWORD` | DB password | `core` | ✅ | 🔴 SECRET |
| `POSTGRES_USER` | PostgreSQL admin | `core` | ✅ | 🔴 SECRET |
| `POSTGRES_PASSWORD` | PostgreSQL admin pass | `core` | ✅ | 🔴 SECRET |
| `POSTGRES_DB` | Default database | `core` | ✅ | 🟢 Public |
| **Keycloak Auth** |
| `KEYCLOAK_BASE_URL` | Keycloak URL | `https://admin.core-platform.local` | ✅ | 🟢 Public |
| `KEYCLOAK_ADMIN` | Admin username | `admin` | ✅ | 🔴 SECRET |
| `KEYCLOAK_ADMIN_PASSWORD` | Admin password | `admin` | ✅ | 🔴 SECRET |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | OAuth2 client secret | `<uuid>` | ✅ | 🔴 SECRET |
| `OIDC_CLIENT_ID` | OAuth2 client ID | `admin-client` | ✅ | 🟢 Public |
| `OIDC_CLIENT_SECRET` | OAuth2 client secret | `${KEYCLOAK_ADMIN_CLIENT_SECRET}` | ✅ | 🔴 SECRET |
| `OIDC_ISSUER_URI` | OIDC issuer | `https://admin.core-platform.local/realms/admin` | ✅ | 🟢 Public |
| **Redis** |
| `REDIS_HOST` | Redis hostname | `redis` | ✅ | 🟢 Public |
| `REDIS_PORT` | Redis port | `6379` | ✅ | 🟢 Public |
| `REDIS_PASSWORD` | Redis password | `` (empty for dev) | ⚠️ | 🟡 None |
| **n8n Integration Hub** |
| `N8N_HOST` | n8n hostname | `n8n` | ✅ | 🟢 Public |
| `N8N_PORT` | n8n port | `5678` | ✅ | 🟢 Public |
| `N8N_PROTOCOL` | n8n protocol | `https` | ✅ | 🟢 Public |
| `N8N_EDITOR_BASE_URL` | n8n base URL | `https://admin.core-platform.local/n8n` | ✅ | 🟢 Public |
| `N8N_WEBHOOK_URL` | n8n webhook URL | `https://admin.core-platform.local/webhook` | ✅ | 🟢 Public |
| `N8N_ENCRYPTION_KEY` | n8n data encryption | `<random-32-char-string>` | ✅ | 🔴 SECRET |
| `N8N_USER_MANAGEMENT_JWT_SECRET` | n8n JWT secret | `<random-string>` | ✅ | 🔴 SECRET |
| `N8N_DB_TYPE` | n8n database type | `postgresdb` | ✅ | 🟢 Public |
| `N8N_DB_HOST` | n8n DB host | `core-db` | ✅ | 🟢 Public |
| `N8N_DB_PORT` | n8n DB port | `5432` | ✅ | 🟢 Public |
| `N8N_DB_NAME` | n8n database name | `n8n` | ✅ | 🟢 Public |
| `N8N_DB_USER` | n8n DB user | `n8n_app` | ✅ | 🔴 SECRET |
| `N8N_DB_PASSWORD` | n8n DB password | `<strong-password>` | ✅ | 🔴 SECRET |
| **Observability** |
| `LOKI_URL` | Loki endpoint | `http://loki:3100` | ✅ | 🟢 Public |
| `PROMETHEUS_URL` | Prometheus endpoint | `http://prometheus:9090` | ✅ | 🟢 Public |
| `GRAFANA_ADMIN_USER` | Grafana admin | `admin` | ✅ | 🔴 SECRET |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password | `admin` | ✅ | 🔴 SECRET |

**Poznámka:** Viz kompletní audit v [SECURITY_CONFIG_AUDIT.md](../../SECURITY_CONFIG_AUDIT.md)

---

## 🧪 Smoke Test Specification

### Manuální checklist (před automatizací)

Smoke test ověří:

1. **Backend Health**
   ```bash
   curl -k https://admin.core-platform.local/api/actuator/health
   # Expected: {"status":"UP"}
   ```

2. **Backend Actuator (internal)**
   ```bash
   docker exec core-backend curl -s http://localhost:8080/actuator/health
   # Expected: {"status":"UP"}
   ```

3. **Frontend Load**
   ```bash
   curl -k https://admin.core-platform.local
   # Expected: HTTP 200, HTML response s React app
   ```

4. **Keycloak OIDC Config**
   ```bash
   curl -k https://admin.core-platform.local/auth/realms/admin/.well-known/openid-configuration
   # Expected: JSON s issuer, authorization_endpoint, token_endpoint
   ```

5. **Loki Ready**
   ```bash
   docker exec core-loki curl -s http://localhost:3100/ready
   # Expected: "ready"
   ```

6. **Prometheus Ready**
   ```bash
   curl -s http://localhost:9090/-/ready
   # Expected: "Prometheus is Ready."
   ```

7. **Keycloak Realm Initialized**
   - Přihlásit se do Keycloak admin console: `https://admin.core-platform.local/auth/admin`
   - Username: `admin`, Password: `admin`
   - Ověřit existenci realm `admin` a client `admin-client`

8. **Frontend Authentication Flow**
   - Otevřít `https://admin.core-platform.local`
   - Přesměrování na Keycloak login
   - Login jako `test_admin` / `Test.1234`
   - Přesměrování zpět do aplikace s platným session

### Automatizace (implementace)

**Cíl:** Kdokoliv nový v týmu nebo CI runner spustí 2 příkazy:

```bash
make up
make smoke-test-env
```

a hned ví, jestli infra stojí nebo ne.

**Implementace smoke testu:**

```bash
#!/bin/bash
# scripts/smoke-test-env.sh

set -e

DOMAIN="${DOMAIN:-core-platform.local}"
TIMEOUT=10

echo "🔍 Core Platform Environment Smoke Test"
echo "========================================"

# 1. Backend Health (via Nginx)
echo -n "Backend Health (Nginx)... "
if curl -f -k -s -m $TIMEOUT "https://admin.${DOMAIN}/api/actuator/health" | grep -q '"status":"UP"'; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# 2. Frontend
echo -n "Frontend... "
if curl -f -k -s -m $TIMEOUT "https://admin.${DOMAIN}/" > /dev/null; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# 3. Keycloak OIDC
echo -n "Keycloak OIDC... "
if curl -f -k -s -m $TIMEOUT "https://admin.${DOMAIN}/auth/realms/admin/.well-known/openid-configuration" | grep -q '"issuer"'; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# 4. Loki (internal)
echo -n "Loki... "
if docker exec core-loki curl -f -s -m $TIMEOUT "http://localhost:3100/ready" | grep -q "ready"; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

# 5. Prometheus (external port)
echo -n "Prometheus... "
if curl -f -s -m $TIMEOUT "http://localhost:9090/-/ready" | grep -q "Prometheus is Ready"; then
  echo "✅ OK"
else
  echo "❌ FAIL"
  exit 1
fi

echo "========================================"
echo "✅ All smoke tests passed"
```

**Makefile integration:**

```makefile
.PHONY: smoke-test-env
smoke-test-env: ## Run environment smoke tests
	@echo "🧪 Running smoke tests..."
	@bash scripts/smoke-test-env.sh
```

---

## ⛔ Mimo scope EPIC-007

Následující **NEPATŘÍ** do EPIC-007 a budou řešeny jinými EPICy:

### Vault integrace → EPIC-012
- HashiCorp Vault deployment
- Secret rotation API
- Vault PKI pro SSL certs
- Audit logging pro secret access

### E2E test framework → EPIC-002
- Playwright setup
- Page Object Model
- Test coverage dashboard
- Visual regression testing
- Performance testing

### Feature moduly → vlastní EPICy
- Metamodel Studio → EPIC-004
- Workflow Designer → EPIC-005
- DMS (Document Management) → EPIC-006
- AI/MCP integrace → EPIC-016

### Pokročilé security pro produkci
- HSM integrace
- mTLS mezi službami
- Vault PKI
- Advanced WAF rules
- SIEM integrace

**EPIC-007 je o tom, aby:**
- ✅ Lokál/prod-like prostředí šlo spustit bez magie
- ✅ Konfigurace byla čistá
- ✅ Základní bezpečnost a pořádek v env/secretech byl nastaven
- ✅ Bylo možné jednoduše ověřit, že vše běží

---

## 📅 Implementační plán EPIC-007

### Fáze 1 – Cleanup & sjednocení (Week 1, ~8h)

**Úkoly:**
1. Odstranit hardcoded DB URL z `application.properties`
   - Přesunout do `application.yml` s `${DATABASE_URL}`
   - Ověřit že env vars fungují

2. Přidat `.env` do `.gitignore`
   - Vytvořit `.env.example` s placeholdery
   - Dokumentovat všechny proměnné

3. Sloučit/smazat duplicitní dokumenty
   - `EPIC-007-COMPLETE.md` → označit jako deprecated
   - `REFACTORING_TASKS.md` → zakomponovat užitečné části do README
   - Ponechat pouze tento README jako zdroj pravdy

**Deliverables:**
- ✅ Čistý `.gitignore` (`.env` ignorován)
- ✅ `.env.example` s dokumentovanými proměnnými
- ✅ `application.yml` používá env vars (ne hardcoded)
- ✅ README je jediný dokument v EPIC-007

### Fáze 2 – Stabilní lokální prostředí (Week 2, ~12h)

**Úkoly:**
1. Zkontrolovat `docker-compose.yml` konzistenci
   - Všechny služby mají health checks
   - Závislosti správně definované (depends_on)
   - Žádné restart loopy

2. Ověřit Nginx + SSL setup
   - Wildcard cert pro `*.core-platform.local`
   - Všechny domény routují správně
   - SSL termination funguje

3. Ověřit Keycloak + realm import
   - `realm-admin.json` generován z template
   - Client `admin-client` vytvořen automaticky
   - Test users seednutí

4. Ověřit Loki + Prometheus
   - Logy sbírány z klíčových služeb
   - Metriky scrapovány z backendu
   - Základní dashboard v Grafaně

**Deliverables:**
- ✅ `make clean && make up` funguje konzistentně
- ✅ Všechny služby healthy po startu
- ✅ Žádná manuální akce nutná

### Fáze 3 – Smoke test (Week 3, ~8h)

**Úkoly:**
1. Implementovat `scripts/smoke-test-env.sh`
   - 6 endpoint checks (backend, FE, Keycloak, Loki, Prometheus)
   - Timeout handling
   - Clear error messages

2. Přidat Makefile target `make smoke-test-env`

3. Otestovat na čistém prostředí
   - Fresh clone repo
   - První `make up`
   - Spustit smoke test

**Deliverables:**
- ✅ Funkční smoke test script
- ✅ Makefile integrace
- ✅ Dokumentace v README

### Fáze 3.5 – n8n Integration Hub Deployment (Week 3, ~10h)

**Úkoly:**
1. **Docker Compose Integration**
   - Přidat n8n service do `docker-compose.yml`
   - Konfigurace PostgreSQL database `n8n`
   - Separate DB user: `n8n_app` (isolace od `core`)
   - Volume persistence: `n8n_data:/root/.n8n`
   - Health check: `/healthz` endpoint

2. **Keycloak OIDC Configuration**
   - Vytvořit Keycloak client: `n8n-client` v admin realm
   - OIDC redirect URIs: `https://admin.${DOMAIN}/n8n/*`
   - Role mapping: `CORE_PLATFORM_ADMIN`, `INTEGRATION_ADMIN`
   - Service account enabled (pro API calls)

3. **Nginx Reverse Proxy**
   - Route `/n8n/*` → `http://n8n:5678/`
   - SSL termination (TLS 1.2+)
   - CSP headers: `frame-ancestors 'self'`
   - Rate limiting: 50 req/s per IP
   - Auth validation: Keycloak SSO required

4. **Observability Integration**
   - Loki: ship n8n logs s labelem `{service="n8n"}`
   - Prometheus: scrape n8n metrics (pokud dostupné)
   - Health checks: `make verify` kontroluje n8n endpoint

5. **Environment Variables**
   - Přidat do `.env.template`:
     * `N8N_ENCRYPTION_KEY` (32-char random)
     * `N8N_USER_MANAGEMENT_JWT_SECRET`
     * `N8N_EDITOR_BASE_URL`, `N8N_WEBHOOK_URL`
     * `N8N_DB_*` credentials

**Deliverables:**
- ✅ n8n běží na `https://admin.${DOMAIN}/n8n`
- ✅ SSO přes Keycloak admin realm funguje
- ✅ PostgreSQL database `n8n` vytvořena
- ✅ Logy v Loki, metriky v Prometheus (pokud dostupné)
- ✅ Smoke test kontroluje n8n health endpoint
- ✅ Dokumentace v README (n8n env vars, access, security)

### Fáze 4 – Dokumentace (Week 4, ~12h)

**Úkoly:**
1. Aktualizovat README
   - Quick start guide
   - Environment variables reference
   - Troubleshooting sekce
   - Architecture diagram

2. Vytvořit `.env.example`
   - Všechny proměnné s komentáři
   - Bezpečné placeholdery

3. Vytvořit onboarding checklist
   - Pro nového vývojáře
   - Pro CI/CD setup

**Deliverables:**
- ✅ README kompletní a přesný
- ✅ `.env.example` použitelný out-of-the-box
- ✅ Developer může nastartovat prostředí do 10 minut

---

## 🚀 Quick Start Guide

### Prerequisites

- Docker Desktop 4.x+
- Make
- `/etc/hosts` entry: `127.0.0.1 admin.core-platform.local`

### First Time Setup

```bash
# 1. Clone repository
git clone https://github.com/Muriel2Horak/core-platform.git
cd core-platform

# 2. Create .env from example
cp .env.example .env
# (Edit .env if needed, defaults should work)

# 3. Generate SSL certificates (first time only)
bash docker/ssl/generate-ssl.sh

# 4. Start environment
make clean && make up

# 5. Wait for services to be ready (~2-3 min)
make wait-for-services

# 6. Verify environment
make smoke-test-env

# 7. Open app
open https://admin.core-platform.local
# Login: test_admin / Test.1234
```

### Daily Development

```bash
# Start environment
make up

# Stop environment
make down

# Rebuild after code changes
make clean-fast

# View logs
make logs-backend
make logs-frontend
make logs-errors

# Smoke test
make smoke-test-env
```

---

## 🔍 Troubleshooting

### Problem: SSL certificate errors

**Symptom:** Browser shows "Your connection is not private"

**Solution:**
```bash
# Regenerate SSL certs
bash docker/ssl/generate-ssl.sh

# Restart nginx
make restart-nginx
```

### Problem: Keycloak login fails

**Symptom:** "Invalid credentials" or redirect loop

**Solution:**
```bash
# Check Keycloak logs
make logs-keycloak

# Verify realm imported
docker exec core-keycloak /opt/keycloak/bin/kcadm.sh get realms/admin

# Recreate realm (nuclear option)
make rebuild-keycloak
```

### Problem: Backend can't connect to database

**Symptom:** `Connection refused` in backend logs

**Solution:**
```bash
# Check DB is running
docker ps | grep postgres

# Check DATABASE_URL in .env
cat .env | grep DATABASE_URL

# Verify no hardcoded URL in application.properties
grep "spring.datasource.url" backend/src/main/resources/application.properties
# Should return nothing!

# Check DB health
docker exec core-db pg_isready -U core
```

### Problem: `make smoke-test-env` fails

**Symptom:** Specific check fails (e.g., "Loki... ❌ FAIL")

**Solution:**
```bash
# Check service health
docker ps

# Check specific service logs
docker logs core-loki

# Restart failed service
docker restart core-loki

# Re-run smoke test
make smoke-test-env
```

---

## � Security Alignment (EPIC-000)

**Tento EPIC dodržuje následující pravidla z [EPIC-000](../EPIC-000-security-platform-hardening/README.md):**

### Identity & Access Management
- ✅ **Keycloak deployment**: Jediný IdP, realm config, SSL setup
- ✅ **Service accounts**: n8n, backend services mají vlastní Keycloak identities
- ✅ **JWT validation**: Backend ověřuje tokeny z Keycloak issuer

### Secrets Management
- ✅ **No hardcoded secrets**: Žádné DB URLs, hesla, API klíče v `application.properties`
- ✅ **`.env` gitignored**: Plain-text secrets nejsou v Gitu
- ✅ **`.env.example` template**: Bezpečné placeholdery pro onboarding
- ✅ **Environment variables**: Všechny secrets načítány z env (připraveno pro Vault migration)

### API & Network Security
- ✅ **Nginx SSL termination**: Wildcard cert `*.core-platform.local`
- ✅ **Internal network isolation**: PostgreSQL, Redis, Kafka nejsou exposed ven
- ✅ **HTTPS everywhere**: Public endpoints pouze HTTPS

### Logging & Audit
- ✅ **Structured logs**: JSON format do Loki
- ✅ **Loki centralization**: Všechny logy (nginx, backend, keycloak) sbírány centrálně

### Build & Supply Chain
- ✅ **Docker image hardening**: Multi-stage builds, non-root users
- ✅ **SSL cert generation**: Automated `generate-ssl.sh` script
- ✅ **Config templates**: `envsubst` based generation (traceable, repeatable)

### Testing
- ✅ **Smoke test**: `make smoke-test-env` validuje security basics (HTTPS, auth endpoints)
- ✅ **DoD includes**: Žádné hardcoded secrets v diff, .env v .gitignore

**Security Improvements Planned:**
- 🔵 Migration to Vault (EPIC-012): Replace `.env` secrets with Vault dynamic secrets
- 🔵 Let's Encrypt integration: Auto SSL cert rotation (production)
- 🔵 Rate limiting: Nginx rate limit rules (API protection)
- 🔵 WAF rules: Basic SQL injection, XSS protection

---

## �📚 References

- **Security Master:** [EPIC-000](../EPIC-000-security-platform-hardening/README.md) - Security & Access Control Platform Hardening
- **Security Audit:** [SECURITY_CONFIG_AUDIT.md](../../SECURITY_CONFIG_AUDIT.md) - Kompletní audit 47 env vars a 12 secrets
- **DB Users Plan:** [DB_SEPARATE_USERS_PLAN.md](../../DB_SEPARATE_USERS_PLAN.md) - Migrace na separate DB users
- **Makefile:** [Makefile](../../Makefile) - Build orchestration
- **Docker Compose:** [docker/docker-compose.yml](../../docker/docker-compose.yml) - Service definitions

---

**Total Effort:** ~40 hodin (4 fáze)  
**Priority:** P0 (CRITICAL - Foundation for all development)  
**Value:** Stabilní, deterministické, prod-like lokální prostředí s čistou konfigurací

**Last Updated:** 9. listopadu 2025
