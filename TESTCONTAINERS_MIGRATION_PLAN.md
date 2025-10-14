# 🧪 TESTCONTAINERS MIGRATION PLAN

**Datum**: 2025-10-14  
**Cíl**: Všechny backend integration testy → Testcontainers  
**Důvod**: Pre-build testy nesmí záviset na running environment  

---

## 🎯 TESTING STRATEGY

### ✅ PRE-BUILD (CI/CD Gate)
```
Unit Tests → Pure Java, mocky, žádná DB
Integration Tests → Testcontainers (izolovaná PostgreSQL/Redis)
```
**Vlastnosti**:
- ✅ Rychlé (Testcontainers reuse)
- ✅ Izolované (žádné závislosti)
- ✅ Reprodukovatelné (čistá DB každý run)
- ✅ Paralelní (každý test svůj kontejner)

### ✅ POST-BUILD (Deployment Verification)
```
E2E Tests → Skutečné prostředí (staging/production)
Smoke Tests → Health checks, základní flow
Load Tests → Performance na produkci
```
**Vlastnosti**:
- ✅ Reálná konfigurace
- ✅ Síťové latence
- ✅ Skutečná data
- ✅ Monitoring & alerting

---

## 📊 SOUČASNÝ STAV

### Kategorie testů:

#### ✅ JIŽ POUŽÍVAJÍ TESTCONTAINERS (6 testů)
```java
// Tyto testy JIŽ DĚDÍ z AbstractIntegrationTest
1. PresenceServiceIntegrationTest
2. TenantFilterIntegrationTest  
3. MonitoringProxyServiceTest
4. ReportingPropertiesTest
5. Phase2IntegrationTest (možná)
6. ? (need to check)
```

#### ⚠️ POTŘEBUJÍ SPRING CONTEXT, ALE BEZ DB (MOCKY)
```java
// Pure unit tests s Spring, ale mockovanými dependencies
- TenantOrgServiceImplTest → má @Mock, nepotřebuje DB
- ReportingFeatureToggleTest → testuje feature toggle logic
- TenantResolverTest → unit test pro resolver logic
```
**Action**: ✅ Ponechat jako unit testy (žádná změna)

#### ❌ INTEGRATION TESTY BEZ TESTCONTAINERS (need fix)
```java
// Tyto používají @SpringBootTest ALE NEDĚDÍ z AbstractIntegrationTest
- MonitoringProxyServiceTest (selhává!)
- PresenceServiceIntegrationTest (selhává!)
- ReportingPropertiesTest (selhává!)
- TenantFilterIntegrationTest (selhává!)
```
**Action**: 🔧 Přidat extends AbstractIntegrationTest

#### ✅ PURE UNIT TESTS (no Spring, no DB)
```java
// Tyto jsou OK, žádná změna
- WorkflowExecutorRegistryTest
- SendEmailExecutorTest
- MonitoringDSLValidatorTest
- TenantDeterministicUuidTest
- WorkflowExecutionServiceTest (částečně)
- WorkflowVersionServiceTest
```
**Action**: ✅ Perfektní (žádná změna)

---

## 🔧 IMPLEMENTATION PLAN

### Krok 1: Audit všech testů
```bash
cd backend

# Najdi všechny @SpringBootTest testy
echo "=== Tests with @SpringBootTest ==="
grep -r "@SpringBootTest" src/test/java --include="*.java" -l

# Najdi které JIŽ dědí z AbstractIntegrationTest
echo -e "\n=== Already using AbstractIntegrationTest ==="
grep -r "extends AbstractIntegrationTest" src/test/java --include="*.java" -l

# Najdi které NEDĚDÍ, ale MĚLY by
echo -e "\n=== Need Testcontainers (SpringBootTest but no AbstractIntegrationTest) ==="
grep -r "@SpringBootTest" src/test/java --include="*.java" -l | \
  while read file; do
    if ! grep -q "extends AbstractIntegrationTest" "$file"; then
      echo "$file"
    fi
  done
```

### Krok 2: Opravit integration testy
Pro každý test který používá `@SpringBootTest` ALE nemá DB access:

**Before:**
```java
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
  @Autowired
  private MyService service;
  
  @Test
  void shouldWork() {
    // test používá databázi
  }
}
```

**After:**
```java
@SpringBootTest
class MyIntegrationTest extends AbstractIntegrationTest {
  @Autowired
  private MyService service;
  
  @Test
  void shouldWork() {
    // test používá Testcontainers PostgreSQL
  }
}
```

