# core-platform

Univerzální vývojová platforma pro tvorbu enterprise aplikací, založená na modulárním **CORE** frameworku. Odděluje doménově nezávislou funkcionalitu od konkrétních aplikací – umožňuje rychlé vytváření nadstavbových řešení pomocí sdíleného jádra.

## 🚨 **BEZPEČNOSTNÍ PROHLÁŠENÍ**

**⚠️ KRITICKÉ: Tento projekt NENÍ o kompromisech. Tvoříme bezpečný a spolehlivý produkt pro produkční nasazení.**

### 🔴 **ZNÁMÉ SECURITY ISSUES (MUSÍ BÝT VYŘEŠENY PŘED PRODUKCI):**

1. **KeycloakClient.getAdminToken()** - Nezabezpečený admin přístup
   - 🚨 Používá `admin-cli` bez client secret
   - 🚨 Umožňuje neomezenou admin eskalaci
   - 🚨 **IMMEDIATE FIX REQUIRED**

2. **Hardkódované credentials** v konfiguracích
3. **Chybějící rate limiting** pro auth endpointy  
4. **Token caching** bez secure storage

### ✅ **BEZPEČNÁ ARCHITEKTURA (IMPLEMENTOVAT):**

```
┌─────────────────┐    Service      ┌─────────────────┐    Admin API    ┌──────────────┐
│   Backend App   │ ────────────→   │ Keycloak Admin  │ ──────────────→ │   Keycloak   │
│                 │   Account       │   Service       │   with proper   │    Server    │
│                 │   Token         │                 │   credentials   │              │
└─────────────────┘                 └─────────────────┘                 └──────────────┘
```

**Bezpečné řešení:**
1. **Dedikovaný Service Account** s omezenými právy
2. **Client Secret** uložený v environment variables
3. **Token caching** s refresh mechanismem  
4. **Audit logging** všech admin operací
5. **Rate limiting** a monitoring

## 🧱 Architektura

### Frontend
- **React** s **TypeScriptem**
- Vite jako dev server a bundler
- Modulární GUI komponenty
- **Strukturované logování** - logger.js posílá logy na backend endpoint
- Vizualizační nástroje (např. React Flow pro řízení stavů)

### Backend
- **Spring Boot** (Java 21)
- REST API
- **Logback + Loki4j appender** pro centralizované logování
- **FrontendLogsController** - přijímá frontend logy a přeposílá do Loki
- Připojení na PostgreSQL
- Připraven pro rozšíření o:
  - Metadata engine
  - Procesní engine
  - Modulární security
  - DMS integrace
  - Auditní logy

### Infrastruktura & Logování
- Docker Compose pro vývojové prostředí
- PostgreSQL jako databáze
- PgAdmin pro správu DB
- **Hybridní logování:**
  - **Frontend** → HTTP POST → **Backend** → **Loki** (jediná možnost pro React)
  - **Backend** → **Loki4j appender** → **Loki** (přímé logování)
  - **Ostatní služby** → **Promtail** → **Loki** (Docker log driver)
- **Grafana** pro vizualizaci logů a metrik
- Jaeger (volitelně) pro tracing
- Keycloak pro autentizaci (OIDC)

## 🧪 Vývojové prostředí

