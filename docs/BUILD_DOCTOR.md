# Build Doctor & Auto-Triage

Automatický diagnostický systém pro Build Doctor & Auto-Triage lokálního PROD-like prostředí.

## 🎯 Co to dělá?

Build Doctor automaticky:
- **Zachytává build chyby** s kontextem
- **Měří čas** buildů
- **Loguje** vše do `diagnostics/build-*.log`
- **Analyzuje** chyby pomocí heuristik
- **Navrhuje konkrétní opravy** s přesnými kroky
- **Detekuje crashloopy** v reálném čase
- **Integruje s Loki** pro pokročilou analýzu logů
- **Automaticky ověřuje** prostředí po deployu (smoke testy)

## 🚀 Rychlý start

```bash
# Normální použití (s Build Doctorem + automatická verifikace)
make up        # Start s diagnostikou + post-deployment checks
make rebuild   # Rebuild s diagnostikou + verifikací
make clean     # Clean s diagnostikou

# Sledování crashloopů
make watch

# Manuální verifikace
make verify       # Rychlé smoke testy (health checks)
make verify-full  # Plné integration testy
```

## 📁 Kde najít reporty

- **Logy**: `diagnostics/build-YYYYMMDD-HHMMSS.log`
- **JSON reporty**: `diagnostics/build-report-YYYYMMDD-HHMMSS.json`
- **Crash dumps**: `.tmp/crash-<container>-YYYYMMDD-HHMMSS.json`

## 🧪 Post-Deployment Checks

Po úspěšném `make up` nebo `make rebuild` se **automaticky** spustí sada smoke testů, které ověří:

### 1. Container Health
- ✅ Všechny kontejnery běží
- ✅ Žádný kontejner se nerestartuje
- ✅ Backend health endpoint
- ✅ Keycloak health endpoint
- ✅ Database connectivity

### 2. API Endpoints
- ✅ API root accessible
- ✅ Swagger UI dostupný
- ✅ Actuator endpoints (info, metrics)

### 3. Frontend
- ✅ Frontend přístupný přes HTTPS
- ✅ Admin frontend přístupný

### 4. Observability Stack
- ✅ Grafana health
- ✅ Loki ready
- ✅ Prometheus healthy

### 5. Keycloak
- ✅ Realm existuje
- ✅ Admin console přístupný

### 6. Volitelné: Plné testy
Při `make verify-full` nebo `RUN_FULL_TESTS=true`:
- ✅ Multitenancy smoke tests
- ✅ Streaming integration tests

Pokud některý test selže, Build Doctor vypíše konkrétní chybu a návod na troubleshooting.

## 🔬 Jak funguje triage

### 1. Wrapper (`scripts/build/wrapper.sh`)
- Obaluje `make` příkazy
- Zachycuje chyby (EXIT/ERR trap)
- Měří čas
- Volá triage při chybě

### 2. Triage (`scripts/build/triage.sh`)
Analyzuje:
- Docker Compose status
- Container health a restart counts
- Logy (Docker nebo Loki)
- Známé error patterns

### 3. Heuristiky (Error Patterns)

| Pattern | Detekce | Fix |
|---------|---------|-----|
| Port conflict | `bind: address already in use` | Zabít proces nebo změnit port |
| Keycloak host | `redirect uri mismatch` | Opravit `KEYCLOAK_FRONTEND_URL` |
| DB migration | `relation already exists` | `make db-clean-migrate` |
| npm/pnpm | `lockfile mismatch` | `pnpm store prune` |
| Maven | `BUILD FAILURE` | `mvn -U clean install` |
| OOM | `OOMKilled=true` | Zvýšit `mem_limit` |
| Disk space | `no space left` | `docker system prune -f` |

## 🔗 Loki integrace (volitelné)

### Setup

```bash
# 1. Zkopíruj example
cp .env.local.example .env.local

# 2. Nastavení v .env.local
LOKI_URL=http://localhost:3100
LOKI_TENANT=core-platform
LOKI_LABEL_SELECTOR={compose_project="core-platform"}
```

### Použití

Build Doctor automaticky použije Loki, pokud:
- Je nastaveno `LOKI_URL`
- Docker logy nejsou dostupné
- Kontejner má problémy

## 📊 JSON Report formát

