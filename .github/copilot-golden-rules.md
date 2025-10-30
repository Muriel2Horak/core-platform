# GitHub Copilot - Golden Rules (Build & Template System)

## 🎯 ABSOLUTNÍ PRAVIDLA - VŽDY DODRŽUJ

### 1. **TEMPLATE SYSTÉM - NIKDY NEEDIUJ GENEROVANÉ SOUBORY**

#### ❌ NIKDY NEMĚNIT PŘÍMO:
```
.env                          → VŽDY edituj .env.template
docker-compose.yml           → VŽDY edituj docker-compose.template.yml  
realm-admin.json            → VŽDY edituj realm-admin.template.json
jakýkoli *.json v docker/   → VŽDY edituj odpovídající *.template.json
```

#### ✅ SPRÁVNÝ WORKFLOW:
```bash
# 1. EDITUJ TEMPLATE
vim .env.template
vim docker-compose.template.yml
vim docker/keycloak/realm-admin.template.json

# 2. VYGENERUJ (automatické v Makefile targets)
# Generování je AUTOMATICKÉ při build/up/clean targets!
# Není potřeba manuálně volat, ale můžeš:
bash docker/keycloak/generate-realm.sh  # Pro realm soubory

# 3. APLIKUJ ZMĚNY
make clean-fast   # Restart s novými konfiguracemi
```

### 2. **BUILD WORKFLOW - REBUILDY PO ZMĚNÁCH**

#### Java/Backend změny (.java, pom.xml):
```bash
# Po JAKÉKOLI změně backendu je NUTNÝ rebuild
make clean-fast        # ✅ DOPORUČENO (bez E2E, rychlé)
make rebuild-backend   # ✅ Rebuild pouze backend
make rebuild          # ✅ Full rebuild s cache
make clean            # ⚠️ Full rebuild + E2E (slow, pro CI)
```

#### TypeScript/Frontend změny (.ts, .tsx):
```bash
# Hot reload funguje automaticky (změny v src/)
# Rebuild POUZE při:
make rebuild-frontend  # Změna dependencies (package.json)
make rebuild-frontend  # Změna build configu (vite.config.ts)
```

#### Keycloak změny (realm templates):
```bash
# Po změně realm-admin.template.json nebo environment proměnných
vim docker/keycloak/realm-admin.template.json
make rebuild-keycloak  # Rebuild Keycloak image + realm import
```

### 3. **LOGY POUZE Z LOKI - NIKDY docker logs**

#### ❌ ZAKÁZÁNO:
```bash
docker logs core-backend     # NEČITELNÉ (JSON)
docker logs core-frontend    # CHYBÍ CONTEXT
docker-compose logs          # NECENTRÁLNÍ
```

#### ✅ POVINNÉ:
```bash
make logs              # Všechny logy ze všech služeb
make logs-backend      # Backend logy (strukturované)
make logs-frontend     # Frontend + nginx logy
make logs-keycloak     # Keycloak logy
make logs-errors       # POUZE ERROR logy (rychlé)
```

**Proč?** 
- Loki agreguje logy z více kontejnerů
- Filtruje JSON strukturu do čitelné formy
- Podporuje časové rozsahy a label queries
- Centrální místo pro všechny logy

### 4. **KEYCLOAK BUILD PROCES**

#### Build flow (automatický v `make up/clean/rebuild`):
```bash
# 1. Vygeneruj realm config z template (AUTOMATICKY!)
# → Volá se VŽDY při 'make kc-image' nebo 'make kc-image-no-cache'
bash docker/keycloak/generate-realm.sh
# → Čte: docker/keycloak/realm-admin.template.json
# → Generuje: docker/keycloak/realm-admin.json
# → Substituuje: $DOMAIN, $KEYCLOAK_ADMIN_CLIENT_SECRET, atd.

# 2. Build Keycloak Docker image
docker build -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .
# → Zkopíruje VYGENEROVANÝ realm-admin.json do /opt/keycloak/data/import/
# → Zkopíruje SSL certifikáty
# → Zbuildí Keycloak s PostgreSQL podporou

# 3. Keycloak container start
# → Importuje realmy z /opt/keycloak/data/import/
# → Flag: --import-realm --spi-import-if-exists=skip
```

