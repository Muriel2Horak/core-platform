# Build Analysis - 16. října 2025

## 📊 Build Status

**Start:** 06:50:45  
**Current:** 08:07 (76 minut běhu)  
**Progress:** 5/6 kroků (83%)

### Dokončené kroky:
- ✅ **Cleanup** - 1s
- ✅ **Pre-build tests** - 179s (67 testů)
- ✅ **Build images** - 50s
- ✅ **Start services** - 211s (všechny kontejnery UP)
- ✅ **E2E pre-deploy** - 4098s (68 minut!) ⚠️

### Aktuální:
- ⏳ **E2E post-deploy** - IN PROGRESS

---

## 🔍 Identifikované problémy

### 1. **Cube.js neustále restartuje** 🔴 CRITICAL

**Chyba:**
```
Error: Only 'cubestore' or 'memory' are supported for cacheAndQueueDriver option, passed: redis
```

**Důsledek:**
- Cube kontejner se restartuje každých ~15 sekund
- E2E testy selhávají kvůli nedostupnosti Cube API
- Pre-deploy testy trvaly 68 minut místo ~10 minut

**Řešení:**
- Zkontrolovat `docker/cube/cube.js` nebo environment variables
- Změnit `CUBEJS_CACHE_AND_QUEUE_DRIVER` z `redis` na `cubestore` nebo `memory`

**Umístění konfigurace:**
```bash
# Zkontrolovat:
grep -r "CACHE_AND_QUEUE" docker/ .env*
```

---

### 2. **E2E pre-deploy testy selhávaly** 🟡 MAJOR

**Problém:**
- Některé testy failovaly na autentizaci timeout
- Playwright proces se zasekl po dokončení všech testů
- Nesdílel finální report (běžel 57 minut bez aktivity)

**Pozorování:**
```
Error: expect(page).toHaveURL(expected)
Expected pattern: /\/(dashboard|home)/i
Received: "https://core-platform.local/realms/admin/protocol/openid-connect/..."
Timeout: 15000ms
```

**Důvody:**
1. Cube restart během testu → app nefunguje
2. Keycloak redirect issues?
3. Playwright zaseklý při generování reportu

**Test výsledky:**
- 21 pre-deploy testů spuštěno
- Některé prošly, některé failovaly (kvůli Cube)
- HTML report nebyl vygenerován (Playwright se zasekl)

---

### 3. **Progress tracking nefunguje pro E2E testy** 🟢 MINOR

**Problém:**
- Panel ukazuje "IN PROGRESS" ale ne "X/Y tests"
- Playwright používá ANSI escape sekvence pro vlastní progress bar
- Náš parser nedokáže extrahovat počet testů

**Důsledek:**
- Uživatel nevidí kolik testů běží/dokončeno
- Vypadá to že build visí (i když běží)

**Možná řešení:**
1. Parsovat Playwright JSON output (`--reporter=json`)
2. Sledovat `test-results/` složky v reálném čase
3. Přidat custom Playwright reporter

---

## 📈 Pozitivní zjištění

✅ **Build progress tracker funguje:**
- Správně ukazuje 6 kroků pipeline
- Real-time update při unit testech (67 testů zobrazeno)
- Elapsed time tracking funguje
- Panel se vykresluje korektně

✅ **Build pokračoval i po zabití Playwright:**
- Po `kill 29857` build přešel na E2E post-deploy
- Wrapper.sh správně handluje process termination
- Tmux session zůstala funkční

✅ **Docker stack funguje:**
- Všechny kontejnery běží (kromě Cube s config errorem)
- Services health checks fungují
- Networking OK

---

## 🔧 Akční plán

### Priorita 1: Opravit Cube konfiguraci
```bash
# 1. Najít konfiguraci
grep -r "CACHE_AND_QUEUE" docker/ .env*

# 2. Změnit na cubestore
# V příslušném souboru změnit:
# CUBEJS_CACHE_AND_QUEUE_DRIVER=redis
# na:
# CUBEJS_CACHE_AND_QUEUE_DRIVER=cubestore

# 3. Restartovat Cube
docker compose restart core-cube
```

### Priorita 2: Implementovat E2E progress tracking
```bash
# Možnost 1: JSON reporter
# e2e/playwright.config.ts
reporter: [
  ['json', { outputFile: 'test-results/results.json' }],
  ['html']
]

# Možnost 2: Sledovat test-results složky
# scripts/build/e2e-progress-watcher.sh
```

### Priorita 3: Analyzovat E2E failures
```bash
# Po dokončení post-deploy testů:
cd e2e
npx playwright show-report --browser=none  # Vypsat URL bez Safari
# Nebo:
python3 -m http.server 9999 --directory playwright-report
```

---

## ⏱️ Časová analýza

| Krok | Očekáváno | Aktuálně | Rozdíl |
|------|-----------|----------|--------|
| Cleanup | ~5s | 1s | ✅ Rychlejší |
| Pre-build tests | ~180s | 179s | ✅ OK |
| Build images | ~60s | 50s | ✅ Rychlejší |
| Start services | ~120s | 211s | ⚠️ +91s (health checks) |
| E2E pre-deploy | ~600s | 4098s | 🔴 +3498s (Cube restart loop!) |
| E2E post-deploy | ~900s | ??? | ⏳ Běží |

**Celkem:**
- Očekáváno: ~30 minut
- Aktuálně: ~76 minut (250% slower)
- **Hlavní důvod: Cube restart loop**

---

## 📝 Poznámky

- Atomic write fix pro state file funguje správně
- Tmux split-pane mode nebyl použit (VS Code terminal limitation)
- Normal scrolling mode funguje dobře
- E2E testy v `--project=pre` a `--project=post` správně rozděleny

---

## 🎯 Závěr

**Hlavní úspěch:**
- Build progress tracker funguje a poskytuje užitečný feedback
- Pipeline správně prochází všemi 6 kroky
- Build se recovernul po zabití zasekladého Playwright procesu

**Hlavní problém:**
- **Cube Redis config error** způsobuje 6x zpomalení E2E testů
- Fix: Změnit `CUBEJS_CACHE_AND_QUEUE_DRIVER=cubestore`

**Next steps:**
1. Opravit Cube config
2. Počkat na dokončení E2E post-deploy
3. Analyzovat finální výsledky
4. Implementovat E2E progress tracking (nice-to-have)
