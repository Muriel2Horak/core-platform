# GitHub Copilot - Core Platform Project Rules

> 📚 **Kompletní build dokumentace:** [Golden Rules](copilot-golden-rules.md)

## 🚨 CRITICAL WORKFLOW RULES

### 1. **REBUILD AFTER CODE CHANGES**
- **Backend změny** (Java/Spring Boot): Po jakékoli změně `.java` souborů je nutný rebuild
  ```bash
  make clean-fast       # Clean rebuild BEZ E2E testů (DOPORUČENO pro dev)
  make rebuild-backend  # Rebuild pouze backendu
  make clean            # Full rebuild + E2E testy (pro CI/CD)
  ```
- **Frontend změny** (TypeScript/React): Po změně v `frontend/src` rebuild není nutný (hot reload), ale pro nové dependencies:
  ```bash
  make rebuild-frontend
  ```
- **⚠️ POZNÁMKA**: `dev-*` targety nefungují v tomto projektu, používej `clean-fast` nebo `rebuild`

### 2. **LOGY POUZE Z LOKI** 
❌ **NIKDY NEPOUŽÍVAT**: `docker logs <container>`  
✅ **VŽDY POUŽÍVAT**: Makefile targets pro Loki

```bash
make logs              # Všechny logy
make logs-backend      # Backend logy  
make logs-frontend     # Frontend logy
make logs-keycloak     # Keycloak logy
make logs-errors       # ERROR logy ze všech služeb
```

**Proč?** Logy jsou v JSON formátu, Loki je centralizovaný agregátor, `docker logs` dává nečitelný output.

### 3. **KONFIGURACE ZE ŠABLON**
❌ **NIKDY RUČNĚ NEEDITOVAT**:
- `.env` soubory
- `docker-compose.yml` v kořenu
- konfigurace v `config/` adresáři

✅ **VŽDY EDITOVAT ŠABLONY**:
- `.env.template` → pak `make env-generate`
- `docker-compose.template.yml` → pak `make compose-generate`
- `config/*.template` → pak `make config-generate`

**Workflow:**
```bash
# 1. Edituj šablonu
vim .env.template

# 2. Vygeneruj finální soubory
make env-generate      # nebo make compose-generate, config-generate

# 3. Aplikuj změny
make clean-fast        # restart s novými konfiguracemi
```

### 4. **ENVIRONMENT VARIABLES - SPRÁVNÉ POUŽÍVÁNÍ**

#### ❌ NIKDY HARDCODE VALUES V KÓDU
```java
// ❌ ŠPATNĚ - hardcoded DB URL
spring.datasource.url=jdbc:postgresql://db:5432/core

// ✅ SPRÁVNĚ - použij env var placeholder
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://core-db:5432/core}
```

```typescript
// ❌ ŠPATNĚ - hardcoded API URL
const API_URL = "https://admin.core-platform.local";

// ✅ SPRÁVNĚ - použij env var
const API_URL = process.env.API_BASE || "https://admin.core-platform.local";
```

#### ✅ SPRÁVNÝ WORKFLOW PRO ENV VARS
1. **Přidej do `.env.template`** (NIKDY ne do `.env`)
2. **Použij v kódu**: `${VARIABLE_NAME}` (Spring), `process.env.VAR` (Node.js)
3. **Vygeneruj .env**: `cp .env.template .env` (nebo `make env-generate`)
4. **Validate**: `make env-validate` nebo `make doctor`

#### 🔒 SECURITY PRAVIDLA
- `.env` obsahuje secrets → **NIKDY necommituj** (je v `.gitignore`)
- `.env.template` je vzor → **commituj do Gitu** (BEZ secrets)
- Hardcoded credentials v kódu → **ZAKÁZÁNO** (security audit fail)
- SSL private keys → **NIKDY do Gitu** (`docker/ssl/*.key.pem` v `.gitignore`)

**Validace environment:**
```bash
make env-validate    # Rychlá kontrola .env existence a hodnot
make doctor          # Full health check (env + služby + konektivita)
```

**Pokud chybí proměnné:**
```bash
# Doctor ti řekne CO chybí
make doctor

# Oprav v .env
vim .env
# Přidej chybějící: KEYCLOAK_BASE_URL, POSTGRES_PASSWORD, atd.

# Verify
make env-validate
```

#### 📖 DOKUMENTACE ENV VARS
- **Kompletní audit**: `SECURITY_CONFIG_AUDIT.md` (1293 řádků)
- **47 environment variables** mapovaných
- **12 secrets/credentials** identifikovaných
- **3 substitution mechanismy** vysvětlené

### 5. **PRODUCTION WORKFLOW**
make env-generate      # nebo make compose-generate, config-generate

# 3. Aplikuj změny
make dev-clean         # restart s novými konfiguracemi
```

### 5. **PRODUCTION WORKFLOW**
```bash
# Start production environment
make up