```json
{
  "buildTs": "2025-01-10T12:34:56Z",
  "status": "FAILED",
  "errors": [...],
  "containers": [
    {
      "name": "backend",
      "state": "restarting",
      "restartCount": 5,
      "exitCode": 1,
      "oomKilled": false,
      "health": "unhealthy"
    }
  ],
  "suspectedCauses": [
    "Port conflict detected on :8080 for backend"
  ],
  "recommendedFixes": [
    {
      "title": "Fix port conflict for backend",
      "steps": [
        "Check docker-compose.yml ports for backend",
        "Kill process using :8080: lsof -ti:8080 | xargs kill -9",
        "Or change port in docker-compose.yml"
      ],
      "runnable": false
    }
  ],
  "artifacts": {
    "logFile": "diagnostics/build-20250110-123456.log"
  }
}
```

## 🤖 Copilot integrace

Build Doctor automaticky vypisuje JSON report v bloku pro GitHub Copilot:

```
##[COPILOT_START_JSON]
{ ... report ... }
##[COPILOT_END_JSON]

COPILOT_HINT: Port conflict detected on :8080 for backend
COPILOT_HINT: Check diagnostics/build-report.json for 3 recommended fix(es)
```

Copilot pak může:
- Analyzovat chyby
- Navrhnout konkrétní diff
- Automaticky opravit config-only problémy

## 👁️ Crashloop watcher

Sleduje kontejnery v reálném čase:

```bash
make watch
```

Při detekci crashloopu:
- Uloží diagnostiku do `.tmp/crash-*.json`
- Vytiskne COPILOT_HINT s cestou k souboru
- Zobrazí Exit Code, OOMKilled status, Health

## 🏗️ BuildKit optimalizace

Build Doctor automaticky používá:
- **DOCKER_BUILDKIT=1** - rychlejší buildy
- **--parallel** - paralelní build services
- **Cache mounts**:
  - Maven: `/root/.m2`
  - npm: `/root/.npm`

## 🔧 Ruční použití scriptů

```bash
# Triage (jen analýza)
./scripts/build/triage.sh diagnostics/build.log diagnostics/report.json

# Health check (jen čekání)
./scripts/build/wait-healthy.sh --timeout 180

# Crashloop watcher (standalone)
./scripts/build/watch-crashloop.sh

# Loki query
./scripts/build/loki.sh query backend 10  # posledních 10min
```

## ⚠️ Safety

Build Doctor **NIKDY SÁM NEOPRAVÍ** kód nebo config. Pouze:
- ✅ Navrhuje opravy s konkrétními kroky
- ✅ Vypisuje COPILOT_HINT pro asistenci
- ✅ Generuje JSON reporty
- ❌ **NEMAZÁVÁ** volumes mimo `make clean`
- ❌ **NEMĚNÍ** certs ani proxy

## 🐛 Troubleshooting

### Build Doctor nefunguje

```bash
# Zkontroluj závislosti
which jq       # triage potřebuje jq
which docker
which curl     # pro Loki

# Zkontroluj permissions
ls -la scripts/build/*.sh  # všechny musí být executable
```

### Post-deployment checks selhávají

```bash
# Zkontroluj konkrétní service
docker ps --filter "name=core-platform"
docker logs <container-name> --tail=100

# Spusť jednotlivé checks manuálně
curl -sf http://localhost:8080/actuator/health | jq '.'
curl -sf http://localhost:8081/health | jq '.'
curl -sfk https://core-platform.local/ -o /dev/null

# Zkontroluj DNS/hosts
ping core-platform.local
cat /etc/hosts | grep core-platform

# Zkontroluj porty
lsof -i :8080  # Backend
lsof -i :8081  # Keycloak
lsof -i :3100  # Loki
```

### Přeskočit automatickou verifikaci

Pokud chcete spustit prostředí bez automatických testů:

```bash
# Použít dev-up místo up (žádné wrapper/triage)
make dev-up

# Nebo zavolat _up_inner přímo (NEDOPORUČENO)
make _up_inner
```

### Loki nefunguje

```bash
# Test Loki
curl -s "$LOKI_URL/ready"

# Test query
./scripts/build/loki.sh query all 5
```

### Triage nic nenajde

```bash
# Ruční kontrola
docker ps --filter "name=core-platform"
docker inspect <container-name>
docker logs <container-name> --tail=100
```

## 📚 Související

- [TESTING.md](./TESTING.md) - E2E testy
- [REPORTING_README.md](./REPORTING_README.md) - Reporting modul
- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) - API docs

## 🎯 Definition of Done

- ✅ `make up/rebuild/clean` běží přes wrapper s logováním
- ✅ Při chybě vznikne JSON report + COPILOT_HINT
- ✅ `make watch` detekuje crashloopy
- ✅ Build je rychlejší (BuildKit, cache)
- ✅ Žádné zásahy do stávající proxy/certů
- ✅ Trunk-based: malé commity přímo do main
