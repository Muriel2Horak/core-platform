# Makefile E2E Testing Integration

**Datum:** 15. října 2025  
**Účel:** Integrace kompletní E2E testovací pipeline do `make clean`

---

## 🎯 Změny

### 1. **Nový target: `make clean` (upraveno)**
```bash
make clean
```

**Chování:**
- ✅ Odstraní všechna data (volumes, containers, images)
- ✅ Spustí **FULL E2E TESTING PIPELINE**:
  1. **PRE-BUILD:** Unit testy (backend + frontend)
  2. **BUILD:** Docker image build
  3. **POST-DEPLOY:** Smoke tests (health checks, API)
  4. **E2E PRE:** Smoke E2E testy (5-7 min)
  5. **E2E POST:** Full E2E scénáře s ephemeral daty (20-30 min)

**Časová náročnost:** ~35-45 minut (v závislosti na HW)

**Použití:** Production-ready rebuild s kompletní validací

---

### 2. **Nový target: `make clean-fast` (nový)**
```bash
make clean-fast
```

**Chování:**
- ✅ Odstraní všechna data (volumes, containers, images)
- ✅ Spustí **ZÁKLADNÍ TESTOVÁNÍ**:
  1. **PRE-BUILD:** Unit testy (backend + frontend)
  2. **BUILD:** Docker image build
  3. **POST-DEPLOY:** Smoke tests (health checks, API)
  4. ⏭️ **E2E PŘESKOČENO** (dev mode)

**Časová náročnost:** ~10-15 minut

**Použití:** Rychlý development rebuild bez E2E testů

---

### 3. **Parametry pro `rebuild` a `rebuild-clean`**

#### 3.1 Základní rebuild (bez E2E)
```bash
make rebuild
# nebo
make rebuild-clean
```
**Pipeline:** PRE-BUILD → BUILD → POST-DEPLOY

---

#### 3.2 Rebuild s PRE-DEPLOY E2E
```bash
make rebuild RUN_E2E_PRE=true
# nebo
make rebuild-clean RUN_E2E_PRE=true
```
**Pipeline:** PRE-BUILD → BUILD → POST-DEPLOY → **E2E PRE** (smoke)

---

#### 3.3 Rebuild s FULL E2E
```bash
make rebuild RUN_E2E_FULL=true
# nebo
make rebuild-clean RUN_E2E_FULL=true
```
**Pipeline:** PRE-BUILD → BUILD → POST-DEPLOY → **E2E PRE** → **E2E POST**

---

## 📋 Testovací Pipeline

### **Kompletní flow (`make clean`):**

```
╔════════════════════════════════════════════════════════════════╗
║  🧹 CLEAN RESTART (REMOVES DATA + FULL E2E TESTING)           ║
╚════════════════════════════════════════════════════════════════╝

▶️  [1/6] PRE-BUILD Unit Tests
    Backend:  ./mvnw test
    Frontend: npm test -- --run
    ✅ Exit on failure

▶️  [2/6] Docker Build (parallel)
    docker compose build --parallel
    ✅ All images built

▶️  [3/6] Start Services
    docker compose up -d
    ⏳ Wait for healthy (180s timeout)
    ✅ POST-DEPLOYMENT smoke tests
       - Container health
       - API endpoints
       - Frontend accessibility

▶️  [4/6] E2E PRE-DEPLOY (Smoke Tests)
    cd e2e && npm run test:pre
    ⏱️  ~5-7 minut
    ✅ Login, RBAC, Grid/Form, Workflow

▶️  [5/6] E2E POST-DEPLOY (Full Scenarios)
    cd e2e && npm run scaffold      # Create test data
    cd e2e && npm run test:post     # Run full E2E
    cd e2e && npm run teardown      # Cleanup
    ⏱️  ~20-30 minut
    ✅ Complete user journeys

▶️  [6/6] All E2E tests completed ✅

🎉 Clean restart with full E2E testing completed!
```

---

## 🔧 Implementační detaily

### Změny v Makefile:

1. **`_clean_inner`** - volá `rebuild RUN_E2E_FULL=true`
2. **`_clean_fast_inner`** - volá `rebuild` (bez E2E)
3. **`_rebuild_inner`** - podporuje `RUN_E2E_PRE` a `RUN_E2E_FULL`
4. **`_rebuild_clean_inner`** - podporuje `RUN_E2E_PRE` a `RUN_E2E_FULL`

### Conditional E2E execution:

```makefile
@if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then
    echo "▶️  [4/6] Running PRE-DEPLOY E2E tests (smoke)...";
    $(MAKE) test-e2e-pre || (echo "❌ PRE-DEPLOY E2E failed!"; exit 1);
    echo "▶️  [5/6] Running POST-DEPLOY E2E tests (full scenarios)...";
    $(MAKE) test-e2e-post || (echo "❌ POST-DEPLOY E2E failed!"; exit 1);
elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then
    echo "▶️  [4/6] Running PRE-DEPLOY E2E tests...";
    $(MAKE) test-e2e-pre || (echo "❌ E2E tests failed!"; exit 1);
else
    echo "⏭️  [4/6] E2E tests skipped";
fi
```

---

## 📊 Doporučené použití

### Development workflow:
```bash
# První setup
make clean-fast              # Rychlý start bez E2E

# Iterace (hot reload)
make dev-up                  # Watch mode s automatickým reload

# Před commitem
make rebuild RUN_E2E_PRE=true  # Quick validation
```

### CI/CD workflow:
```bash
# Pull request validation
make rebuild RUN_E2E_PRE=true  # Fast gate (5-7 min)

# Pre-production validation
make clean                     # Full E2E pipeline (35-45 min)

# Post-deployment
make test-e2e-post            # Verify deployment
```

### Production release:
```bash
# Kompletní validace před release
make clean                     # Full pipeline s všemi testy
```

---

## ✅ Výhody

1. **🔒 Kompletní coverage** - `make clean` zajistí, že vše funguje
2. **⚡ Flexibilita** - Můžeš si vybrat level testování
3. **🚀 Dev-friendly** - `make clean-fast` pro rychlý development
4. **📊 CI/CD ready** - Environment proměnné pro automatizaci
5. **🛡️ Safety** - Testy failují build při jakémkoliv problému

---

## 📚 Související dokumentace

- [E2E_TWO_TIER_COMPLETE.md](E2E_TWO_TIER_COMPLETE.md) - E2E architektura
- [TESTING.md](TESTING.md) - Testovací strategie
- [Makefile](Makefile) - Kompletní build systém

---

## 🎉 Shrnutí

**`make clean`** nyní poskytuje **KOMPLETNÍ validaci** celé platformy:
- ✅ Unit testy (FE + BE)
- ✅ Smoke testy (POST-DEPLOY)
- ✅ E2E PRE (fast smoke)
- ✅ E2E POST (full scenarios)

**Použij:**
- `make clean` - pro production-ready rebuild
- `make clean-fast` - pro development rebuild
- `make rebuild RUN_E2E_PRE=true` - pro quick validation
- `make rebuild RUN_E2E_FULL=true` - pro full validation bez cleanup
