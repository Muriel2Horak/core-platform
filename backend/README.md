# Core Platform Backend

Java-based backend service for Core Platform, built with Spring Boot 3.5.5.

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+** (or use bundled `./mvnw`)
- **Docker** (for Testcontainers in integration tests)

### Build & Test
```bash
# Clean build with all tests
./mvnw clean verify

# Skip tests (faster build)
./mvnw clean package -DskipTests

# Run only unit tests (fast)
./mvnw test

# Run only integration tests (slow, requires Docker)
./mvnw verify -DskipTests=true -DskipITs=false
```

## 🧪 Testing

### Local Development (Parallel Execution)
Run tests with **8 parallel threads** (default):
```bash
./mvnw verify -Dsurefire.threads=8
```

Run tests with **custom thread count**:
```bash
# Use 4 threads (half CPU utilization)
./mvnw verify -Dsurefire.threads=4

# Use 16 threads (for high-core machines)
./mvnw verify -Dsurefire.threads=16
```

### CI/CD (Auto-detect CPU cores)
Use the `ci-fast` profile for optimal parallelism in CI environments:
```bash
# Auto-detect cores: builds with 1 thread/core, tests with $CPU_THREADS or 8
mvn -T 1C -Pci-fast -DskipITs=false -Dsurefire.threads=${CPU_THREADS:-8} verify
```

**Example CI Script (GitHub Actions, GitLab CI, etc.):**
```yaml
# .github/workflows/backend-tests.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Set CPU threads for tests
        run: echo "CPU_THREADS=$(nproc)" >> $GITHUB_ENV
      
      - name: Run tests
        run: |
          cd backend
          mvn -T 1C -Pci-fast -Dsurefire.threads=$CPU_THREADS verify
```

### Debug Parallel Issues
If tests fail due to parallelism, run **sequentially** for debugging:
```bash
# Single-threaded, no forking
./mvnw verify -Dsurefire.threads=1 -DforkCount=1

# Verbose output
./mvnw verify -X
```

## 📊 Test Architecture

### Parallelism Configuration
- **Maven Surefire** (Unit Tests): `1.5C` forks, `classesAndMethods` parallelism, configurable threads (default: 8)
- **Maven Failsafe** (Integration Tests): `1C` forks, `reuseForks=true`, same thread count
- **JUnit 5**: Concurrent execution enabled at class and method level
- **Testcontainers**: Shared PostgreSQL/Redis/Kafka containers per JVM, reused across tests

### Per-Test Isolation
Each integration test gets:
- **Unique DB schema**: `s_<8-char-uuid>` (e.g., `s_a1b2c3d4`)
- **Unique Kafka topics**: `base_topic_<schema>` suffix
- **Unique consumer groups**: `test_<schema>`

This allows **safe parallel execution** without test interference.

### Best Practices
See [TESTING_BEST_PRACTICES.md](./TESTING_BEST_PRACTICES.md) for:
- Writing parallel-safe tests
- Using `AbstractIntegrationTest` base class
- Awaitility vs `Thread.sleep`
- Avoiding `@DirtiesContext`
- Timeouts configuration

## 🏗️ Project Structure

```
backend/
├── src/main/java/cz/muriel/core/
│   ├── controller/        # REST API controllers
│   ├── service/           # Business logic services
│   ├── repository/        # JPA repositories
│   ├── security/          # Authentication & authorization
│   ├── streaming/         # Kafka/CDC streaming
│   ├── reporting/         # Cube.js reporting layer
│   ├── presence/          # WebSocket presence tracking
│   └── workflow/          # Workflow engine
├── src/main/resources/
│   ├── db/migration/      # Flyway SQL migrations
│   └── application.yml    # Main configuration
├── src/test/java/         # Unit & integration tests
│   └── cz/muriel/core/test/
│       └── AbstractIntegrationTest.java  # Base class for ITs
└── pom.xml                # Maven dependencies & build config
```

## 📦 Dependencies

### Core Stack
- **Spring Boot 3.5.5** (Web, Data JPA, Security, Kafka)
- **PostgreSQL 16** (main database + TimescaleDB for metrics)
- **Kafka** (event streaming + CDC)
- **Redis** (caching + presence tracking)
- **Keycloak** (OAuth2/OIDC authentication)

### Testing Stack
- **JUnit 5** (test framework)
- **Testcontainers** (PostgreSQL, Redis, Kafka)
- **Awaitility** (async assertions)
- **WireMock** (HTTP stubbing)
- **AssertJ** (fluent assertions)

## 🔧 Configuration

### Profiles
- `default` - Production configuration
- `test` - Test configuration (Testcontainers, mocks)
- `ci-fast` - CI optimization (configurable thread count)
- `security` - OWASP Dependency-Check

### Environment Variables
```bash
# Required for production
export DATABASE_URL=jdbc:postgresql://localhost:5432/coredb
export KEYCLOAK_URL=http://localhost:8080/auth
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export REDIS_HOST=localhost

# Optional for CI
export CPU_THREADS=8  # Override thread count
export NVD_API_KEY=xxx  # OWASP dependency-check rate limit
```

## 🛡️ Security

### Dependency Scanning
Run OWASP Dependency-Check:
```bash
./mvnw verify -Psecurity
```

Reports generated in `target/dependency-check/`.

### Enforcer Rules
Maven Enforcer runs automatically during build:
- ✅ BanDuplicateClasses
- ✅ RequireUpperBoundDeps (warn)
- ✅ DependencyConvergence
- ✅ RequireMavenVersion (3.6.0+)
- ✅ RequireJavaVersion (17+)

Disable enforcer temporarily (refactoring only):
```bash
./mvnw clean install -Denforcer.skip=true
```

## 📈 Performance

### Build Times (8-core machine)
- **Sequential build** (`./mvnw verify`): ~3-4 min
- **Parallel build** (`./mvnw -T 1C -Pci-fast verify`): ~90-120 sec
- **Unit tests only** (`./mvnw test`): ~15-20 sec
- **IT tests only** (`./mvnw verify -DskipTests -Dit.test=*`): ~60-90 sec

### Optimization Tips
1. **Use `-T 1C`**: Parallel Maven build (1 thread per core)
2. **Use `-Pci-fast`**: Configurable test threads via `$CPU_THREADS`
3. **Skip unnecessary phases**: `-DskipTests` or `-DskipITs`
4. **Reuse containers**: Testcontainers `.withReuse(true)` already enabled

## 🐛 Troubleshooting

### "Tests fail randomly in parallel"
→ See [TESTING_BEST_PRACTICES.md](./TESTING_BEST_PRACTICES.md) for isolation strategies.

### "Testcontainers can't start containers"
→ Ensure Docker is running: `docker ps`

### "OutOfMemoryError: Metaspace"
→ Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
./mvnw verify
```

### "Dependency convergence errors"
→ Check `<dependencyManagement>` in `pom.xml` for version overrides.

## 📚 Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Testcontainers Guides](https://www.testcontainers.org/quickstart/junit_5_quickstart/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