# Po změně backendu (.java soubory)
make clean-fast        # NUTNÝ REBUILD! (bez E2E testů)
# NEBO
make rebuild-backend   # Rebuild pouze backend

# Po změně frontendu (.ts/.tsx soubory)  
make rebuild-frontend  # Rebuild frontend

# Kontrola zdraví
make verify

# Zastavení
make down
```

### 6. **DEV MODE (NEFUNGUJE - NEPOUŽÍVAT)**
❌ **NEPOUŽÍVAT**: `make dev-up`, `make dev-clean`, `make dev-*` - nefungují v tomto projektu  
✅ **POUŽÍVAT**: `make clean-fast`, `make rebuild`, `make up`

### 7. **DEBUGGING WORKFLOW**
```bash
# 1. Zjisti co se děje
make logs-errors       # ERROR logy

# 2. Konkrétní služba
make logs-backend
make logs-frontend  

# 3. Health check
make verify

# 4. Pokud backend nereaguje
make clean-fast        # Force rebuild + restart
```

## 📁 PROJEKTOVÁ STRUKTURA

```
core-platform/
├── .env.template           # ✅ EDITUJ TOTO (ne .env)
├── docker-compose.template.yml  # ✅ EDITUJ TOTO  
├── Makefile               # Hlavní build orchestrace
├── backend/
│   ├── src/main/java/     # Java kód (REBUILD NUTNÝ)
│   └── pom.xml
├── frontend/
│   └── src/               # TS/React (hot reload)
├── docker/
│   ├── backend/Dockerfile.dev
│   └── frontend/Dockerfile.dev
├── .devcontainer/
│   └── docker-compose.devcontainer.yml  # Dev overlay
└── e2e/                   # Playwright testy
```

## 🎯 COMMON SCENARIOS

### Změna Backend Controller
```bash
# 1. Edituj .java soubor
vim backend/src/main/java/cz/muriel/core/monitoring/GrafanaAuthBridgeController.java

# 2. POVINNÝ REBUILD
make clean-fast        # NUTNÝ REBUILD! (bez E2E testů)

# 3. Verify
make logs-backend  # Zjisti jestli nastartoval
make verify        # Health check
```

### Přidání Environment Variable
```bash
# 1. Edituj TEMPLATE (ne .env!)
vim .env.template

# 2. Vygeneruj .env
make env-generate

# 3. Restart s novou konfigurací
make clean-fast
```

### 4. **ENVIRONMENT VARIABLES - SPRÁVNÉ POUŽÍVÁNÍ**

> 📖 **Kompletní dokumentace:** [SECURITY_CONFIG_AUDIT.md](../SECURITY_CONFIG_AUDIT.md)

#### ❌ NIKDY HARDCODE VALUES V KÓDU
```java
// ❌ ŠPATNĚ - hardcoded DB URL
spring.datasource.url=jdbc:postgresql://db:5432/core

// ✅ SPRÁVNĚ - použij env var placeholder
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://core-db:5432/core}
```

```typescript
// ❌ ŠPATNĚ - hardcoded API URL
const API_URL = "https://admin.core-platform.local";

