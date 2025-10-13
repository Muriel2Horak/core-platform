# Automatické Testování v Build Procesu

## 🎯 Přehled

Od této verze se **všechny unit testy spouštějí automaticky** před Docker buildem. To zajišťuje, že do Docker image se nikdy nedostane kód s nefunkčními testy.

## 🏗️ Kdy se testy spouštějí?

### Automaticky při buildu

```bash
make rebuild    # 1. Unit testy → 2. Docker build → 3. Start
```

**Workflow:**
1. 🧪 **Step 1/3**: Running pre-build tests...
   - Backend unit tests (všechny `*Test.java`)
   - Frontend tests (Vitest/Jest)
2. 🏗️ **Step 2/3**: Building Docker images...
3. 🚀 **Step 3/3**: Starting services + post-deployment checks

### Pokud testy selžou

```
❌ Pre-build tests FAILED

💡 Options:
   1. Fix the failing tests
   2. Run with SKIP_TESTS=true to bypass (NOT RECOMMENDED)
      Example: SKIP_TESTS=true make rebuild
```

Build se **zastaví** a nedojde k Docker buildu, dokud testy neprojdou.

## 🧪 Manuální spuštění testů

```bash
# Backend unit testy
make test-backend

# Frontend testy
make test-frontend

# Všechny unit testy najednou
make test-all

# Specifický test (backend)
cd backend && ./mvnw test -Dtest=TenantOrgServiceImplTest

# Integration testy (po startu prostředí)
make test-mt              # Multitenancy smoke tests
make verify               # Quick health checks
make verify-full          # Full integration tests
```

## 🚫 Přeskočení testů (NOT RECOMMENDED)

V **výjimečných případech** můžete testy přeskočit:

```bash
# Přeskočit unit testy před buildem
SKIP_TESTS=true make rebuild

# Nebo jednotlivě
SKIP_TESTS=true bash scripts/build/pre-build-test.sh all
```

⚠️ **Varování:** Používejte jen dočasně během debugování! Nikdy necommitujte kód s nefunkčními testy.

## 📊 Typy testů v projektu

### 1. Unit Testy (Pre-build)
- **Kdy:** Před Docker buildem
- **Co:** Jednotkové testy bez závislostí
- **Backend:** `*Test.java` (JUnit + Mockito)
- **Frontend:** `*.test.ts`, `*.spec.ts` (Vitest)
- **Čas:** ~10-30 sekund

### 2. Integration Testy (Post-deployment)
- **Kdy:** Po startu prostředí
- **Co:** Testy s reálnými službami
- **Příklady:**
  - `make test-mt` - Multitenancy (Keycloak + Backend + DB + Loki)
  - `make verify` - Health checks (API, Frontend, Observability)
- **Čas:** ~15 sekund (verify) až 3 minuty (test-mt)

### 3. E2E Testy (Manuálně)
- **Kdy:** Před releasy
- **Co:** Playwright testy přes browser
- **Lokace:** `tests/e2e/`
- **Čas:** ~2-5 minut

## 📁 Test Reports

### Unit testy
```bash
# Maven surefire reports
backend/target/surefire-reports/

# Pre-build test logs
/tmp/backend-test.log
/tmp/frontend-test.log
```

### Integration testy
```bash
# Multitenancy smoke tests
artifacts/
TEST_REPORT.md

# Build Doctor diagnostics
diagnostics/build-*.log
diagnostics/build-report-*.json
```

## 🔧 Konfigurace

### Backend (Maven)
```xml
<!-- pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <configuration>
        <includes>
          <include>**/*Test.java</include>
        </includes>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### Frontend (Vitest)
```json
// package.json
{
  "scripts": {
    "test": "vitest",
    "test:run": "vitest run"
  }
}
```

## 🚀 Best Practices

### ✅ DO

- **Psát unit testy** pro každou novou funkcionalitu
- **Spouštět testy lokálně** před commitem: `make test-all`
- **Fixovat failing testy** okamžitě
- **Používat mocks** pro externí závislosti v unit testech
- **Psát popisné názvy testů**: `shouldReturnCoreOrgForCoreAdmin`

### ❌ DON'T

- **Necommitovat** nefunkční testy
- **Nepřeskakovat** testy routinně (`SKIP_TESTS=true`)
- **Netestovat** v unit testech externí služby (DB, Keycloak)
- **Neduplikovat** testy (jeden test = jedna věc)
- **Neignorovat** failing testy (`@Disabled`)

## 🐛 Troubleshooting

### "Tests fail in Docker but pass locally"

```bash
# 1. Zkontroluj Maven/Node verzi
cd backend && ./mvnw --version
cd frontend && node --version

# 2. Clean rebuild
cd backend && ./mvnw clean test
cd frontend && rm -rf node_modules && npm install && npm test
```

### "UnnecessaryStubbingException in Mockito"

```java
// Použij lenient() pro mocky, které nejsou použity ve všech testech
lenient().when(mockRepository.findById(anyString()))
    .thenReturn(Optional.of(entity));
```

### "Tests are too slow"

```bash
# Spusť jen konkrétní test
./mvnw test -Dtest=ClassName#methodName

# Paralelní testy (Maven)
./mvnw test -T 1C  # 1 thread per CPU core
```

## 📚 Související

- [BUILD_DOCTOR.md](BUILD_DOCTOR.md) - Build diagnostika
- [POST_DEPLOYMENT_VERIFICATION.md](POST_DEPLOYMENT_VERIFICATION.md) - Post-deployment checks
- [TESTING.md](TESTING.md) - E2E a smoke testy
- [backend/src/test/](../backend/src/test/) - Backend testy
- [frontend/src/**/*.test.ts](../frontend/src/) - Frontend testy

## 🎯 Summary

**Před touto změnou:**
- Testy se spouštěly manuálně
- Docker build ignoroval testy (`-DskipTests`)
- Failing testy se dostaly do image

**Po této změně:**
- ✅ Testy běží automaticky před buildem
- ✅ Failing testy zastaví build
- ✅ Option přeskočit (emergency only)
- ✅ Kompletní test reporting

**Výsledek:** Vyšší kvalita kódu, méně bugů v produkci! 🚀
