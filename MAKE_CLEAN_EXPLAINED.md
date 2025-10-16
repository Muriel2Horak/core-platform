# Make Clean - Complete Documentation

## 🎯 Co dělá `make clean`?

`make clean` provádí **ÚPLNÝ RESTART celého prostředí** od nuly včetně FULL E2E testování.

## 📋 Přesný Průběh

### Krok 1: 🧹 Cleanup (Smazání dat)
```bash
docker compose down --rmi local --volumes
```

**Co se smaže:**
- ✅ Všechny Docker kontejnery (backend, frontend, keycloak, postgres, kafka, etc.)
- ✅ Všechny Docker image (lokální buildy)
- ✅ Všechny Docker volumes (databáze, Kafka data, Keycloak data)
- ✅ Úplně čisté prostředí jako na nové instalaci

### Krok 2: 🧪 Pre-build Tests (Unit testy)
```bash
bash scripts/build/pre-build-test.sh all
```

**Co se testuje:**
- ✅ Backend unit testy (~7 test tříd, 2-5 minut)
  - Skipuje `*IT` integration testy (potřebují Docker)
- ✅ Frontend unit testy (~67 testů, Vitest)
- ❌ Pokud selžou → STOP, nic se nebuiluje

**Proč nejdřív testy?**
- Rychlá validace před náročným Docker buildem
- Ušetří čas pokud je něco rozbité

### Krok 3: 🏗️ Build Images (Sestavení Docker images)
```bash
docker compose build --parallel
```

**Co se builduje:**
- 🐳 Backend image (Spring Boot JAR + runtime)
- 🐳 Frontend image (npm build + nginx)
- 🐳 Keycloak image (custom with themes)
- 🐳 Ostatní services (postgres, kafka, grafana, loki, atd.)

**Build cache:**
- První build: ~5-15 minut
- Další buildy: rychlejší díky Docker layer cache

### Krok 4: 🚀 Start Services (Spuštění prostředí)
```bash
docker compose up -d
```

**Co se spustí:**
- ✅ PostgreSQL (hlavní DB + tenant DBs)
- ✅ Kafka + Zookeeper (streaming)
- ✅ Keycloak (autentizace)
- ✅ Backend (Spring Boot API)
- ✅ Frontend (React + nginx)
- ✅ Grafana (monitoring)
- ✅ Loki (logy)
- ✅ Prometheus (metriky)

**Startup čas:** ~60-120 sekund (čeká na health checks)

### Krok 5: 🎭 E2E Pre-Deploy Tests (Smoke testy)
```bash
make test-e2e-pre
```

**Co se testuje:**
- ✅ Login přes Keycloak
- ✅ Menu RBAC (admin/user visibility)
- ✅ Základní navigace
- ⚡ Rychlé - 5-7 minut
- 🎯 Cíl: Validace před scaffold dat

### Krok 6: 🧪 E2E Post-Deploy Tests (Full E2E)
```bash
make test-e2e-post
```

**Co se testuje:**
- ✅ Vytvoří test data (scaffold):
  - Test uživatele (test_admin, test_user, test_manager)
  - Test role
  - Test skupiny
  - Test tenanta
- ✅ Spustí full E2E scénáře:
  - Profile update
  - Directory consistency
  - User management
  - Role management
  - Group management
  - Tenant management
  - CRUD operace
- ✅ Cleanup test dat (teardown)
- 🐌 Pomalejší - 20-30 minut
- 🎯 Cíl: Kompletní validace celého stacku

---

## 🆚 Porovnání: `make clean` vs `make clean-fast`

| Feature | `make clean` | `make clean-fast` |
|---------|--------------|-------------------|
| **Smazání dat** | ✅ Všechny volumes | ✅ Všechny volumes |
| **Unit testy** | ✅ Backend + Frontend | ✅ Backend + Frontend |
| **Docker build** | ✅ Od nuly | ✅ Od nuly |
| **Spuštění services** | ✅ Všechny | ✅ Všechny |
| **E2E PRE-DEPLOY** | ✅ Ano (5-7 min) | ❌ Ne |
| **E2E POST-DEPLOY** | ✅ Ano (20-30 min) | ❌ Ne |
| **Celkový čas** | ~40-50 minut | ~15-20 minut |
| **Použití** | CI/CD, release validation | Development, quick reset |

---

## ⏱️ Časová Náročnost

### `make clean` (FULL)
```
1. Cleanup           4-5s
2. Unit tests        2-5 min
3. Docker build      5-15 min (první build)
4. Start services    1-2 min
5. E2E PRE           5-7 min
6. E2E POST          20-30 min
─────────────────────────────────
CELKEM:              35-60 min
```

### `make clean-fast` (DEV)
```
1. Cleanup           4-5s
2. Unit tests        2-5 min
3. Docker build      5-15 min
4. Start services    1-2 min
5. Smoke tests       30s
─────────────────────────────────
CELKEM:              10-25 min
```

---

## 🚀 Kdy Použít Co?

### ✅ Použij `make clean` když:
- 🎯 Potřebuješ **úplné ověření** celého stacku
- 🚀 Připravuješ **release nebo deploy**
- 🐛 Máš **podezření na data corruption**
- 📊 Chceš **kompletní E2E test report**
- 🔄 Testuješ **full workflow scenarios**