#### ⚡ DŮLEŽITÉ - Automatické generování:
- **Makefile targets `kc-image` a `kc-image-no-cache` VŽDY volají `generate-realm.sh` před buildem**
- **NENÍ potřeba** manuálně volat `generate-realm.sh` - děje se automaticky
- **Změny v template** se projeví při příštím `make clean-fast` nebo `make rebuild-keycloak`

#### Důležité soubory:
```
docker/keycloak/
├── Dockerfile                      # Build Keycloak image
├── realm-admin.template.json       # ✅ EDITUJ TOTO
├── realm-admin.json               # ❌ GENEROVANÉ (neměnit)
└── generate-realm.sh              # Generátor

Dockerfile kopíruje:
- realm-admin.json → /opt/keycloak/data/import/
- ssl/* → /opt/keycloak/conf/
- themes/core-material/ → /opt/keycloak/themes/
```

### 5. **MAKEFILE TARGETS - CO DĚLÁ CO**

#### Production workflow:
```bash
make up              # Start environment (build pokud nutné)
make down            # Stop all services
make restart         # Restart bez rebuildu
make rebuild         # Rebuild s cache + unit tests
make rebuild-clean   # Rebuild BEZ cache (slow)
make clean           # ☢️ NUCLEAR: Rebuild + FULL E2E (~30-40 min)
make clean-fast      # 🚀 DEV: Rebuild BEZ E2E (~5-10 min)
```

#### Targeted rebuilds:
```bash
make rebuild-backend     # Backend only
make rebuild-frontend    # Frontend only
make rebuild-keycloak    # Keycloak only (+ realm regenerate)
make restart-backend     # Restart bez rebuildu
make restart-frontend    # Restart bez rebuildu
```

#### Testing:
```bash
make test-backend          # Unit tests (2-5 min)
make test-backend-full     # Unit + Integration (10-15 min)
make test-frontend         # Frontend tests
make test-e2e-pre          # Pre-deploy smoke (5-7 min)
make test-e2e-post         # Post-deploy full (20-30 min)
make test-e2e              # Pre + Post E2E
make verify                # Quick health checks
```

#### Build proces internals:
```bash
make build               # Build all images
make kc-image            # Build Keycloak image (volá generate-realm.sh)
make validate-env        # Check .env existence
make wait-for-services   # Health check waiting
```

### 6. **ENVIRONMENT VARIABLES - SUBSTITUCE V TEMPLATES**

#### Template syntax (používá `envsubst`):
```json
{
  "redirectUris": ["https://${DOMAIN}/*"],
  "secret": "${KEYCLOAK_ADMIN_CLIENT_SECRET}",
  "password": "${TEST_USER_PASSWORD}"
}
```

#### Fallback syntax (convert před envsubst):
```json
{
  "password": "${TEST_USER_PASSWORD:-Test.1234}"
}
```
→ `generate-realm.sh` konvertuje na `${TEST_USER_PASSWORD}` před envsubst

#### Kde se berou hodnoty:
```bash
1. .env soubor (root projektu)
2. docker-compose.yml environment sekce
3. Fallback hodnoty v .env.template
```

### 7. **DEBUGGING WORKFLOW**

```bash
# 1. Zjisti co se děje
make logs-errors          # Rychlý ERROR scan

# 2. Konkrétní služba
make logs-backend         # Backend detailní logy
make logs-keycloak        # Keycloak import/startup

# 3. Health check
make verify               # Quick smoke test
curl -k https://localhost/api/actuator/health

# 4. Pokud nefunguje
make clean-fast           # Force rebuild + restart

# 5. Nuclear option
make down
make docker-cleanup       # Clean všechno
make clean-fast           # Rebuild from scratch
```

### 8. **COMMON SCENARIOS - KDYŽ...**

#### Změnil jsi backend controller:
```bash
vim backend/src/.../MyController.java
make clean-fast        # NUTNÝ REBUILD (Java změny nejsou hot-reload)
make logs-backend      # Verify startup
make verify            # Health check
```

#### Změnil jsi frontend komponentu:
```bash
vim frontend/src/components/MyComponent.tsx
# HOT RELOAD automaticky! ✅
# Rebuild NENÍ nutný (vite dev server)
```

#### Přidal jsi environment variable:
```bash
vim .env.template      # EDITUJ TEMPLATE (ne .env!)
# Přidej: NEW_VAR=hodnota

# Použij v docker-compose.template.yml
vim docker-compose.template.yml
# environment:
#   - NEW_VAR=${NEW_VAR}

# Rebuild vygeneruje nový .env a docker-compose.yml
make clean-fast
```

