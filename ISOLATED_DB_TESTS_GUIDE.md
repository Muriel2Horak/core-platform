# 🐳 PRE-DEPLOY TESTS S IZOLOVANOU DB

**Datum**: 2025-10-14  
**Problem**: Backend unit testy vyžadují databázi  
**Solution**: 3 možnosti pro izolované testování  

---

## 🎯 AKTUÁLNÍ STAV

### Co máš v projektu:
- ✅ **Testcontainers** (PostgreSQL + Redis) v `pom.xml`
- ✅ **AbstractIntegrationTest** - base class s kontejnery
- ✅ **H2** in-memory database pro unit testy
- ⚠️ **Problém**: Některé testy používají Testcontainers, ale vyžadují Docker

### Selhávající testy:
- `MonitoringProxyServiceTest` - extends AbstractIntegrationTest ✅
- `PresenceServiceIntegrationTest` - extends AbstractIntegrationTest ✅
- Ostatní - používají full Spring context bez izolace ⚠️

---

## 📋 MOŽNOST 1: TESTCONTAINERS (DOPORUČENO)

### Co to dělá:
- Automaticky **startuje Docker kontejner** s PostgreSQL
- Spustí Flyway migrace
- Běží testy
- **Automaticky smaže kontejner** po testech

### Jak to funguje:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
  
  @Container
  protected static final PostgreSQLContainer<?> postgresContainer = 
    new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test")
      .withReuse(true);  // ← Rychlejší: reuse napříč testy
  
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgresContainer::getUsername);
    registry.add("spring.datasource.password", postgresContainer::getPassword);
  }
}
```

### Jak použít:
```bash
# 1. Ujisti se, že Docker běží
docker ps

# 2. Spusť testy
cd backend
./mvnw test

# Testcontainers automaticky:
# - Stáhne postgres:16-alpine image (pokud není cached)
# - Spustí kontejner
# - Nakonfiguruje Spring datasource
# - Spustí Flyway migrace
# - Běží testy
# - Zastaví a smaže kontejner
```

### Výhody:
- ✅ **Plná izolace** - každý test run dostane čistou DB
- ✅ **Reálná PostgreSQL** - stejně jako v produkci
- ✅ **Automatické cleanup** - žádné manuální mazání
- ✅ **Rychlé s reuse** - první run pomalý, další rychlé
- ✅ **Paralelní testy** - každý má svůj kontejner

### Nevýhody:
- ⚠️ Vyžaduje běžící **Docker Desktop** (nebo Docker daemon)
- ⚠️ První test run trvá ~30s (download image)
- ⚠️ Pomalejší než H2 (ale reálnější)

### Implementace:
Už MÁTE! Stačí:
```bash
# Spustit Docker
open -a Docker

# Počkat na start (5-10s)
sleep 10

# Spustit testy
cd backend && ./mvnw test
```

---

## 📋 MOŽNOST 2: H2 IN-MEMORY DATABASE

### Co to dělá:
- Používá **H2 databázi v RAM**
- Vytvoří se při startu testu
- Automaticky se **smaže po testu**
- Žádný Docker nepotřeba

### Jak to funguje:
```yaml
# src/test/resources/application-test-h2.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop  # H2 vytvoří schema automaticky
  
  flyway:
    enabled: false  # Flyway může mít problémy s H2
```

### Implementace:
```java
// Option A: Base test class pro H2
@SpringBootTest
@ActiveProfiles("test-h2")
public abstract class AbstractH2Test {
  // Žádná konfigurace - H2 se samo vytvoří a smaže
}

// Option B: Použít v konkrétním testu
@SpringBootTest
@ActiveProfiles("test-h2")
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
  "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MyFastUnitTest {
  // test
}
```

### Jak použít:
```bash
# 1. Vytvořit H2 profile
cd backend/src/test/resources
cat > application-test-h2.yml << 'EOF'
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
EOF

