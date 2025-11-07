# T5: JUnit Listener

**Story:** [S8: Test Registry & Tracking](../S8.md)  
**Status:** 🔵 TODO  
**Effort:** ~1 hodina  
**LOC:** ~50 řádků

---

## 🎯 Objective

Implementovat JUnit test listener pro automatickou registraci Unit a Integration testů do test registry.

---

## 📋 Requirements

### Funkcionalita

- Custom `@UserStory("CORE-XXX")` anotace
- JUnit `TestWatcher` extension pro zachycení test results
- Automatické ukládání do `TestRegistryRepository`
- Extrakce test ID z test metody + class name
- Podpora pro Unit i Integration testy

### Test Annotation Format

```java
@Test
@UserStory("CORE-123")
@DisplayName("Should create user with valid data")
void shouldCreateUserWithValidData() {
    // Test implementation
}
```

---

## 💻 Implementation

### 1. UserStory Annotation

**File:** `backend/src/main/java/cz/muriel/core/testing/annotation/UserStory.java`

```java
package cz.muriel.core.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks test method as covering specific User Story
 * 
 * Usage:
 * @Test
 * @UserStory("CORE-123")
 * void testSomething() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserStory {
    String value();
}
```

---

### 2. Test Registry Extension

**File:** `backend/src/main/java/cz/muriel/core/testing/listener/TestRegistryExtension.java`

```java
package cz.muriel.core.testing.listener;

import cz.muriel.core.testing.annotation.UserStory;
import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import cz.muriel.core.testing.repository.TestRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JUnit 5 extension that automatically registers test results to test registry
 */
@Slf4j
@Component
public class TestRegistryExtension implements AfterTestExecutionCallback {

    private final TestRegistryRepository testRegistryRepository;

    public TestRegistryExtension(TestRegistryRepository testRegistryRepository) {
        this.testRegistryRepository = testRegistryRepository;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        try {
            Optional<Method> testMethod = context.getTestMethod();
            if (testMethod.isEmpty()) {
                return;
            }

            Method method = testMethod.get();
            UserStory userStoryAnnotation = method.getAnnotation(UserStory.class);
            
            if (userStoryAnnotation == null) {
                // Skip tests without @UserStory annotation
                return;
            }

            String userStoryId = userStoryAnnotation.value();
            String testId = generateTestId(context);
            String testName = context.getDisplayName();
            String filePath = method.getDeclaringClass().getName();
            TestStatus status = determineStatus(context);
            TestType testType = determineTestType(context);
            long durationMs = context.getExecutionException().isEmpty() 
                ? 0 
                : System.currentTimeMillis(); // Simplified - actual duration from context

            TestRegistry testRegistry = TestRegistry.builder()
                .testId(testId)
                .userStoryId(userStoryId)
                .testType(testType)
                .testName(testName)
                .filePath(filePath)
                .status(status)
                .lastRunAt(LocalDateTime.now())
                .durationMs((int) durationMs)
                .build();

            // Upsert: update if exists, create if new
            Optional<TestRegistry> existing = testRegistryRepository.findByTestId(testId);
            if (existing.isPresent()) {
                TestRegistry record = existing.get();
                record.setStatus(status);
                record.setLastRunAt(LocalDateTime.now());
                record.setDurationMs((int) durationMs);
                testRegistryRepository.save(record);
                log.debug("Updated test registry: {}", testId);
            } else {
                testRegistryRepository.save(testRegistry);
                log.debug("Created test registry: {}", testId);
            }

        } catch (Exception e) {
            log.error("Failed to register test result", e);
            // Don't fail the test if registry fails
        }
    }

    /**
     * Generate unique test ID from class and method name
     */
    private String generateTestId(ExtensionContext context) {
        String className = context.getTestClass()
            .map(Class::getSimpleName)
            .orElse("UnknownClass");
        
        String methodName = context.getTestMethod()
            .map(Method::getName)
            .orElse("unknownMethod");

        // Format: UNIT-ClassName-methodName
        return String.format("UNIT-%s-%s", className, methodName)
            .replaceAll("[^a-zA-Z0-9-]", "-")
            .toUpperCase();
    }

    /**
     * Determine test status from execution result
     */
    private TestStatus determineStatus(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) {
            return TestStatus.FAIL;
        }
        
        // Check if test was skipped
        Optional<String> skipReason = context.getStore(ExtensionContext.Namespace.GLOBAL)
            .get("skipReason", String.class);
        
        if (skipReason.isPresent()) {
            return TestStatus.SKIP;
        }

        return TestStatus.PASS;
    }

    /**
     * Determine test type from test class name
     */
    private TestType determineTestType(ExtensionContext context) {
        String className = context.getTestClass()
            .map(Class::getName)
            .orElse("");

        if (className.contains("integration") || className.contains("Integration")) {
            return TestType.INTEGRATION;
        }

        return TestType.UNIT_BE;
    }
}
```