#### Změnil jsi Keycloak realm config:
```bash
vim docker/keycloak/realm-admin.template.json
# Změna redirect URIs, clientů, atd.

make rebuild-keycloak  # Rebuild KC image + reimport realm
make logs-keycloak     # Verify import
```

#### Debug Grafana SSO:
```bash
make logs-backend | grep -i grafana        # Backend auth logs
make logs | grep -i "auth request"         # Nginx auth errors
docker exec core-grafana cat /etc/grafana/grafana.ini | grep oauth
```

### 9. **ANTI-PATTERNS - NIKDY NEDĚLAT**

| ❌ ŠPATNĚ | ✅ SPRÁVNĚ | DŮVOD |
|-----------|------------|-------|
| `vim .env` | `vim .env.template && make clean-fast` | .env je generovaný |
| `docker restart core-backend` | `make clean-fast` | Neaplikuje změny kódu |
| `docker logs core-backend` | `make logs-backend` | Loki centralizuje + filtruje |
| `docker-compose up` | `make up` | Makefile má pre-hooks |
| Rebuild frontendu po .tsx změně | Čekej hot reload | Zbytečné, dev server ho dělá |
| Zapomenout rebuild po .java změně | `make clean-fast` VŽDY | Java není hot-reload |
| Editovat realm-admin.json | Edituj realm-admin.template.json | Přepíše se při buildu |

### 10. **BUILD DOCTOR - DIAGNOSTIKA**

```bash
make doctor            # Build diagnostics report
make verify            # Quick health checks  
make verify-full       # Full integration tests
```

Vygeneruje:
- `diagnostics/build-TIMESTAMP.log` - Full build log
- `diagnostics/build-report-TIMESTAMP.json` - JSON report
- Health check results
- Container status

### 11. **KDY CO POUŽÍVAT**

#### Development (běžný den):
```bash
make dev-up            # Start dev environment (DEPRECATED - nepoužívat)
make clean-fast        # ✅ POUŽÍVAT: Rebuild bez E2E
make logs-backend      # Watch logy při vývoji
make rebuild-backend   # Po změně .java souboru
# Frontend hot reload automaticky ✅
```

#### Pre-commit:
```bash
make test-backend      # Unit tests (fast)
make test-frontend     # Frontend tests
make verify            # Quick smoke test
```

#### CI/CD:
```bash
make clean             # Full rebuild + E2E
make test-backend-full # All backend tests
make test-e2e          # All E2E tests
```

## 📋 QUICK REFERENCE

### Template Files Map:
```
.env.template                                → .env (auto při build)
docker-compose.template.yml                  → docker-compose.yml (auto při build)
docker/keycloak/realm-admin.template.json    → realm-admin.json (generate-realm.sh)
```

### Build Triggers (kdy se co builduje):
```
make up           → kc-image + docker compose build (if needed)
make clean-fast   → kc-image + docker compose build --no-cache
make rebuild      → kc-image + docker compose build
make kc-image     → docker build keycloak + generate-realm.sh
```

### Log Commands Quick:
```
make logs                    # All logs (last 10 min)
make logs-errors             # Only ERRORs (last 30 min)
make logs-backend            # Backend (last 10 min)
make logs SERVICE=grafana    # Custom service
```

### Emergency Recovery:
```bash
# Level 1: Soft restart
make down && make up

# Level 2: Force rebuild
make clean-fast

# Level 3: Nuclear cleanup
make down
make docker-cleanup
docker volume prune -f
make clean-fast
```

## 🎓 WHEN IN DOUBT

1. **Změnil jsi kód?** → `make clean-fast`
2. **Změnil jsi .env?** → `vim .env.template && make clean-fast`
3. **Potřebuješ logy?** → `make logs-errors` nebo `make logs-backend`
4. **Nefunguje?** → `make doctor` + `make logs-errors`
5. **Template changes?** → Edit `.template` file, rebuild aplikuje změny
6. **Keycloak realm?** → Edit `realm-admin.template.json` + `make rebuild-keycloak`

---

**Last updated:** 2025-10-26
**Project:** core-platform
**Maintainer:** Martin Horak (@Muriel2Horak)
