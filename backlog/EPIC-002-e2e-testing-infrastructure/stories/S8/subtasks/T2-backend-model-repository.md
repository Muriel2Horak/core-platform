# T2: Backend Model & Repository

**Story:** [S8: Test Registry & Tracking](../S8.md)  
**Status:** 🔵 TODO  
**Effort:** ~2 hodiny  
**LOC:** ~150 řádků

---

## 🎯 Objective

Implementovat JPA entity `TestRegistry` s repository pro ORM mapping test_registry tabulky.

---

## 📋 Requirements

- JPA entity s mappingem na `test_registry` table
- Repository interface s custom queries
- Service layer pro business logiku
- Enums pro `TestType` a `TestStatus`

---

## 💻 Implementation

### 1. Enums

**File:** `backend/src/main/java/cz/muriel/core/testing/model/TestType.java`

```java
package cz.muriel.core.testing.model;

public enum TestType {
    E2E_SMOKE("E2E Smoke Test"),
    E2E_FULL("E2E Full Test"),
    UNIT_FE("Frontend Unit Test"),
    UNIT_BE("Backend Unit Test"),
    INTEGRATION("Integration Test"),
    A11Y("Accessibility Test");

    private final String displayName;

    TestType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/testing/model/TestStatus.java`

```java
package cz.muriel.core.testing.model;

public enum TestStatus {
    PASS("Passed"),
    FAIL("Failed"),
    SKIP("Skipped");

    private final String displayName;

    TestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

### 2. JPA Entity

**File:** `backend/src/main/java/cz/muriel/core/testing/model/TestRegistry.java`

```java
package cz.muriel.core.testing.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_registry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false, unique = true, length = 255)
    private String testId;

    @Column(name = "user_story_id", length = 50)
    private String userStoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false, length = 50)
    private TestType testType;

    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestStatus status;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "coverage_lines", precision = 5, scale = 2)
    private BigDecimal coverageLines;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 3. Repository Interface

**File:** `backend/src/main/java/cz/muriel/core/testing/repository/TestRegistryRepository.java`

```java
package cz.muriel.core.testing.repository;

import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRegistryRepository extends JpaRepository<TestRegistry, Long> {

    // Find by test ID
    Optional<TestRegistry> findByTestId(String testId);

    // Find all tests for a User Story
    List<TestRegistry> findByUserStoryId(String userStoryId);

    // Find all tests by type
    List<TestRegistry> findByTestType(TestType testType);

    // Find all tests by status
    List<TestRegistry> findByStatus(TestStatus status);

    // Count distinct User Stories with passing tests
    @Query("SELECT COUNT(DISTINCT tr.userStoryId) FROM TestRegistry tr WHERE tr.status = :status")
    long countDistinctUserStoryIdByStatus(@Param("status") TestStatus status);

    // Get test coverage by type
    @Query("SELECT tr.testType, COUNT(DISTINCT tr.userStoryId) FROM TestRegistry tr " +
           "WHERE tr.status = 'PASS' GROUP BY tr.testType")
    List<Object[]> getCoverageByType();

    // Find failing tests
    @Query("SELECT tr FROM TestRegistry tr WHERE tr.status = 'FAIL' ORDER BY tr.lastRunAt DESC")
    List<TestRegistry> findFailingTests();

    // Find tests not run recently (e.g., 7 days)
    @Query("SELECT tr FROM TestRegistry tr WHERE tr.lastRunAt < :cutoffDate OR tr.lastRunAt IS NULL")
    List<TestRegistry> findStaleTests(@Param("cutoffDate") LocalDateTime cutoffDate);
}
```

### 4. Service Layer

**File:** `backend/src/main/java/cz/muriel/core/testing/service/TestRegistryService.java`

