# GitHub Copilot - Core Platform Project Rules

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
make dev-clean         # restart s novými konfiguracemi
```

### 4. **PRODUCTION WORKFLOW**
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

### 5. **DEV MODE (NEFUNGUJE - NEPOUŽÍVAT)**
❌ **NEPOUŽÍVAT**: `make dev-up`, `make dev-clean`, `make dev-*` - nefungují v tomto projektu  
✅ **POUŽÍVAT**: `make clean-fast`, `make rebuild`, `make up`
### 6. **DEBUGGING WORKFLOW**
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

### Debug Grafana SSO
```bash
# 1. Logy z backendu
make logs-backend | grep -i grafana

# 2. Nginx logy (auth errors)
make logs | grep -i "auth request"

# 3. Playwright test
cd e2e
npx playwright test specs/monitoring/grafana-sso-debug.spec.ts
```

## 🚫 ANTI-PATTERNS (CO NEDĚLAT)

1. ❌ `docker restart core-backend` → ✅ `make clean-fast`
2. ❌ `docker logs core-backend` → ✅ `make logs-backend`
3. ❌ `vim .env` → ✅ `vim .env.template && make env-generate`
4. ❌ `docker-compose up` → ✅ `make up`
5. ❌ Rebuild frontendu po změně `.tsx` → ✅ Hot reload automatický
6. ❌ Zapomenout rebuild po změně `.java` → ✅ VŽDY `make clean-fast`

## 💡 TIPS

- **Rychlý dev loop**: `make dev-watch` (foreground mode s live logy)
- **Full clean**: `make clean` (rebuild + E2E testy, slow)
- **Fast clean**: `make clean-fast` (rebuild bez E2E, dev mode)
- **Unit testy**: `make test-backend` (fast, 2-5 min)
- **Full testy**: `make test-backend-full` (unit + integration, 10-15 min)

## 🎓 KDYŽ SI NEJSI JISTÝ

1. Zkontroluj `make help` a `make help-advanced`
2. Logy VŽDY přes `make logs-*` targets
3. Po změně backendu VŽDY `make dev-clean`
4. Šablony edituj, ne finální soubory
5. Když nevíš jak dál: `make doctor` (build diagnostika)