### Požadavky
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Node.js ≥ 20](https://nodejs.org/) (pouze pro frontend development)
- [Java 21](https://adoptium.net/)
- [Visual Studio Code](https://code.visualstudio.com/) + doporučené pluginy:
  - ESLint
  - Prettier
  - Java
  - Docker
  - GitHub Copilot

### Složky
```
📦 core-platform/
 ┣ 📂 backend/             ← Spring Boot backend + Logback konfigurace
 ┣ 📂 frontend/            ← React frontend + logger.js
 ┣ 📂 docker/              ← Docker Compose, Grafana, Loki, Promtail konfigurace
 ┣ 📂 docs/                ← Dokumentace platformy
 ┣ 📂 tools/               ← Nástroje a utility (např. datové migrace)
 ┣ 📂 .vscode/             ← Nastavení vývojového prostředí
 ┗ 📄 README.md
```

## 🚀 Spuštění vývojového prostředí

```bash
cd docker
docker compose up -d --build
```

**Služby:**
- Frontend: http://localhost:3000  
- Backend: http://localhost:8080  
- **Grafana**: http://localhost:3001 (admin/admin)
- **Loki**: http://localhost:3100 (health: /ready)
- **Promtail**: http://localhost:9080
- PgAdmin: http://localhost:5050
- Keycloak: http://localhost:8081

## 📊 Logování & Monitoring

### Hybridní logovací architektura

```
┌─────────────┐    HTTP POST     ┌─────────────┐    Loki4j      ┌──────────┐
│  Frontend   │ ───────────────→ │   Backend   │ ─────────────→ │   Loki   │
│ (React)     │ /api/frontend-   │ (Spring)    │   appender     │          │
└─────────────┘     logs         └─────────────┘                └──────────┘
                                                                      ↑
┌─────────────┐    Docker logs   ┌─────────────┐    HTTP POST          │
│Infrastructure│ ─────────────→ │  Promtail   │ ──────────────────────┘
│ (DB,Grafana)│                  │             │
└─────────────┘                  └─────────────┘
```

### ✅ Funguje
- **Frontend logování** - logger.js v frontend/src/services/logger.js
- **Promtail sbírání** infrastrukturních logů
- **Grafana vizualizace** - předdefinované dashboardy
- **Docker networking** - všechny služby komunikují přes core-net

### ❌ TODO (známé problémy)
- **Backend Loki appender** - backend logy se nedostávají do Loki (DNS/networking issue)
- **Audit logging** - implementace audit trail
- **Log retention** - automatické mazání starých logů

### Grafana Dashboards
- **App Overview** - celkový přehled aplikace
- **Performance** - metriky výkonu a response times  
- **Security** - bezpečnostní události
- **Audit** - auditní logy uživatelských akcí

### Testování logování

**Frontend logger (testovací tlačítka v DEV módu):**
```javascript
import { logger } from './services/logger';

logger.info('TEST_INFO', 'Test message', { key: 'value' });
logger.error('TEST_ERROR', 'Error message', { error: 'details' });
logger.security('SECURITY_EVENT', 'Security violation', { ip: '1.2.3.4' });
```

**Grafana Explore dotazy:**
```
{source="frontend"}           # Frontend logy
{source="backend"}            # Backend logy (momentálně nefunguje)
{container=~".*"}            # Všechny logy
{level="error"}              # Pouze error logy
{event_type="security"}      # Bezpečnostní události
```

## 🔗 Proxy API (Vite)

Frontend proxy přesměrovává `/api` na backend, viz `vite.config.ts`:
```ts
server: {
  proxy: {
    '/api': {
      target: 'http://core-backend:8080',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api/, ''),
    }
  }
}
```

## 🛠️ Docker Networking

**⚠️ Důležité**: Všechny konfigurace musí používat správné **DNS názvy kontejnerů**:

- **Loki**: `core-loki:3100` (NIKOLI `loki:3100`)
- **Backend**: `core-backend:8080` 
- **Frontend**: `core-frontend:3000`
- **Database**: `core-db:5432`

**Síť**: `docker_core-net` (automaticky vytvořena)

### Konfigurační soubory s DNS odkazy:
- `backend/src/main/resources/logback-spring.xml` - Loki appender URL
- `backend/.../FrontendLogsController.java` - LOKI_URL konstanta
- `docker/promtail/config.yml` - Loki client URL

## 🧹 Úklid konfigurace

**Smazané nepotřebné soubory:**
- `docker-compose.dev.yml`
- `docker-compose.direct-logging.yml` 
- `docker-compose.loki-driver.yml`
- `docker-compose.hybrid-logging.yml`

**Ponechán pouze:** `docker-compose.yml` s integrovanou hybridní konfigurací

## 🧑‍💻 Příprava pro GitHub Copilot

Copilot získá kontext z:
- `README.md` (tento soubor)
- Projektové struktury a názvů složek
- `.vscode/settings.json`, `tsconfig.json`, `.prettierrc`, `.eslintrc`
- Názvů a struktur komponent, endpointů a entit

➡️ Doporučeno začít vývoj s komentovaným skeletonem komponent / API / služeb.

## 📦 Roadmapa

- [x] Docker stack pro vývoj
- [x] Vite + React + Spring Boot propojeno
- [x] Frontend logging do Loki (přes backend)
- [x] Grafana dashboardy pro monitoring
- [x] Hybridní logování konfigurace
- [ ] **Backend Loki appender fix** (hlavní TODO)
- [ ] GUI Designer
- [ ] Metadata Editor
- [ ] BPM Engine
- [ ] Uživatelské role a oprávnění
- [ ] Auditní logy

## 🔐 Přihlášení (Keycloak)

Umístění
- docker/docker-compose.yml → služby db a keycloak (import realmu)
- docker/keycloak/realm-core-platform.json → realm core-platform (klienti web, api; role admin; uživatel test/Test.1234)
- frontend/public/auth → přihlašovací stránka (keycloak-js), přesměrování do aplikace

Spuštění
- docker compose -f docker/docker-compose.yml up -d db keycloak
- Keycloak UI: http://localhost:8081 (admin/admin)
- Login stránka: http://localhost:3000/auth/

Frontend
- KEYCLOAK_CFG je v frontend/public/auth/index.html (url, realm, clientId=web)
- APP_URL určuje, kam po přihlášení pokračovat (výchozí „/")

Backend
- Ověřuj JWT proti issueru: http://localhost:8081/realms/core-platform (z hosta)
- V kontejneru používej: http://keycloak:8080/realms/core-platform
- Očekávaná audience: api (claim aud)
- Role z claimu: realm_access.roles
- Doporučené env: OIDC_ISSUER, OIDC_API_AUDIENCE=api, CORS_ORIGINS=http://localhost:3000

Troubleshooting
- 401: zkontroluj ISS a audience=api, i čas (clock skew)
- Redirect/CORS: Redirect URIs a Web Origins u klienta „web“ musí obsahovat http://localhost:3000/*
- Chybí audience=api: client scope „api-audience“ je v defaultClientScopes klienta „web“

Kontrola docker-compose (rychlý checklist)
- keycloak běží na 8081, db běží a je healthy
- Volume pro import realmu: docker/keycloak → /opt/keycloak/data/import:ro
- Backend má v docker-compose dependency na keycloak a env OIDC_ISSUER=http://keycloak:8080/realms/core-platform
- Frontend běží na 3000 a obsluhuje /auth

🧹 Úklid: docker/node_modules
- Složka docker/node_modules do repozitáře nepatří. Smaž ji a přidej do .gitignore:
  - rm -rf docker/node_modules
  - do .gitignore přidej řádek: docker/node_modules/

---

> Tento repozitář obsahuje **jádro platformy**. Konkrétní aplikace budou vznikat jako samostatné repozitáře, které budou importovat `core-platform` jako submodul nebo závislost.

## 🔐 **SECURITY IMPLEMENTATION ROADMAP**

### **FÁZE 1: OKAMŽITÉ OPRAVY (CRITICAL)**
- [ ] **Refactor KeycloakClient.getAdminToken()** - použít service account s proper credentials
- [ ] **Environment variables** pro všechny secrets (client secrets, DB passwords)
- [ ] **Audit logging** pro admin operace (user management, password changes)
- [ ] **Input validation** a sanitization všech user inputs

### **FÁZE 2: SECURITY HARDENING (HIGH)**  
- [ ] **Rate limiting** pro authentication endpointy
- [ ] **Token caching** s secure storage a refresh
- [ ] **HTTPS enforcement** ve všech prostředích
- [ ] **Security headers** (CSP, HSTS, X-Frame-Options)
- [ ] **Vulnerability scanning** CI/CD pipeline

### **FÁZE 3: ADVANCED SECURITY (MEDIUM)**
- [ ] **Multi-factor authentication** support
- [ ] **Session management** s proper timeout
- [ ] **IP whitelisting** pro admin operace
- [ ] **Security monitoring** a alerting
- [ ] **Regular security audits**