**Změny**:
1. ✅ Přidat `extends AbstractIntegrationTest`
2. ✅ Odstranit `@ActiveProfiles("test")` (už je v parent)
3. ✅ Odstranit `@Testcontainers` (už je v parent)
4. ✅ Odstranit custom `@DynamicPropertySource` (už je v parent)

### Krok 3: Fix failing tests

#### A) MonitoringProxyServiceTest
```java
// src/test/java/cz/muriel/core/monitoring/bff/service/MonitoringProxyServiceTest.java

// BEFORE:
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(MockTestConfig.class)
class MonitoringProxyServiceTest {
  // ...
}

// AFTER:
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitoringProxyServiceTest extends AbstractIntegrationTest {
  // @ActiveProfiles, @Testcontainers, @Import už je v parent
  // ...
}
```

#### B) PresenceServiceIntegrationTest
```java
// BEFORE:
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(MockTestConfig.class)
class PresenceServiceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = ...;
  
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
  }
}

// AFTER:
@SpringBootTest
class PresenceServiceIntegrationTest extends AbstractIntegrationTest {
  // Všechno už je v AbstractIntegrationTest!
  // Žádný custom @Container, @DynamicPropertySource
}
```

#### C) ReportingPropertiesTest
```java
// BEFORE:
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
  "reporting.enabled=true",
  // ...
})
class ReportingPropertiesTest {
  // test
}

// AFTER:
@SpringBootTest
@TestPropertySource(properties = {
  "reporting.enabled=true",
  // ...
})
class ReportingPropertiesTest extends AbstractIntegrationTest {
  // Přidán extends AbstractIntegrationTest
}
```

#### D) TenantFilterIntegrationTest
```java
// BEFORE:
@SpringBootTest
@ActiveProfiles("test")
class TenantFilterIntegrationTest {
  // test používá Repository → potřebuje DB
}

// AFTER:
@SpringBootTest
class TenantFilterIntegrationTest extends AbstractIntegrationTest {
  // Nyní má Testcontainers PostgreSQL
}
```

### Krok 4: Update WorkflowVersionServiceTest
```java
// BEFORE:
@SpringBootTest
class WorkflowVersionServiceTest {
  // Selhává: "Unable to find @SpringBootConfiguration"
}

// AFTER:
@SpringBootTest(classes = BackendApplication.class)
class WorkflowVersionServiceTest extends AbstractIntegrationTest {
  // Fixed: explicit config + Testcontainers
}
```

### Krok 5: Verify pure unit tests
Tyto testy jsou OK (NEMĚNIT):
```java
// WorkflowExecutorRegistryTest - pure unit test
// SendEmailExecutorTest - uses @Mock
// MonitoringDSLValidatorTest - validator logic only
// TenantOrgServiceImplTest - uses @Mock
```

---

## 🚀 EXECUTION PLAN

### Phase 1: Audit (5 min)
```bash
cd backend
./scripts/audit-tests.sh > /tmp/test-audit.txt
cat /tmp/test-audit.txt
```

### Phase 2: Fix Integration Tests (15 min)
```bash
# 1. MonitoringProxyServiceTest
# 2. PresenceServiceIntegrationTest
# 3. ReportingPropertiesTest
# 4. TenantFilterIntegrationTest
# 5. WorkflowVersionServiceTest
```

### Phase 3: Test (10 min)
```bash
# Start Docker
docker ps

# Run tests
./mvnw test

# Should see:
# ✅ Testcontainers starting PostgreSQL container
# ✅ All tests pass
# ✅ Container automatically cleaned up
```

### Phase 4: Verify (5 min)
```bash
# Check no tests rely on external DB
docker stop $(docker ps -aq)  # Stop all containers
./mvnw test  # Should still work (Testcontainers auto-start)
```

---

## 📋 CHECKLIST

### Per Test File:
- [ ] Používá `@SpringBootTest`?
  - [ ] ANO → Je to integration test
    - [ ] Dědí z `AbstractIntegrationTest`?
      - [ ] ANO → ✅ OK
      - [ ] NE → 🔧 Přidat `extends AbstractIntegrationTest`
  - [ ] NE → Je to unit test
    - [ ] ✅ OK (ponechat)

### Global Checks:
- [ ] Žádný test nemá vlastní `@Container` PostgreSQL (duplikace s AbstractIntegrationTest)
- [ ] Žádný test nemá vlastní `@DynamicPropertySource` pro datasource
- [ ] Všechny testy s `@Repository` / `@Service` dependencies dědí z `AbstractIntegrationTest`
- [ ] Pure unit testy (s `@Mock`) NEDĚDÍ z `AbstractIntegrationTest`

