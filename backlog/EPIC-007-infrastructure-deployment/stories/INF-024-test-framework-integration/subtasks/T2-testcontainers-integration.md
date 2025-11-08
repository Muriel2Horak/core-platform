# T2: Integrate Testcontainers for Integration Tests

**Parent Story:** INF-024 Test Framework Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 5 hours  
**Owner:** Backend

---

## 🎯 Objective

Replace hardcoded test database URLs with Testcontainers for PostgreSQL, Redis, Kafka.

---

## 📋 Tasks

### 1. Add Testcontainers Dependencies

**File:** `backend/pom.xml`

```xml
<dependencies>
  <!-- Testcontainers -->
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>com.redis.testcontainers</groupId>
    <artifactId>testcontainers-redis</artifactId>
    <version>1.6.4</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

### 2. Create Base Test Configuration

**File:** `backend/src/test/java/cz/muriel/core/test/BaseIntegrationTest.java`

```java
package cz.muriel.core.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    // PostgreSQL Container (shared across tests)
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withReuse(true);  // Reuse container between tests

    // Redis Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")
    )
        .withExposedPorts(6379)
        .withReuse(true);

    // Kafka Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    )
        .withReuse(true);

    @BeforeAll
    static void startContainers() {
        postgres.start();
        redis.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### 3. Migrate Existing Integration Test

**Before (hardcoded):**

**File:** `backend/src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb  # ❌ Hardcoded
```

**After (Testcontainers):**

**File:** `backend/src/test/java/cz/muriel/core/user/UserRepositoryIT.java`

```java
package cz.muriel.core.user;

import cz.muriel.core.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIT extends BaseIntegrationTest {  // ✅ Extends base

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        // Given
        User user = new User("test@example.com", "Test User");
        userRepository.save(user);

        // When
        User found = userRepository.findByEmail("test@example.com").orElseThrow();

        // Then
        assertThat(found.getName()).isEqualTo("Test User");
    }
}
```

### 4. Create Testcontainers Configuration

**File:** `backend/src/test/resources/testcontainers.properties`

```properties
# Enable container reuse across test runs (faster!)
testcontainers.reuse.enable=true

# Docker host (use Colima on macOS)
# docker.host=unix:///Users/USER/.colima/default/docker.sock
```

### 5. Add Singleton Container Pattern

**File:** `backend/src/test/java/cz/muriel/core/test/PostgresTestContainer.java`

```java
package cz.muriel.core.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withReuse(true);

    private PostgresTestContainer() {}

    public static PostgreSQLContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
```

**Usage:**

```java
@DynamicPropertySource
static void configurePostgres(DynamicPropertyRegistry registry) {
    PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

### 6. Test Integration Tests

```bash
# Run integration tests (Testcontainers will start automatically)
cd backend
./mvnw verify -Dtest='**/*IT.java'

# Expected output:
# [Testcontainers] 🐳 Starting container: postgres:16-alpine
# [Testcontainers] ✅ Container postgres:16-alpine started in 3.2s
# [Testcontainers] 🐳 Starting container: redis:7-alpine
# ...
# [INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0

# Verify container reuse (2nd run should be faster)
./mvnw verify -Dtest='**/*IT.java'
# Expected: Containers reused, tests run in ~50% less time
```

---

## ✅ Acceptance Criteria

- [ ] Testcontainers dependencies added to `pom.xml`
- [ ] `BaseIntegrationTest` class created with PostgreSQL, Redis, Kafka
- [ ] All `*IT.java` tests extend `BaseIntegrationTest`
- [ ] Container reuse enabled (faster test runs)
- [ ] Integration tests pass without external services running
- [ ] No hardcoded database URLs in `application-test.yml`
- [ ] CI/CD pipeline runs integration tests successfully

---

## 🔗 Dependencies

- **REQUIRES:** T1 (Pre-Commit Hooks)
- **BLOCKS:** T3 (Coverage Thresholds)
- Docker/Colima must be running
