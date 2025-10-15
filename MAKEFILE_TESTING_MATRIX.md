# Makefile Testing Matrix

**Quick Reference:** Který command použít pro jaký účel?

---

## 🎯 Testing Levels

| Level | Tests Included | Duration | Use Case |
|-------|---------------|----------|----------|
| **None** | ❌ No tests | ~5 min | Emergency hotfix |
| **Smoke** | ✅ POST-DEPLOY only | ~8-12 min | Quick validation |
| **Unit** | ✅ PRE-BUILD + POST-DEPLOY | ~10-15 min | Standard dev |
| **E2E PRE** | ✅ Unit + Smoke E2E | ~15-20 min | PR validation |
| **E2E FULL** | ✅ Unit + Smoke E2E + Full E2E | ~35-45 min | Production |

---

## 🔧 Make Commands Matrix

| Command | Data Cleanup | Build Cache | Unit Tests | Smoke Tests | E2E PRE | E2E POST | Total Time |
|---------|--------------|-------------|------------|-------------|---------|----------|------------|
| `make up` | ❌ | ✅ | ❌ | ✅ | ❌ | ❌ | ~3-5 min |
| `make rebuild` | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ | ~10-15 min |
| `make rebuild RUN_E2E_PRE=true` | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ~15-20 min |
| `make rebuild RUN_E2E_FULL=true` | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ~35-45 min |
| `make rebuild-clean` | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ~15-20 min |
| `make clean-fast` | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ~15-20 min |
| `make clean` | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ | ~40-50 min |

**Legenda:**
- ✅ = Included
- ❌ = Not included
- 🔄 = Conditional (via env var)

---

## 📋 Test Breakdown

### 1. **Unit Tests (PRE-BUILD)**
```bash
# Backend
cd backend && ./mvnw test

# Frontend  
cd frontend && npm test -- --run
```
**Duration:** ~5-8 min  
**Coverage:** Business logic, services, utilities

---

### 2. **Smoke Tests (POST-DEPLOY)**
```bash
bash scripts/build/post-deployment-check.sh
```
**Duration:** ~2-3 min  
**Coverage:**
- ✅ Container health
- ✅ API endpoints
- ✅ Frontend accessibility
- ✅ Database connectivity
- ✅ Observability stack

---

### 3. **E2E PRE-DEPLOY (Smoke)**
```bash
cd e2e && npm run test:pre
```
**Duration:** ~5-7 min  
**Coverage:**
- ✅ Login flow
- ✅ RBAC validation
- ✅ Grid/Form rendering
- ✅ Workflow panel
- ✅ Basic CRUD operations

**Test files:**
- `tests/smoke/login.spec.ts`
- `tests/smoke/rbac.spec.ts`
- `tests/smoke/grid-form.spec.ts`
- `tests/smoke/workflow.spec.ts`

---

### 4. **E2E POST-DEPLOY (Full Scenarios)**
```bash
cd e2e && npm run scaffold   # Create ephemeral data
cd e2e && npm run test:post  # Run full E2E
cd e2e && npm run teardown   # Cleanup
```
**Duration:** ~20-30 min  
**Coverage:**
- ✅ Complete user journeys
- ✅ Multi-tenant scenarios
- ✅ Complex workflows
- ✅ Edge cases
- ✅ Performance tests

**Test files:**
- `tests/full/*.spec.ts`
- Uses ephemeral test data (no production pollution)

---

## 🎯 Recommended Workflows

### Development (Hot Reload)
```bash
# First start
make dev-up

# No rebuilds needed - hot reload active ⚡
# Edit code → Auto reload
```
**No testing overhead during development**

---

### Quick Fix
```bash
# Make changes
make rebuild

# Quick validation
make test-e2e-pre
```
**Total:** ~15-20 min

---

### Pull Request
```bash
# Before pushing
make rebuild RUN_E2E_PRE=true

# If successful → push
git push
```
**Total:** ~15-20 min  
**Confidence:** Medium-High

---

### Pre-Production
```bash
# Full validation
make clean

# If successful → deploy to staging
```
**Total:** ~40-50 min  
**Confidence:** Very High

---

### Emergency Hotfix
```bash
# Option 1: Fast (risky)
make rebuild-clean SKIP_TESTS=true
make up

# Option 2: Safer
make clean-fast

# Then manually test critical paths
```
**Total:** ~5-20 min depending on option

---

## 🚦 Exit Codes

| Exit Code | Meaning | Action |
|-----------|---------|--------|
| `0` | ✅ All tests passed | Proceed |
| `1` | ❌ Tests failed | Check logs, fix issues |
| `2` | ⚠️ Build failed | Check Docker logs |
| `3` | ⚠️ Deployment failed | Check health checks |

---

## 📊 CI/CD Integration

### GitHub Actions Example:
```yaml
# .github/workflows/ci.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      # PR validation (fast)
      - name: Build and test
        run: make rebuild RUN_E2E_PRE=true
      
  deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      # Full validation before production
      - name: Full E2E validation
        run: make clean
```

---

## 🔍 Troubleshooting

### Tests failed but I need to deploy NOW
```bash
# EMERGENCY ONLY - bypass tests
make rebuild SKIP_TESTS=true

# Then:
# 1. Deploy
# 2. Monitor closely
# 3. Fix tests ASAP
# 4. Run: make test-e2e-post
```

### E2E tests are too slow
```bash
# Use fast mode for development
make clean-fast

# Run E2E manually when ready
make test-e2e-pre   # Quick smoke
make test-e2e-post  # Full (optional)
```

### Want to run only specific E2E tests
```bash
# Run specific test file
cd e2e
npm run test -- tests/smoke/login.spec.ts

# Run tests matching pattern
npm run test -- --grep "workflow"
```

---

## ✅ Best Practices

1. **Development:** `make dev-up` (hot reload, no rebuilds)
2. **Before commit:** `make rebuild RUN_E2E_PRE=true`
3. **Before PR merge:** CI runs `make rebuild RUN_E2E_PRE=true`
4. **Before production:** `make clean` (full validation)
5. **Emergency:** `make clean-fast` (skip E2E but keep unit tests)

---

## 🎉 Summary

| Scenario | Command | Why |
|----------|---------|-----|
| Daily dev | `make dev-up` | Hot reload, no rebuild overhead |
| Quick check | `make rebuild` | Fast iteration with unit tests |
| PR validation | `make rebuild RUN_E2E_PRE=true` | Smoke E2E for confidence |
| Production release | `make clean` | Full validation, no compromises |
| Fast recovery | `make clean-fast` | Skip E2E but keep safety net |

**Default for `make clean`: FULL E2E TESTING ✅**