---

### 3. Enable Extension Globally

**Option A: Test Base Class**

**File:** `backend/src/test/java/cz/muriel/core/BaseUnitTest.java`

```java
package cz.muriel.core;

import cz.muriel.core.testing.listener.TestRegistryExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base class for all unit tests with test registry integration
 */
@SpringBootTest
@ExtendWith(TestRegistryExtension.class)
public abstract class BaseUnitTest {
    // Common test setup
}
```

**Option B: Global Extension Registration**

**File:** `backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`

```
cz.muriel.core.testing.listener.TestRegistryExtension
```

---

## 🧪 Testing

### Example Test with @UserStory

**File:** `backend/src/test/java/cz/muriel/core/user/service/UserServiceTest.java`

```java
package cz.muriel.core.user.service;

import cz.muriel.core.BaseUnitTest;
import cz.muriel.core.testing.annotation.UserStory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest extends BaseUnitTest {

    @Test
    @UserStory("CORE-123")
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Arrange
        String username = "testuser";
        String email = "test@example.com";

        // Act
        User user = userService.createUser(username, email);

        // Assert
        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
    }

    @Test
    @UserStory("CORE-123")
    @DisplayName("Should throw exception for duplicate username")
    void shouldThrowExceptionForDuplicateUsername() {
        // Test implementation
    }

    @Test
    // No @UserStory - will not be registered
    void internalHelperTest() {
        // This test won't be recorded
    }
}
```

### Manual Verification

```bash
# 1. Run backend tests
cd backend
./mvnw test

# 2. Check logs for test registration
# [TestRegistryExtension] Created test registry: UNIT-USERSERVICETEST-SHOULDCREATEUSERWIITHVALIDDATA

# 3. Verify in database
psql -U core -d core -c "SELECT test_id, user_story_id, test_type, status FROM test_registry WHERE test_type = 'UNIT_BE';"

# 4. Verify via API
curl http://localhost:8080/api/test-registry/story/CORE-123 | jq
```

---

## 📊 Example Output

### Console Log
```
[TestRegistryExtension] Created test registry: UNIT-USERSERVICETEST-SHOULDCREATEUSERWIITHVALIDDATA
[TestRegistryExtension] Created test registry: UNIT-USERSERVICETEST-SHOULDTHROWEXCEPTIONFORDUPLICATEUSERNAME
```

### Database Record
```sql
test_id                                              | user_story_id | test_type | status
-----------------------------------------------------+---------------+-----------+--------
UNIT-USERSERVICETEST-SHOULDCREATEUSERWIITHVALIDDATA | CORE-123      | UNIT_BE   | PASS
UNIT-USERSERVICETEST-SHOULDTHROWEXCEPTIONFORDUPLICATE| CORE-123      | UNIT_BE   | PASS
```

---

## ✅ Acceptance Criteria

- [ ] `@UserStory` annotation created
- [ ] `TestRegistryExtension` implemented
- [ ] `afterTestExecution` hook works
- [ ] Test ID auto-generated from class + method
- [ ] User Story ID extracted from annotation
- [ ] Test type determined (UNIT_BE vs INTEGRATION)
- [ ] Status correctly mapped (PASS/FAIL/SKIP)
- [ ] Upsert logic works (update existing, create new)
- [ ] Global registration configured
- [ ] Example test works

---

## 🎯 Definition of Done

- [ ] Annotation created
- [ ] Extension implemented
- [ ] Global registration configured
- [ ] Example tests tagged
- [ ] Manual verification successful
- [ ] Database records created
- [ ] No test failures due to registry
- [ ] Documentation updated

---

## 🔗 Related Tasks

- **T2**: [Backend Model & Repository](./T2-backend-model-repository.md) - Data model
- **T3**: [REST API Controller](./T3-rest-api-controller.md) - API endpoints
- **T4**: [Playwright Reporter](./T4-playwright-reporter.md) - E2E test registration

---

**Back to:** [S8 Tasks](./README.md) | [S8 Story](../S8.md)