```java
package cz.muriel.core.testing.service;

import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.repository.TestRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRegistryService {

    private final TestRegistryRepository repository;

    @Transactional
    public TestRegistry recordTestRun(TestRegistry testRegistry) {
        log.debug("Recording test run: {} for story: {}", 
                  testRegistry.getTestId(), testRegistry.getUserStoryId());

        // Check if test already exists
        Optional<TestRegistry> existing = repository.findByTestId(testRegistry.getTestId());

        if (existing.isPresent()) {
            // Update existing record
            TestRegistry record = existing.get();
            record.setStatus(testRegistry.getStatus());
            record.setLastRunAt(LocalDateTime.now());
            record.setDurationMs(testRegistry.getDurationMs());
            record.setCoverageLines(testRegistry.getCoverageLines());
            return repository.save(record);
        } else {
            // Create new record
            testRegistry.setLastRunAt(LocalDateTime.now());
            return repository.save(testRegistry);
        }
    }

    public List<TestRegistry> getTestsByStory(String userStoryId) {
        return repository.findByUserStoryId(userStoryId);
    }

    public long getTestedStoriesCount() {
        return repository.countDistinctUserStoryIdByStatus(TestStatus.PASS);
    }

    public List<TestRegistry> getFailingTests() {
        return repository.findFailingTests();
    }

    public List<TestRegistry> getStaleTests(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        return repository.findStaleTests(cutoff);
    }
}
```

---

## 🧪 Testing

### Unit Tests

**File:** `backend/src/test/java/cz/muriel/core/testing/service/TestRegistryServiceTest.java`

```java
package cz.muriel.core.testing.service;

import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import cz.muriel.core.testing.repository.TestRegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestRegistryServiceTest {

    @Mock
    private TestRegistryRepository repository;

    @InjectMocks
    private TestRegistryService service;

    private TestRegistry testRegistry;

    @BeforeEach
    void setUp() {
        testRegistry = TestRegistry.builder()
            .testId("E2E-LOGIN-001")
            .userStoryId("CORE-123")
            .testType(TestType.E2E_SMOKE)
            .testName("Login flow test")
            .status(TestStatus.PASS)
            .durationMs(1500)
            .build();
    }

    @Test
    void shouldCreateNewTestRecord() {
        when(repository.findByTestId("E2E-LOGIN-001")).thenReturn(Optional.empty());
        when(repository.save(any(TestRegistry.class))).thenReturn(testRegistry);

        TestRegistry result = service.recordTestRun(testRegistry);

        assertNotNull(result);
        assertEquals("E2E-LOGIN-001", result.getTestId());
        verify(repository).save(any(TestRegistry.class));
    }

    @Test
    void shouldUpdateExistingTestRecord() {
        TestRegistry existing = TestRegistry.builder()
            .id(1L)
            .testId("E2E-LOGIN-001")
            .userStoryId("CORE-123")
            .testType(TestType.E2E_SMOKE)
            .testName("Login flow test")
            .status(TestStatus.FAIL)
            .build();

        when(repository.findByTestId("E2E-LOGIN-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(TestRegistry.class))).thenReturn(existing);

        testRegistry.setStatus(TestStatus.PASS);
        TestRegistry result = service.recordTestRun(testRegistry);

        assertEquals(TestStatus.PASS, result.getStatus());
        verify(repository).save(any(TestRegistry.class));
    }

    @Test
    void shouldGetTestsByStory() {
        when(repository.findByUserStoryId("CORE-123")).thenReturn(List.of(testRegistry));

        List<TestRegistry> results = service.getTestsByStory("CORE-123");

        assertEquals(1, results.size());
        assertEquals("CORE-123", results.get(0).getUserStoryId());
    }
}
```

---

## ✅ Acceptance Criteria

- [ ] `TestType` enum created (6 values)
- [ ] `TestStatus` enum created (3 values)
- [ ] `TestRegistry` entity created with all fields
- [ ] JPA annotations correct (@Entity, @Column, etc.)
- [ ] `@PrePersist` sets timestamps automatically
- [ ] `TestRegistryRepository` interface created
- [ ] Custom queries implemented (findByUserStoryId, countDistinct, etc.)
- [ ] `TestRegistryService` created with business logic
- [ ] Unit tests written (80%+ coverage)
- [ ] All tests passing

---

## 🎯 Definition of Done

- [ ] Code compiles without errors
- [ ] Unit tests passing (80%+ coverage)
- [ ] Repository queries tested
- [ ] Service layer tested
- [ ] Lombok annotations working (@Data, @Builder)
- [ ] JPA mapping verified (application starts)
- [ ] Code reviewed

---

## 🔗 Next Tasks

- **T3**: [REST API Controller](./T3-rest-api-controller.md) - Expose endpoints
- **T4**: [Playwright Reporter](./T4-playwright-reporter.md) - Auto-register E2E tests

---

**Back to:** [S8 Tasks](./README.md) | [S8 Story](../S8.md)
