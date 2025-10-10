# 🎯 Test Implementation Completion Plan
**Status: 10. října 2025, 19:45**

## ✅ DOKONČENO (100%)

### Backend - Kompilace a Unit Testy
- [x] Oprava duplicate class v `StreamingGlobalConfigDto.java`
- [x] Přidán explicit Lombok 1.18.36 v pom.xml
- [x] **MonitoringDSLValidator** - kompletní implementace (142 lines)
  - SQL injection patterns (DELETE FROM, UPDATE SET, INSERT INTO)
  - Command injection patterns
  - XSS patterns
  - Query complexity limits (50 pipes, 5000 chars)
  - Brace balance validation
- [x] **MonitoringDSLValidatorTest** - ✅ **25/25 testů PASSED**
- [x] **LogCapture** utility pro testování logů
- [x] Conditional beans pro testy:
  - `@ConditionalOnProperty` na RateLimitFilter
  - `@ConditionalOnProperty` na KeycloakDataSourceConfig
  - `@ConditionalOnProperty` na KeycloakJpaConfig
- [x] `application-test.yml` s H2 test databází
- [x] BUILD SUCCESS ✅

### Frontend - Dependencies
- [x] npm install (292 packages)
- [x] Playwright 1.56.0 installed
- [x] `@types/node` installed

### Git Commits
- [x] Commit 324694d: "fix(tests): Fix compilation errors and add test infrastructure"

---

## ⏳ ZBÝVÁ DOKONČIT

### 1. Backend Integration Tests (Priorita: STŘEDNÍ)

**Problém:** MonitoringMetricsAndLogsIT a MonitoringProxyServiceTest vyžadují plný Spring context

**Řešení A - Testcontainers (DOPORUČENO):**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**Řešení B - Přepsat jako Unit testy:**
- Změnit `@SpringBootTest` → `@WebMvcTest`
- Mockovat všechny dependencies
- Rychlejší běh, ale méně realistické

**Odhadovaný čas:** 30-45 minut

---

### 2. Frontend Build Issues (Priorita: VYSOKÁ)

**Problém:** 51 errors s `@grafana/schema` dependencies
```
ERROR: Could not resolve "@grafana/schema/dist/esm/raw/composable/..."
```

**Možné příčiny:**
1. Nekompatibilní verze `@grafana/scenes` vs `@grafana/schema`
2. Chybějící peer dependencies
3. esbuild resolver issue

**Debugging kroky:**
```bash
# 1. Zkontrolovat verze
npm list @grafana/scenes @grafana/schema

# 2. Zkusit reinstall
rm -rf node_modules package-lock.json
npm install

# 3. Případně downgrade Grafana packages
npm install @grafana/scenes@^X.Y.Z
```

**Odhadovaný čas:** 15-30 minut

---

### 3. E2E Testy - Playwright Setup (Priorita: STŘEDNÍ)

**Zbývá:**
- [ ] Doinstalovat Playwright browsers (Firefox, WebKit)
- [ ] Vytvořit první E2E test (např. login flow)
- [ ] Nastavit CI/CD pipeline pro E2E testy

**Příklad prvního testu:**
```typescript
// tests/e2e/login.spec.ts
import { test, expect } from '@playwright/test';

test('should display login page', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('h1')).toContainText('Core Platform');
});
```

**Odhadovaný čas:** 20-30 minut

---

### 4. Dokumentace (Priorita: NÍZKÁ)

- [ ] Aktualizovat TEST_IMPLEMENTATION_SUMMARY.md
- [ ] Přidat README.md do tests/ složky
- [ ] Dokumentovat test patterns a best practices

**Odhadovaný čas:** 15 minut

---

## 📋 DOPORUČENÉ POŘADÍ

### Fáze 1: Critical Path (45-60 min)
1. **Fix Frontend Build** (15-30 min)
   - Debugging Grafana dependencies
   - npm install + verify build works
   
2. **Backend Integration Tests** (30-45 min)
   - Přidat Testcontainers
   - NEBO přepsat na @WebMvcTest
   - Verify all tests pass

### Fáze 2: E2E Foundation (30-45 min)
3. **Playwright E2E Setup** (20-30 min)
   - Install browsers
   - Create first smoke test
   
4. **Dokumentace** (15 min)
   - Update summaries
   - Document patterns

---

## 🚀 QUICK WINS (Pro immediate progress)

### Win #1: Commit Current Progress (2 min)
```bash
git add -A
git commit -m "fix(tests): Manual code formatting and @types/node

- Reformat MonitoringDSLValidator, LogCapture
- Add @types/node for Playwright config
- All MonitoringDSLValidatorTest passing (25/25)"
```

### Win #2: Skip Integration Tests For Now (5 min)
```bash
# Run all unit tests only
./mvnw test -Dtest='!**/*IT'

# OR exclude specific tests
./mvnw test -Dtest='!MonitoringMetricsAndLogsIT,!MonitoringProxyServiceTest'
```

### Win #3: Frontend Quick Fix Attempt (10 min)
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

---

## 📊 PROGRESS METRICS

| Kategorie | Hotovo | Zbývá | % |
|-----------|--------|-------|---|
| Backend Compilation | ✅ | - | 100% |
| Backend Unit Tests | ✅ 25/25 | - | 100% |
| Backend Integration Tests | ⏳ 0/13 | 13 | 0% |
| Frontend Build | ❌ | Fix deps | 0% |
| E2E Tests | ⏳ Config OK | Tests | 20% |
| Documentation | ⏳ Partial | Updates | 50% |
| **CELKEM** | **~50%** | **~50%** | **50%** |

---

## 🎯 NEXT IMMEDIATE ACTION

**Option A - Quick Wins Path:**
1. Commit současný stav (2 min)
2. Fix frontend build (10-30 min)
3. Skip integration tests pro později
4. Update dokumentace (10 min)
→ **Total: 22-42 minut → můžeme pushnout funkční stav**

**Option B - Complete Path:**
1. Commit současný stav (2 min)  
2. Fix frontend build (10-30 min)
3. Add Testcontainers + fix IT tests (30-45 min)
4. Basic E2E test (20 min)
5. Update docs (15 min)
→ **Total: 77-112 minut → kompletní implementace**

---

**Tvoje volba:** Co teď tackneš? Quick wins nebo complete implementation? 🚀
