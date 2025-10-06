# Core Platform

Enterprise-ready **multitenantní** aplikace postavená na **Java 21 + Spring Boot 3.x** s **Keycloak** autentifikací, **React** frontendem a **PostgreSQL** databází.

## 🏗️ Architektura

- **Backend**: Java 21, Spring Boot 3.5.x, Spring Security OAuth2
- **Frontend**: React 18, TypeScript, Vite
- **Auth**: Keycloak 25.x s custom theme
- **Database**: PostgreSQL 16 s Flyway migrations
- **Monitoring**: Grafana + Loki + Prometheus stack
- **Deployment**: Docker Compose s SSL/HTTPS support

## 🏢 Multitenancy Features

### Core Infrastructure
- **Tenant-aware JWT**: Automatická extrakce tenant informací z JWT tokenů
- **Database filtering**: Hibernate filtry pro úplnou datovou izolaci
- **Caching**: Optimalizované cachování tenant dat s TTL
- **Logging**: Tenant-aware logování s MDC kontextem

### 🌐 Subdomain Architecture
- **1 realm = 1 tenant**: Každý tenant má vlastní Keycloak realm a subdoménu
- **Wildcard SSL**: `*.core-platform.local` certifikát pro neomezené subdomény
- **Automatic routing**: Nginx automaticky routuje `{tenant}.core-platform.local` na správný tenant kontext

### 🚀 Tenant Creation Workflow

#### 1. **Automatický setup (doporučeno)**
```bash
# První setup - nastaví domény automaticky
make dev-setup

# Spustí celé prostředí
make up

# Vytvoření nového tenantu přes API
curl -X POST https://core-platform.local/api/admin/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"key": "acme-corp", "displayName": "ACME Corporation"}'
```

#### 2. **Manuální setup**
```bash
# Přidat doménu pro nový tenant
make add-tenant-domain TENANT=acme-corp

# Nebo přímo scriptem
sudo scripts/setup-local-domains.sh add-tenant acme-corp
```

#### 3. **True Wildcard Support (macOS)**
```bash
# Pro neomezené subdomény bez manuálního přidávání
make setup-wildcard

# Pak funguje JAKÁKOLI subdoména
# https://anything.core-platform.local
```

### 🎯 Tenant Management API

**Vytvoření tenantu:**
```bash
POST /api/admin/tenants
{
  "key": "acme-corp",
  "displayName": "ACME Corporation"
}
```

**Seznam tenantů:**
```bash
GET /api/admin/tenants
```

**Smazání tenantu:**
```bash
DELETE /api/admin/tenants/acme-corp
```

### 🔧 Domain Management Commands

```bash
# Ukázat současnou konfiguraci domén
make show-domains

# Přidat doménu pro tenant
make add-tenant-domain TENANT=my-company

# Odebrat doménu tenantu  
make remove-tenant-domain TENANT=my-company

# Nastavit wildcard support (macOS)
make setup-wildcard
```

## 🚀 Quick Start

### Příprava
```bash
# Zkopíruj environment konfiguraci
cp .env.example .env
```

### Spuštění
```bash
# Build a spustí všechny služby
docker compose up --build -d

# Sledování logů
docker compose logs -f backend keycloak
```

## 🔄 Správa Dat a Restartů

### Typy Restartů
Core Platform nabízí různé možnosti restartu podle vašich potřeb:

#### 🔄 **Běžný Restart (Zachová VŠECHNA data)**
```bash
make restart
```
- Standardní restart pro běžný vývoj
- **Zachová**: Keycloak customizace, databázová data, uživatelská nastavení
- **Použití**: Denní vývoj, po změnách kódu

#### 🆕 **Fresh Start (Smaže JEN Keycloak data)**
```bash
make fresh
```
- Smaže pouze Keycloak data, zachová aplikační databázi
- **Zachová**: Všechna aplikační data v PostgreSQL
- **Smaže**: Keycloak realms, uživatele, role, customizace
- **Použití**: Reset autentifikace při zachování app dat
- ⚠️ **5 sekund na zrušení**

#### 🔄 **Reset Keycloak**
```bash
make reset-kc
```
- Rychlý reset pouze Keycloak do výchozího stavu
- Obnoví základní realm a test uživatele
- ⚠️ **3 sekundy na zrušení**

#### 💾 **Reset Databáze**
```bash
make reset-db
```
- Smaže pouze aplikační data, zachová Keycloak
- **Zachová**: Keycloak nastavení, uživatele, role
- **Smaže**: Aplikační data v PostgreSQL
- ⚠️ **3 sekundy na zrušení**

#### 🧹 **Úplné Čištění**
```bash
make clean
```
- **SMAŽE VŠECHNA DATA** + rebuild všech images
- Kompletně čisté prostředí od začátku
- **Použití**: Před důležitými testy, po velkých změnách

### Správa Keycloak Customizací

#### ⚠️ Ztráta Customizací
Pokud si v Keycloak admin konzoli upravíte uživatele, role nebo nastavení:

- **`make restart`** → **Vaše změny ZŮSTANOU** ✅
- **`make fresh`** → **Vaše změny SE ZTRATÍ** ❌
- **`make reset-kc`** → **Vaše změny SE ZTRATÍ** ❌
- **`make clean`** → **Vaše změny SE ZTRATÍ** ❌

#### 💡 Best Practices
```bash
# Pro běžný vývoj - zachová customizace
make restart

# Pro testování s čistým Keycloak
make reset-kc

# Pro kompletní reset prostředí
make clean
```