// ✅ SPRÁVNĚ - použij env var
const API_URL = process.env.API_BASE || "https://admin.core-platform.local";
```

#### ✅ SPRÁVNÝ WORKFLOW PRO ENV VARS
1. **Přidej do `.env.template`** (NIKDY ne do `.env`)
2. **Použij v kódu**: `${VARIABLE_NAME}` (Spring), `process.env.VAR` (Node.js)
3. **Vygeneruj .env**: `cp .env.template .env` (nebo `make env-generate`)
4. **Validate**: `make env-validate` nebo `make doctor`

#### 🔒 SECURITY PRAVIDLA
- `.env` obsahuje secrets → **NIKDY necommituj** (je v `.gitignore`)
- `.env.template` je vzor → **commituj do Gitu** (BEZ secrets)
- Hardcoded credentials v kódu → **ZAKÁZÁNO** (security audit fail)
- SSL private keys → **NIKDY do Gitu** (`docker/ssl/*.key.pem` v `.gitignore`)

**Validace environment:**
```bash
make env-validate    # Rychlá kontrola .env existence a hodnot
make doctor          # Full health check (env + služby + konektivita)
```

**Pokud chybí proměnné:**
```bash
# Doctor ti řekne CO chybí
make doctor

# Oprav v .env
vim .env
# Přidej chybějící: KEYCLOAK_BASE_URL, POSTGRES_PASSWORD, atd.

# Verify
make env-validate
```

#### 📖 DOKUMENTACE ENV VARS
- **Kompletní audit**: `SECURITY_CONFIG_AUDIT.md` (1293 řádků)
- **47 environment variables** mapovaných
- **12 secrets/credentials** identifikovaných
- **3 substitution mechanismy** vysvětlené

### 5. **PRODUCTION WORKFLOW**
```bash
# Start production environment
make up

# Po změně backendu (.java soubory)
make clean-fast        # NUTNÝ REBUILD! (bez E2E testů)
# NEBO
make rebuild-backend   # Rebuild pouze backend

# Po změně frontendu (.ts/.tsx soubory)  
make rebuild-frontend  # Rebuild frontend

# Kontrola zdraví
make verify

# Zastavení
make down
```

### 6. **DEV MODE (NEFUNGUJE - NEPOUŽÍVAT)**
❌ **NEPOUŽÍVAT**: `make dev-up`, `make dev-clean`, `make dev-*` - nefungují v tomto projektu  
✅ **POUŽÍVAT**: `make clean-fast`, `make rebuild`, `make up`

### 7. **DEBUGGING WORKFLOW**
```bash
# 1. Zjisti co se děje
make logs-errors       # ERROR logy

# 2. Konkrétní služba
make logs-backend
make logs-frontend  

# 3. Health check
make verify

# 4. Pokud backend nereaguje
make clean-fast        # Force rebuild + restart
```

### 8. **DEBUG GRAFANA SSO**

# 2. Nginx logy (auth errors)
make logs | grep -i "auth request"

# 3. Playwright test
cd e2e
npx playwright test specs/monitoring/grafana-sso-debug.spec.ts
```

### 8. **DEBUG GRAFANA SSO**
```bash
# 1. Logy z backendu
make logs-backend | grep -i grafana

# 2. Nginx logy (auth errors)
make logs | grep -i "auth request"

# 3. Playwright test
cd e2e
npx playwright test specs/monitoring/grafana-sso-debug.spec.ts
```

---

## 🚫 ANTI-PATTERNS (CO NEDĚLAT)

| ❌ ŠPATNĚ | ✅ SPRÁVNĚ | DŮVOD |
|-----------|------------|-------|
| `docker restart core-backend` | `make clean-fast` | Neaplikuje změny kódu |
| `docker logs core-backend` | `make logs-backend` | Loki centralizuje + filtruje |
| `vim .env` | `vim .env.template && make env-generate` | .env je generovaný |
| `docker-compose up` | `make up` | Makefile má pre-hooks |
| Rebuild frontendu po změně `.tsx` | Čekej hot reload | Zbytečné, dev server ho dělá |
| Zapomenout rebuild po změně `.java` | `make clean-fast` VŽDY | Java není hot-reload |
| Hardcoded DB URLs v `application.properties` | Použij `application.yml` s `${DATABASE_URL}` | Properties přepíší env vars! |
| `.env` commitovat do Gitu | Je v `.gitignore` | Obsahuje secrets! |
| Spustit build bez validace env | `make env-validate` nebo `make doctor` | Zjistí chybějící proměnné |
| Stejné heslo pro všechny DB | Separátní users per služba | Viz `DB_SEPARATE_USERS_PLAN.md` |

## 💡 TIPS

- **Rychlý dev loop**: `make clean-fast` (rebuild bez E2E, ~5-10 min)
- **Full clean**: `make clean` (rebuild + E2E testy, ~30-40 min)
- **Unit testy**: `make test-backend` (fast, 2-5 min)
- **Full testy**: `make test-backend-full` (unit + integration, 10-15 min)
- **Environment check**: `make env-validate` (rychlé) nebo `make doctor` (full health check)

## 🎓 KDYŽ SI NEJSI JISTÝ

1. Zkontroluj `make help` a `make help-advanced`
2. Logy VŽDY přes `make logs-*` targets
3. Po změně backendu VŽDY `make clean-fast`
4. Šablony edituj, ne finální soubory
5. Když nevíš jak dál: `make doctor` (build diagnostika + env validation)
6. **Security audit**: Přečti `SECURITY_CONFIG_AUDIT.md` před změnou konfigurace
7. **DB users**: Plán separace je v `DB_SEPARATE_USERS_PLAN.md`

## 🔒 SECURITY BEST PRACTICES

### Co NIKDY nedělat:
- ❌ Commitovat `.env` do Gitu (obsahuje secrets)
- ❌ Hardcoded credentials v kódu (použij env vars)
- ❌ Stejné heslo pro všechny databáze
- ❌ Plain-text passwords v application.properties
- ❌ SSL private keys do Git repository

### Co VŽDY dělat:
- ✅ Používat `.env.template` jako vzor (bez secrets)
- ✅ Environment variables v `application.yml` s `${VAR}` syntax
- ✅ Validovat env před buildem: `make env-validate`
- ✅ Separátní DB users per služba (viz `DB_SEPARATE_USERS_PLAN.md`)
- ✅ Generovat silná hesla: `openssl rand -base64 32`

### Dokumentace:
- **Kompletní security audit**: `SECURITY_CONFIG_AUDIT.md` (1293 řádků)
- **47 environment variables** s použitím a security levelem
- **12 secrets/credentials** s runtime access příklady
- **3 substitution mechanismy** (envsubst, Docker ${}, Spring ${})
- **DB users migration plan**: `DB_SEPARATE_USERS_PLAN.md`
