# Testing FAQ - Core Platform

## ❓ Časté otázky o testech

### 1. 🗂️ Kde jsou všechny testy v repository?

```
core-platform/
├── backend/src/test/          # ✅ Backend unit tests (JUnit 5)
│   └── java/com/example/      # Maven: ./mvnw test
│
├── frontend/src/               # ✅ Frontend unit tests (Vitest)
│   ├── components/*.test.tsx  # npm test
│   └── utils/*.test.ts
│
├── e2e/                       # ✅ E2E tests (Playwright) - NOVÉ
│   ├── specs/pre/             # PRE-DEPLOY smoke tests
│   └── specs/post/            # POST-DEPLOY full E2E
│
└── tests/                     # ⚠️ Legacy integration tests
    ├── multitenancy_smoke.sh  # Bash API tests
    ├── streaming_integration_test.sh
    └── test_tenant_api.sh
```

**Doporučení**: Nové E2E testy psát do `e2e/`, legacy testy postupně migrovat.

---

### 2. 🌐 Jaká je správná URL pro POST-DEPLOY testy?

**Krátká odpověď**: Záleží na prostředí, které testujete.

#### Local Development (výchozí)
```bash
# Testy na lokálně vybuildovaném prostředí
make ci-post-deploy

# Použije: POST_BASE_URL=https://core-platform.local (default)
```

#### Staging/Production
```bash
# Testy na nasazeném staging prostředí
POST_BASE_URL=https://staging.your-domain.com make ci-post-deploy

# Nebo v CI/CD:
POST_BASE_URL=https://prod.your-domain.com make ci-post-deploy
```

#### Konfigurace

**V `.env` nebo `e2e/.env`:**
```bash
# Local (výchozí):
POST_BASE_URL=https://core-platform.local

# Staging:
POST_BASE_URL=https://staging.your-domain.com

# Production:
POST_BASE_URL=https://prod.your-domain.com
```

**V GitHub Actions:**
```yaml
- name: Post-Deploy E2E
  run: POST_BASE_URL=${{ secrets.STAGING_URL }} make ci-post-deploy
```

---

### 3. 🧹 Spustí se E2E testy při `make clean`?

**Krátká odpověď**: NE automaticky, ale ANO pokud nastavíte `RUN_E2E_PRE=true`.

#### Co dělá `make clean`?

```bash
make clean
# 1. Smaže všechny images + volumes
# 2. Zavolá make rebuild
```

#### Co dělá `make rebuild`?

```bash
make rebuild
# 1. Step 1/4: Unit tests (backend + frontend)
# 2. Step 2/4: Build Docker images
# 3. Step 3/4: Start services
# 4. Step 4/4: E2E PRE tests (pouze pokud RUN_E2E_PRE=true)
```

#### Jak spustit clean s E2E testy?

```bash
# Clean BEZ E2E testů (výchozí):
make clean

# Clean S E2E testy:
RUN_E2E_PRE=true make clean
```

#### Kdy se E2E testy spustí automaticky?

**Automaticky (v CI/CD):**
- ✅ GitHub Actions: `.github/workflows/pre-deploy.yml` (při push/PR)
- ✅ GitHub Actions: `.github/workflows/post-deploy.yml` (po deploymentu)
- ✅ `make ci-test-pipeline` (plný CI pipeline)
- ✅ `make ci-post-deploy` (post-deployment validace)

**Manuálně:**
- `make test-e2e-pre` - PRE-DEPLOY smoke
- `make test-e2e-post` - POST-DEPLOY full
- `make test-e2e` - Všechny E2E
- `RUN_E2E_PRE=true make rebuild` - Rebuild s E2E gate

---

## 🎯 Doporučené workflow

### Local Development
```bash
# 1. Vývoj features
make dev-up

# 2. Unit tests (fast feedback)
make test-all

# 3. E2E smoke (před commitem)
make test-e2e-pre

# 4. Commit & Push
git commit -m "feat: new feature"
git push  # Spustí CI s E2E gate
```

### Before Merge/Deploy
```bash
# Comprehensive test suite
make test-comprehensive  # unit + integration + E2E PRE
```

### After Deploy to Staging/Prod
```bash
# Full E2E validation na nasazeném prostředí
POST_BASE_URL=https://staging.your-domain.com make ci-post-deploy
```

---

## 🔄 Flow Diagram