# 2. Spustit testy s H2 profilem
cd backend
./mvnw test -Dspring.profiles.active=test-h2
```

### Výhody:
- ✅ **Velmi rychlé** - vše v RAM
- ✅ **Žádný Docker** - běží všude
- ✅ **Automatické cleanup** - smaže se po testu
- ✅ **Nulová konfigurace** - just works

### Nevýhody:
- ⚠️ **Není 100% PostgreSQL** - některé funkce chybí
- ⚠️ **SQL dialekt rozdíly** - funkce `generate_tenant_uuid` nebude fungovat
- ⚠️ **Flyway migrace** - musí se vypnout nebo přepsat
- ⚠️ **PostgreSQL-specific features** - JSON operators, UUID, custom functions

### Kdy použít:
- ✅ Jednoduché unit testy (CRUD operace)
- ✅ CI/CD pipelines bez Docker
- ✅ Rychlé lokální testování
- ❌ Testy s PostgreSQL-specific SQL
- ❌ Testy s Flyway migrací

---

## 📋 MOŽNOST 3: MAKEFILE TARGET PRO TESTS

### Co to dělá:
- **Spustí ephemeral PostgreSQL** kontejner
- Běží testy
- **Automaticky smaže** kontejner

### Implementace:
Přidej do `Makefile`:

```makefile
# =============================================================================
# 🧪 ISOLATED PRE-DEPLOY TESTS
# =============================================================================

# Run backend tests with ephemeral PostgreSQL container
.PHONY: test-backend-isolated
test-backend-isolated:
	@echo "🧪 Running backend tests with ephemeral PostgreSQL..."
	@echo "1️⃣  Starting PostgreSQL container..."
	@docker run -d \
		--name test-postgres-$$$ \
		-e POSTGRES_DB=testdb \
		-e POSTGRES_USER=test \
		-e POSTGRES_PASSWORD=test \
		-p 15432:5432 \
		postgres:16-alpine
	@echo "⏳ Waiting for PostgreSQL to start (10s)..."
	@sleep 10
	@echo "2️⃣  Running tests..."
	@cd backend && \
		TEST_DB_URL=jdbc:postgresql://localhost:15432/testdb \
		TEST_DB_USER=test \
		TEST_DB_PASSWORD=test \
		./mvnw test -Dspring.profiles.active=test-isolated || \
		(echo "❌ Tests failed!"; docker stop test-postgres-$$$; docker rm test-postgres-$$$; exit 1)
	@echo "3️⃣  Cleaning up PostgreSQL container..."
	@docker stop test-postgres-$$$ || true
	@docker rm test-postgres-$$$ || true
	@echo "✅ Tests completed and container removed!"

# Run all pre-deploy tests (frontend + backend with isolated DB)
.PHONY: test-predeploy
test-predeploy:
	@echo "🚀 Running pre-deploy test suite..."
	@echo ""
	@echo "1️⃣  Frontend tests..."
	@cd frontend && npm test -- --run
	@echo ""
	@echo "2️⃣  Backend tests (with ephemeral DB)..."
	@$(MAKE) test-backend-isolated
	@echo ""
	@echo "🎉 All pre-deploy tests passed!"
