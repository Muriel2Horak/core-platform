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

### 🔗 Realtime User Synchronization 
**Nová funkčnost v Step 2**: Automatická synchronizace uživatelů z Keycloak do lokální `users_directory`.

#### Architektura Sync
1. **Keycloak SPI Webhook**: Custom Event Listener pro real-time události
2. **Backend Webhook Endpoint**: Zabezpečený `/internal/keycloak/events` endpoint
3. **Projection Service**: Idempotentní projekce Keycloak událostí do users_directory
4. **Daily Backfill**: Scheduled job pro denní reconciliaci dat

#### Podporované Events
- `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`
- `ROLE_*` (realm i client roles)  
- `GROUP_MEMBERSHIP_*` (přidání/odebrání ze skupin)

#### Zabezpečení
- **Webhook Secret**: Shared secret mezi Keycloak a Backend
- **Network Security**: Pouze internal Docker network
- **Idempotence**: SHA-256 hash prevence duplicitních událostí

## 🚀 Quick Start

### Příprava
```bash
# 1. Zkopíruj environment konfiguraci
cp .env.example .env

# 2. Vygeneruj webhook secret
openssl rand -hex 32

# 3. Nastav secrets v .env souboru
KC_EVENT_WEBHOOK_SECRET=your-generated-secret-here
APP_KEYCLOAK_WEBHOOK_SECRET=your-generated-secret-here
```

### Spuštění
```bash
# Build a spustí všechny služby (včetně Keycloak SPI)
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

### Ověření Webhook Funkčnosti

1. **Spusť aplikaci**: `docker compose up --build -d`

2. **Zkontroluj Keycloak SPI**: 
   ```bash
   docker logs core-keycloak | grep "core-platform-webhook"
   # Měl by zobrazit: "Registered provider core-platform-webhook"
   ```

3. **Vytvoř/změň uživatele v Keycloak**: 
   - Otevři http://localhost:8081/admin 
   - Přihlaš se jako admin/admin
   - V realm "core-platform" vytvoř nebo uprav uživatele

4. **Ověř webhook příjem**:
   ```bash
   docker logs core-backend | grep "webhook event"
   # Měl by zobrazit: "Received webhook event: type=USER_UPDATED, tenant=..."
   ```

5. **Zkontroluj users_directory**:
   ```bash
   # Připoj se k DB
   docker exec -it core-db psql -U core -d core
   
   # Zkontroluj synchronizované uživatele
   SELECT username, email, active, tenant_id FROM users_directory;
   ```

### Test API Endpoints
```bash
# Získej JWT token (přes frontend nebo direct)
export TOKEN="your-jwt-token"

# Test user search - měl by vrátit aktuální data z users_directory
curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/api/users/search?q=testuser"

# Test current user info  
curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/api/users/me"
```

### Backfill Job Testing
```bash
# Dočasně změň cron pro test (např. každou minutu)
# V .env: APP_KEYCLOAK_BACKFILL_CRON=0 * * * * *

# Restartuj backend
docker compose restart backend

# Sleduj logy backfill jobu
docker logs -f core-backend | grep "backfill"
```

## 📊 API Endpoints

### Public API
- `GET /api/tenants/me` - Current tenant info
- `GET /api/users/me` - Current user from directory  
- `GET /api/users/search?q=` - Search users in tenant

### Internal API
- `POST /internal/keycloak/events` - Keycloak webhook receiver (internal only)

## 🔧 Configuration

### Webhook Configuration
```bash
# Keycloak SPI environment variables
KC_EVENT_WEBHOOK_URL=http://backend:8080/internal/keycloak/events
KC_EVENT_WEBHOOK_SECRET=your-webhook-secret

# Backend environment variables  
APP_KEYCLOAK_WEBHOOK_SECRET=your-webhook-secret
```

### Backfill Configuration
```bash
# Enable/disable daily backfill
APP_KEYCLOAK_BACKFILL_ENABLED=true

# Cron expression (default: 3:25 AM daily)
APP_KEYCLOAK_BACKFILL_CRON=0 25 3 * * *

# Keycloak Admin API access
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
```

## 🏗️ Development

### Build Keycloak SPI
```bash
cd keycloak-spi-event-webhook
mvn clean package
# JAR je zkopírován do Keycloak při docker build
```

### Database Migrations
```bash
# Nová migrace V3 přidává:
# - kc_event_log tabulku pro idempotenci
# - rozšíření users_directory o role, groups, attributes
# Migrace se spouští automaticky při startu
```

### Testování
```bash
# Unit a integration testy
cd backend && ./mvnw test

# Multitenancy smoke testy
make test-and-report
```

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

### Webhook Issues
1. **Webhook events nepřicházejí**:
   - Zkontroluj Keycloak logy: `docker logs core-keycloak`
   - Ověř SPI registraci: hledej "core-platform-webhook" v lozích
   - Zkontroluj network connectivity mezi Keycloak a Backend

2. **401 Unauthorized na webhook**:
   - Ověř, že `KC_EVENT_WEBHOOK_SECRET` == `APP_KEYCLOAK_WEBHOOK_SECRET`
   - Zkontroluj environment variables: `docker exec core-backend env | grep WEBHOOK`

3. **Backend neprocessuje events**:
   - Zkontroluj backend logy: `docker logs core-backend | grep webhook`
   - Ověř, že tenant existuje v tabulce `tenants`
   - Zkontroluj database connectivity

### Backfill Issues
1. **Backfill job neběží**:
   - Ověř, že `APP_KEYCLOAK_BACKFILL_ENABLED=true`
   - Zkontroluj cron expression syntax
   - Sleduj logy: `docker logs core-backend | grep backfill`

2. **Admin API přístup selže**:
   - Ověř Keycloak admin credentials
   - Zkontroluj network connectivity na `http://keycloak:8080`

## 🔄 Data Flow

```
Keycloak Event → SPI Webhook → HTTP POST → Backend Endpoint
    ↓
Webhook Validation (secret + IP) → Event Processing → Users Directory
    ↓  
Idempotence Check → Tenant Validation → User Upsert/Delete
    ↓
Database Update → MDC Logging → Event Log Entry
```

## 📈 Monitoring

### Grafana Queries
```logql
# Webhook events
{service="backend"} |= "webhook event"

# Backfill job status  
{service="backend"} |= "backfill"

# User sync errors
{service="backend"} |= "Failed to process" |= "event"
```

### Metriky
- Počet zpracovaných webhook událostí
- Latence webhook processingu
- Backfill job úspěšnost
- User directory velikost per tenant

---

**Development Team**: Core Platform Engineering  
**Last Updated**: Step 2 - Realtime User Sync Implementation