```
┌─────────────────────┐
│   make clean        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Down + Remove All   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   make rebuild      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ Step 1/4: Unit Tests                │
│  ├─ Backend (JUnit)                 │
│  └─ Frontend (Vitest)               │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ Step 2/4: Build Images              │
│  ├─ Backend (Spring Boot)           │
│  ├─ Frontend (React + Vite)         │
│  └─ Keycloak (custom theme)         │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ Step 3/4: Start Services            │
│  ├─ Docker Compose up               │
│  ├─ Wait for health checks          │
│  └─ Post-deployment checks          │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ Step 4/4: E2E PRE Tests (optional)  │
│                                     │
│ if RUN_E2E_PRE=true:                │
│  ├─ Login smoke                     │
│  ├─ Menu RBAC                       │
│  ├─ Entity CRUD                     │
│  └─ Workflow panel                  │
│                                     │
│ else:                               │
│  └─ Skipped                         │
└─────────────────────────────────────┘
```

---

## 📊 Test Command Reference

### Clean & Rebuild
| Command | Unit Tests | E2E Tests | When |
|---------|-----------|-----------|------|
| `make clean` | ✅ Yes | ❌ No | Full rebuild |
| `RUN_E2E_PRE=true make clean` | ✅ Yes | ✅ Yes | Strict validation |
| `make rebuild` | ✅ Yes | ❌ No | After changes |
| `RUN_E2E_PRE=true make rebuild` | ✅ Yes | ✅ Yes | Before deploy |

### Testing
| Command | What | Duration | When |
|---------|------|----------|------|
| `make test-all` | Unit tests only | 2-5 min | During dev |
| `make test-e2e-pre` | PRE smoke tests | 5-7 min | Before commit |
| `make test-e2e-post` | POST full E2E | 20-30 min | After deploy |
| `make test-e2e` | All E2E | 25-35 min | Manual validation |
| `make test-comprehensive` | Unit + E2E PRE | 7-12 min | Before merge |

### CI/CD
| Command | What | When |
|---------|------|------|
| `make ci-test-pipeline` | Unit + E2E PRE gate | GitHub Actions (push/PR) |
| `make ci-post-deploy` | POST validation | After deploy workflow |

---

## 🛠️ Environment Variables

### E2E Test URLs
```bash
# PRE-DEPLOY (local vždy)
PRE_BASE_URL=https://core-platform.local

# POST-DEPLOY (podle prostředí)
POST_BASE_URL=https://core-platform.local           # local
POST_BASE_URL=https://staging.your-domain.com       # staging
POST_BASE_URL=https://prod.your-domain.com          # production
```

### E2E Test Credentials
```bash
# Regular user (výchozí: test/Test.1234)
E2E_USER=test
E2E_PASS=Test.1234

# Admin user (výchozí: test_admin/Test.1234)
E2E_ADMIN_USER=test_admin
E2E_ADMIN_PASS=Test.1234
```

### Control Flags
```bash
# Skip tests in rebuild (not recommended)
SKIP_TESTS=true make rebuild

# Enable E2E gate in rebuild
RUN_E2E_PRE=true make rebuild

# Ignore TLS errors (local development)
E2E_IGNORE_TLS=true
```

---

## ✅ Best Practices

### 1. Local Development
```bash
# Start dev environment
make dev-up

# Run fast unit tests frequently
make test-all

# Run E2E smoke before pushing
make test-e2e-pre
```

### 2. Before Merge
```bash
# Comprehensive validation
make test-comprehensive

# Or full suite if critical changes
make test-e2e
```

### 3. After Deploy
```bash
# Validate staging
POST_BASE_URL=https://staging.your-domain.com make ci-post-deploy

# Validate production
POST_BASE_URL=https://prod.your-domain.com make ci-post-deploy
```

### 4. Clean Rebuild
```bash
# Normal clean (without E2E)
make clean

# Strict clean (with E2E gate)
RUN_E2E_PRE=true make clean
```

---

## 🔗 Further Reading

- [Testing Structure](./TESTING_STRUCTURE.md) - Přehled všech testů v repo
- [E2E Makefile Integration](./E2E_MAKEFILE_INTEGRATION.md) - Detailní E2E dokumentace
- [E2E README](./e2e/README.md) - Playwright setup a usage
- [Legacy Tests](./tests/README_tests.txt) - Staré bash integration tests

---

## 💡 Quick Answers

**Q: Kde jsou testy?**  
A: `backend/src/test/`, `frontend/src/**/*.test.tsx`, `e2e/`, `tests/` (legacy)

**Q: Jaká URL pro POST testy?**  
A: Local: `https://core-platform.local`, Staging: `https://staging.your-domain.com`

**Q: Spustí se E2E při clean?**  
A: Ne automaticky. Ano s `RUN_E2E_PRE=true make clean`

**Q: Kdy se E2E spustí automaticky?**  
A: V GitHub Actions při push/PR a po deploymentu

**Q: Jak přeskočit testy?**  
A: `SKIP_TESTS=true make rebuild` (nedoporučujeme!)