```

### Konfigurace pro isolated profile:
```yaml
# backend/src/test/resources/application-test-isolated.yml
spring:
  datasource:
    url: ${TEST_DB_URL:jdbc:postgresql://localhost:15432/testdb}
    username: ${TEST_DB_USER:test}
    password: ${TEST_DB_PASSWORD:test}
  
  flyway:
    enabled: true
    clean-disabled: false
```

### Jak použít:
```bash
# Spustit backend testy s ephemeral DB
make test-backend-isolated

# Nebo všechny pre-deploy testy
make test-predeploy
```

### Výhody:
- ✅ **Žádný running environment** - vše je ephemeral
- ✅ **Automatické cleanup** - kontejner se smaže
- ✅ **Reálná PostgreSQL** - stejně jako v produkci
- ✅ **Jednoduchý CI/CD** - jeden příkaz

### Nevýhody:
- ⚠️ Vyžaduje Docker
- ⚠️ Trochu pomalejší než Testcontainers reuse
- ⚠️ Port konflikty (15432) - pokud už běží něco

---

## 📋 MOŽNOST 4: DOCKER COMPOSE FOR TESTS

### Implementace:
Vytvoř `docker-compose.test.yml`:

```yaml
# docker-compose.test.yml - Ephemeral test environment
version: '3.8'

services:
  test-postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    ports:
      - "15432:5432"
    tmpfs:
      - /var/lib/postgresql/data  # ← In-memory, auto-deleted
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U test"]
      interval: 2s
      timeout: 5s
      retries: 5

  test-redis:
    image: redis:7-alpine
    ports:
      - "16379:6379"
    tmpfs:
      - /data  # ← In-memory, auto-deleted
```

### Makefile target:
```makefile
.PHONY: test-backend-compose
test-backend-compose:
	@echo "🧪 Starting test environment..."
	@docker compose -f docker-compose.test.yml up -d
	@echo "⏳ Waiting for services..."
	@docker compose -f docker-compose.test.yml exec -T test-postgres pg_isready -U test || sleep 5
	@echo "🧪 Running tests..."
	@cd backend && \
		TEST_DB_URL=jdbc:postgresql://localhost:15432/testdb \
		./mvnw test -Dspring.profiles.active=test-isolated || \
		(docker compose -f docker-compose.test.yml down -v; exit 1)
	@echo "🗑️  Cleaning up..."
	@docker compose -f docker-compose.test.yml down -v
	@echo "✅ Done!"
```

### Výhody:
- ✅ **Multi-service support** (PostgreSQL + Redis + Kafka)
- ✅ **tmpfs** - vše v RAM, automaticky smazáno
- ✅ **Health checks** - čeká na ready state
- ✅ **Jednoduchá konfigurace**

---

## 🎯 DOPORUČENÍ PRO VÁŠ PROJEKT

### Scénář 1: Lokální Development
```bash
# Používej Testcontainers (už máte!)
docker ps  # Ujisti se, že Docker běží
cd backend && ./mvnw test
```

### Scénář 2: CI/CD Pipeline (GitHub Actions, GitLab CI)
```yaml
# .github/workflows/test.yml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    
    steps:
      - uses: actions/checkout@v3
      - name: Run tests
        run: cd backend && ./mvnw test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb
```

### Scénář 3: Bez Dockeru (laptop bez admin práv)
```bash
# Použij H2 in-memory
cd backend
./mvnw test -Dspring.profiles.active=test-h2
```

---

## 🚀 IMPLEMENTACE: QUICK START

### Krok 1: Přidat Makefile target
```bash
cat >> Makefile << 'EOF'

# Run backend tests with ephemeral PostgreSQL
.PHONY: test-backend-isolated
test-backend-isolated:
	@echo "🧪 Running backend tests with ephemeral PostgreSQL..."
	@docker run -d --name test-postgres-$$$$ \
		-e POSTGRES_DB=testdb -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test \
		-p 15432:5432 postgres:16-alpine >/dev/null
	@sleep 10
	@cd backend && \
		SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:15432/testdb \
		SPRING_DATASOURCE_USERNAME=test \
		SPRING_DATASOURCE_PASSWORD=test \
		./mvnw test || (docker stop test-postgres-$$$$ && docker rm test-postgres-$$$$ && exit 1)
	@docker stop test-postgres-$$$$ >/dev/null && docker rm test-postgres-$$$$ >/dev/null
	@echo "✅ Tests passed and container removed!"
EOF
```

### Krok 2: Test it!
```bash
make test-backend-isolated
```

### Krok 3: Přidat do CI/CD
```bash
# .github/workflows/test.yml nebo GitLab CI
make test-backend-isolated
```

---

## ⚡ PERFORMANCE COMPARISON

| Method | First Run | Subsequent | Docker | Cleanup | Realness |
|--------|-----------|------------|--------|---------|----------|
| **Testcontainers (reuse)** | ~30s | ~5s | ✅ Required | ✅ Auto | 100% |
| **Ephemeral Docker** | ~15s | ~15s | ✅ Required | ✅ Auto | 100% |
| **Docker Compose** | ~15s | ~15s | ✅ Required | ✅ Auto | 100% |
| **H2 In-Memory** | ~3s | ~3s | ❌ Not needed | ✅ Auto | 80% |
| **Running Env** | ~5s | ~5s | ✅ Required | ❌ Manual | 100% |

---

## 🎉 ZÁVĚR

### Pro VÁŠ projekt doporučuji:

**1. Lokálně:** Use Testcontainers (už máte!)
```bash
# Jednou spustit Docker
open -a Docker

# Pak jen:
cd backend && ./mvnw test
```

**2. Pro CI/CD:** Přidat Makefile target
```bash
make test-backend-isolated  # Ephemeral PostgreSQL
```

**3. Pro rychlé iterace:** H2 profile
```bash
./mvnw test -Dspring.profiles.active=test-h2  # Bez Docker
```

### Všechny řešení:
- ✅ **Automaticky vytvoří DB**
- ✅ **Automaticky smaže DB**
- ✅ **Žádné manuální kroky**
- ✅ **Izolované od running environment**

**Chceš, abych implementoval Makefile target s ephemeral PostgreSQL?** 🚀
