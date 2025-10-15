# 🧪 Testing Best Practices - Parallel Execution Guidelines

## 🎯 Overview

Our test suite is configured for **maximum parallelism** to fully utilize CPU cores:
- **Maven Surefire**: 1.5C forks (1.5 threads per CPU core)
- **Maven Failsafe**: 1C forks (1 thread per CPU core)
- **JUnit 5**: Concurrent execution enabled (8 threads default)
- **Testcontainers**: Shared PostgreSQL/Redis/Kafka per JVM
- **Isolation**: Per-test DB schema + Kafka topic suffixes

## ✅ DO: Write Parallel-Safe Tests

### 1. **Extend AbstractIntegrationTest for IT tests**
```java
@SpringBootTest
class MyServiceIT extends AbstractIntegrationTest {
  // Automatically gets:
  // - Shared Testcontainers (PostgreSQL, Redis, Kafka)
  // - Unique DB schema: schemaName (e.g., "s_a1b2c3d4")
  // - Unique Kafka topic suffix: topicSuffix
}
```

### 2. **Use Awaitility for async assertions**
```java
import static org.awaitility.Awaitility.await;

@Test
void testAsyncOperation() {
  // ❌ WRONG: Thread.sleep(5000)
  
  // ✅ CORRECT: Wait with timeout and condition
  await().atMost(5, SECONDS)
         .pollInterval(100, MILLISECONDS)
         .untilAsserted(() -> {
           assertThat(repository.findById(id)).isPresent();
         });
}
```

### 3. **Use unique identifiers per test**
```java
@Test
void testKafkaMessage() {
  // Use schemaName for isolation
  String topic = "workflow.events_" + topicSuffix; // e.g., "workflow.events_s_a1b2c3d4"
  String consumerGroup = "test_" + schemaName;    // e.g., "test_s_a1b2c3d4"
  
  kafkaProducer.send(topic, message);
  // No conflicts with other parallel tests!
}
```

## ⚠️ AVOID: Context Pollution

### 1. **Minimize @DirtiesContext**
```java
// ❌ EXPENSIVE: Reloads Spring context after EVERY test
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class MyTest {
  // Only use if you modify global state that can't be cleaned up
  // Example: CircuitBreaker, WireMock, global caches
}

// ✅ BETTER: Use AFTER_CLASS if really needed
@DirtiesContext(classMode = AFTER_CLASS)
class MyTest {
  // Context reloaded only once after all tests in class
}

// ✅ BEST: No @DirtiesContext at all
class MyTest extends AbstractIntegrationTest {
  // Use per-test schema isolation instead!
  
  @AfterEach
  void cleanup() {
    // Manual cleanup if needed (but schema is auto-dropped)
  }
}
```

### 2. **Split Unit vs Integration Tests**
```java
// Unit test - no Spring context
class ServiceTest {
  @Test
  void testBusinessLogic() {
    Service service = new Service(mockRepository);
    // Fast, parallel-safe, no @SpringBootTest
  }
}

// Integration test - with Spring context
@SpringBootTest
class ServiceIT extends AbstractIntegrationTest {
  @Autowired
  private Service service;
  
  @Test
  void testWithDatabase() {
    // Uses shared Testcontainers + per-test schema
  }
}
```

## 🚀 Performance Tips

### 1. **Avoid unnecessary delays**
```java
// ❌ SLOW: Fixed delay regardless of actual timing
Thread.sleep(2000);

// ✅ FAST: Wait only as long as needed
await().atMost(2, SECONDS)
       .until(() -> condition.isMet());
```

### 2. **Use @TestInstance(PER_CLASS) for expensive setup**
```java
@TestInstance(Lifecycle.PER_CLASS)
class ExpensiveSetupIT extends AbstractIntegrationTest {
  @BeforeAll
  void setupOnce() {
    // Runs ONCE for entire class (not per test)
    // Shared containers already use this pattern
  }
}
```

### 3. **Configure timeouts in application-test.yaml**
```yaml
# Tight timeouts for fast failure detection
spring:
  datasource:
    hikari:
      connection-timeout: 5000  # 5s max
  kafka:
    consumer:
      max-poll-interval-ms: 15000  # 15s max
```

## 📝 Current State

### @DirtiesContext Usage (2 instances)
1. **CubeQueryServiceIT** - AFTER_EACH_METHOD ⚠️
   - Reason: CircuitBreaker global state + WireMock
   - Justified: Yes (global state cannot be isolated)

2. **PresenceNrtIT** - AFTER_CLASS ✅
   - Reason: WebSocket connections
   - Justified: Yes (acceptable performance impact)

### Thread.sleep Usage (17 instances)
Most are **intentional delays** for:
- Kafka message ordering (100ms margins)
- Lock expiration testing (2000ms waits)
- Simulation of processing time (10-200ms)

**Action**: Monitor if these cause flakiness, then migrate to Awaitility.

## 🔧 CI/CD Commands

### Local Development (8 threads)
```bash
mvn verify -Dsurefire.threads=8
```

### CI Environment (auto-detect cores)
```bash
mvn -T 1C -Pci-fast -Dsurefire.threads=${CPU_THREADS:-8} verify
```

### Debug Parallel Issues
```bash
# Run sequentially for debugging
mvn verify -Dsurefire.threads=1 -DforkCount=1

# Verbose test output
mvn verify -X
```

## 📚 Resources

- [Awaitility Documentation](https://github.com/awaitility/awaitility)
- [JUnit 5 Parallel Execution](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parallel-execution)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/examples/fork-options-and-parallel-execution.html)