### ⚡ Použij `make clean-fast` když:
- 💻 **Lokální development** - rychlý restart
- 🔧 **Testování změn** v kódu bez E2E
- 🧪 Chceš jen **unit testy + smoke test**
- ⏰ **Nemáš čas** na full E2E (40+ minut)
- 🔄 **Iterativní vývoj** - rebuild několikrát za den

### 🏃 Použij `make rebuild` když:
- ⚡ Chceš **nejrychlejší rebuild** (s cache)
- 🔄 **Nechceš mazat data** (volumes zůstanou)
- ✅ Jen **unit testy** (bez E2E)
- 💾 **Development** - zachovat DB state

---

## 📊 Co Všechno Se Smaže?

### ✅ `make clean` SMAŽE:

**Docker Kontejnery:**
```
backend, frontend, keycloak
postgres, kafka, zookeeper
grafana, loki, prometheus
nginx, wszystkie kontejnery
```

**Docker Images (lokální buildy):**
```
core-platform-backend:latest
core-platform-frontend:latest
core-platform-keycloak:latest
```

**Docker Volumes (data):**
```
postgres_data       → Všechny databáze (main + tenants)
kafka_data          → Všechny Kafka topics
keycloak_data       → Všichni uživatelé, realmy, role
grafana_data        → Dashboardy, datasources
loki_data           → Logy
prometheus_data     → Metriky
```

### ⚠️ Co `make clean` NESMAŽE:

- ✅ Source code (samozřejmě)
- ✅ node_modules (frontend dependencies)
- ✅ target/ (backend build artifacts)
- ✅ .env soubor (konfigurace)
- ✅ Docker cache layers (pro rychlejší rebuild)

---

## 🎯 Příklad: Celý Workflow

```bash
# 1. Začínáš úplně od začátku
cd /Users/martinhorak/Projects/core-platform

# 2. Spustíš full clean (40-60 min)
make clean

# 3. Vidíš progress bar:
╔═══════════════════════════════════════════════════════════╗
║  🏗️   MAKE CLEAN - FULL PIPELINE                        ║
╠═══════════════════════════════════════════════════════════╣
║  ✅ 1/6  Cleanup                   [████████] DONE (4s)  ║
║  ✅ 2/6  Pre-build tests           [████████] DONE (3m)  ║
║  ✅ 3/6  Build images              [████████] DONE (8m)  ║
║  ✅ 4/6  Start services            [████████] DONE (2m)  ║
║  ✅ 5/6  E2E pre-deploy            [████████] DONE (6m)  ║
║  ⏳ 6/6  E2E post-deploy           [███░░░░░] 15/30 min  ║
╚═══════════════════════════════════════════════════════════╝

# 4. Na konci:
🎉 CLEAN RESTART COMPLETE - Full stack tested!

# 5. Prostředí je připravené:
✅ https://admin.core-platform.local     (Frontend)
✅ http://localhost:8080/actuator/health (Backend)
✅ http://localhost:8082                 (Keycloak)
✅ http://localhost:3100                 (Grafana)
```

---

## 🐛 Troubleshooting

### Problém: Unit testy selžou v kroku 2

```bash
❌ STEP 2 FAILED: Pre-build tests
```

**Řešení:**
1. Podívej se do `diagnostics/tests/error-summary-*.md`
2. Oprav chyby v kódu
3. Spusť znovu `make clean`

### Problém: Docker build selže

```bash
❌ STEP 3 FAILED: Build images
```

**Řešení:**
1. Zkontroluj Docker má dost místa: `docker system df`
2. Vyčisti cache: `docker system prune -a`
3. Spusť znovu `make clean`

### Problém: E2E testy selžou

```bash
❌ STEP 5 FAILED: E2E pre-deploy
```

**Řešení:**
1. Zkontroluj logy: `make logs`
2. Otevři test report: `make e2e-report`
3. Screenshot v `e2e/test-results/`

---

## 📚 Související Příkazy

```bash
# Full clean + all E2E
make clean

# Fast clean (no E2E)
make clean-fast

# Rebuild with cache (no data delete)
make rebuild

# Only unit tests
make test-backend
make test-frontend

# Only integration tests (need Docker)
make test-backend-full

# Only E2E tests (need running environment)
make test-e2e-pre
make test-e2e-post

# Logs
make logs
make logs-backend
make logs-errors
```

---

## ✅ Shrnutí

`make clean` je **ULTIMATE RESET** - smaže úplně všechno, sestaví od nuly a otestuje celý stack včetně full E2E. Používej ho když potřebuješ:

1. ✅ **Úplný restart** od čistého stavu
2. ✅ **Smazat všechna data** (DB, Kafka, Keycloak)
3. ✅ **Rebuild všech images** od začátku
4. ✅ **Kompletní E2E validaci** (PRE + POST)
5. ✅ **Confidence** že celý stack funguje

**Čas:** 40-60 minut  
**Použití:** CI/CD, release validation, troubleshooting  
**Alternative:** `make clean-fast` (10-25 min, bez E2E)

---

_Last updated: 2025-10-16_
_Commit: 2119655_