---

## 🎯 EXPECTED RESULTS

### Before Fix:
```
❌ Tests run: 186, Failures: 2, Errors: 28, Skipped: 8
❌ Requires running PostgreSQL
❌ Flyway migration fails
```

### After Fix:
```
✅ Tests run: 186, Failures: 0, Errors: 0, Skipped: 0
✅ Testcontainers auto-starts PostgreSQL
✅ No external dependencies
✅ Runs anywhere (CI/CD, laptop, Docker only)
```

### Performance:
```
First run:  ~45s (download postgres:16-alpine)
Next runs:  ~15s (Testcontainers reuse)
Parallel:   Multiple tests, isolated containers
```

---

## 🔥 QUICK FIX SCRIPT

```bash
#!/bin/bash
# fix-integration-tests.sh

cd backend/src/test/java

# Fix MonitoringProxyServiceTest
sed -i '' '/@ActiveProfiles("test")/d' cz/muriel/core/monitoring/bff/service/MonitoringProxyServiceTest.java
sed -i '' '/@Testcontainers/d' cz/muriel/core/monitoring/bff/service/MonitoringProxyServiceTest.java
sed -i '' 's/@SpringBootTest/@SpringBootTest\nclass MonitoringProxyServiceTest extends AbstractIntegrationTest {/' cz/muriel/core/monitoring/bff/service/MonitoringProxyServiceTest.java

# Fix PresenceServiceIntegrationTest
sed -i '' '/@ActiveProfiles("test")/d' cz/muriel/core/presence/PresenceServiceIntegrationTest.java
sed -i '' 's/@SpringBootTest/@SpringBootTest\nclass PresenceServiceIntegrationTest extends AbstractIntegrationTest {/' cz/muriel/core/presence/PresenceServiceIntegrationTest.java

# Fix ReportingPropertiesTest
sed -i '' 's/@SpringBootTest/@SpringBootTest\nclass ReportingPropertiesTest extends AbstractIntegrationTest {/' cz/muriel/core/reporting/app/ReportingPropertiesTest.java

# Fix TenantFilterIntegrationTest
sed -i '' 's/@SpringBootTest/@SpringBootTest\nclass TenantFilterIntegrationTest extends AbstractIntegrationTest {/' cz/muriel/core/tenant/TenantFilterIntegrationTest.java

# Fix WorkflowVersionServiceTest
sed -i '' 's/@SpringBootTest/@SpringBootTest(classes = BackendApplication.class)\nclass WorkflowVersionServiceTest extends AbstractIntegrationTest {/' com/platform/workflow/versioning/WorkflowVersionServiceTest.java

echo "✅ Integration tests fixed!"
echo "Run: ./mvnw test"
```

---

## 📚 DOCUMENTATION UPDATES

### Update README.md:
```markdown
## Testing

### Unit Tests
Pure Java tests with mocks. No Docker needed.
```bash
./mvnw test -Dtest="**/*Test" -DexcludeTests="**/*IntegrationTest"
```

### Integration Tests
Uses Testcontainers (requires Docker).
```bash
# Start Docker Desktop first
docker ps

# Run integration tests
./mvnw test
```

Testcontainers automatically:
- Downloads PostgreSQL image (first time only)
- Starts ephemeral container
- Runs Flyway migrations
- Executes tests
- Cleans up container

### E2E Tests (Post-Deploy)
Runs against deployed environment.
```bash
make dev-up           # Start environment
make test-e2e-pre     # Smoke tests
make test-e2e-post    # Full E2E
```
```

---

## 🎉 BENEFITS

### Development:
- ✅ **No manual setup** - just `./mvnw test`
- ✅ **Isolated** - each test run gets clean DB
- ✅ **Fast** - Testcontainers reuse containers
- ✅ **Reliable** - no "works on my machine"

### CI/CD:
- ✅ **No infrastructure** - just Docker
- ✅ **Parallel** - multiple builds don't conflict
- ✅ **Reproducible** - same environment every time
- ✅ **Fast** - cached images, reused containers

### Team:
- ✅ **Onboarding** - new devs just run tests
- ✅ **Confidence** - tests verify real DB behavior
- ✅ **Debugging** - can inspect container logs
- ✅ **Flexibility** - easy to test different PG versions

---

**Ready to implement?** 🚀

Chceš, abych:
1. Provedl audit všech testů
2. Opravil failing integration testy
3. Ověřil, že všechno funguje