#### 🔒 Výchozí Přihlašovací Údaje
Po každém reset Keycloak (`fresh`, `reset-kc`, `clean`):

**Keycloak Admin:**
- URL: http://localhost:8081/admin
- Username: `admin`
- Password: `admin123`

**Test uživatelé:**
- Username: `test` / Password: `Test.1234`
- Username: `test_admin` / Password: `Test.1234`

## 📊 API Endpoints

### Public API
- `GET /api/tenants/me` - Current tenant info
- `GET /api/users/me` - Current user info
- `GET /api/users/search?q=` - Search users in tenant

## 🔍 Kvalita kódu & preflight checks

Před každým commitem je důležité spustit kontroly kvality kódu, aby se předešlo chybám v runtime.

### Povinné kontroly před commitem

```bash
# Spusť v adresáři frontend/
npm run lint && npm run typecheck
```

### Detailní popis kontrol

**ESLint** - kontroluje:
- ✅ Správnost importů a exportů (default vs named)
- ✅ Neexistující moduly a komponenty  
- ✅ React best practices (hooks rules, JSX syntax)
- ✅ Nepoužité proměnné a importy

**TypeScript typecheck** - kontroluje:
- ✅ Typovou správnost kódu
- ✅ Kompatibilitu importů s `esModuleInterop: false`
- ✅ Správnost cest a aliasů

### VS Code integrace

Projekt má nakonfigurované `.vscode/settings.json` pro:
- 🔄 ESLint validaci v reálném čase (`onType`)
- 🎯 Použití workspace TypeScript verze
- ⚡ Okamžité zvýraznění chyb v editoru

### Runtime safety

Aplikace obsahuje:
- 🛡️ **ErrorBoundary** - zachytává chyby komponent místo pádu celé aplikace
- 🔒 **Component guards** - kontrolují platnost komponent před renderem
- 📋 **Jasné error hlášky** - místo cryptic React error #130

### CI/CD integrace

V CI pipeline by měly být tyto kroky povinné:
```yaml
- name: Lint check
  run: npm run lint
- name: Type check  
  run: npm run typecheck
```

### Poznámky k nastavení

- `allowSyntheticDefaultImports` a `esModuleInterop` jsou dočasně vypnuty pro přísné odhalení default/named záměn
- Po vyčištění všech chyb lze tyto volby vrátit na `true` pro pohodlnější development
- CI musí i nadále procházet bez chyb

## 📋 Troubleshooting

### General Issues
1. **Services not starting**:
   - Zkontroluj Docker logy: `docker compose logs`
   - Ověř dostupnost portů: `lsof -i :8080,8443,5432`
   - Zkontroluj disk space: `docker system df`

2. **Authentication issues**:
   - Ověř Keycloak admin credentials
   - Zkontroluj realm configuration
   - Zkontroluj JWT token validitu

3. **Database connectivity**:
   - Ověř PostgreSQL connection string
   - Zkontroluj database credentials
   - Sleduj logy: `docker logs core-db`

## 🌐 Síťová Architektura

### Rozdělení External vs Internal sítě

⚠️ **DŮLEŽITÉ**: Nepomíchej externí domény s interní Docker sítí!

### 🌍 **EXTERNÍ - Uživatelské URL (před nginx)**
```
https://admin.core-platform.local      → Admin frontend + Keycloak admin realm
https://tenant1.core-platform.local    → Tenant1 frontend + tenant1 realm  
https://tenant2.core-platform.local    → Tenant2 frontend + tenant2 realm
https://company-a.core-platform.local  → Company-A frontend + company-a realm
```

### 🐳 **INTERNÍ - Docker síť (za nginx)**
```
nginx:443 → frontend:80    (React app)
nginx:443 → backend:8080   (Spring Boot API)  
nginx:443 → keycloak:8443  (Keycloak server - HTTPS)
nginx:443 → db:5432        (PostgreSQL)
```

### 🔧 **Konfigurace pravidla:**

| Komponenta | Externí doména | Interní hostname | Účel |
|------------|---------------|------------------|------|
| **Nginx** | `*.core-platform.local:443` | `nginx:443` | Revere proxy + SSL termination |
| **Frontend** | Přes nginx | `frontend:80` | React SPA |
| **Backend** | Přes nginx `/api/*` | `backend:8080` | REST API |
| **Keycloak** | Přes nginx `/realms/*`, `/admin/*` | `keycloak:8443` | Auth server |
| **Database** | Nedostupná zvenčí | `db:5432` | PostgreSQL |

### 🎯 **Keycloak konfigurace:**
```yaml
# ✅ SPRÁVNĚ - Keycloak hostname je interní Docker název
KC_HOSTNAME: keycloak  # nebo admin.core-platform.local pro external

# ✅ SPRÁVNĚ - Realm templates používají externí domény pro redirecty  
"frontendUrl": "https://admin.${DOMAIN}"
"redirectUris": ["https://admin.${DOMAIN}/*"]

# ❌ ŠPATNĚ - Míchat interní a externí!
KC_HOSTNAME: core-platform.local  # externí v interní konfiguraci
```

### 🔄 **Workflow:**
1. **DNS**: `admin.core-platform.local` → `127.0.0.1` (dnsmasq)
2. **Nginx**: Zachytí external request na port 443
3. **Routing**: `admin.core-platform.local/realms/*` → `keycloak:8443/realms/*`
4. **Keycloak**: Vrací response s correct external URLs
5. **Browser**: Redirecty používají external domény

---
