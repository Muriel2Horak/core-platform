# T3: REST API Controller

**Story:** [TF-001: Test Registry & Tracking](../TF-001.md)  
**Status:** 🔵 TODO  
**Effort:** ~2 hodiny  
**LOC:** ~200 řádků

---

## 🎯 Objective

Vytvořit REST API controller pro evidenci testů s endpoints pro dotazy na coverage a test výsledky.

---

## 📋 Requirements

### API Endpoints

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| `GET` | `/api/test-registry/story/{storyId}` | Get all tests for User Story | `List<TestRegistry>` |
| `GET` | `/api/test-registry/coverage` | Get coverage statistics | `CoverageStatsDTO` |
| `GET` | `/api/test-registry/failing` | Get all failing tests | `List<TestRegistry>` |
| `POST` | `/api/test-registry` | Record test run | `TestRegistry` |

---

## 💻 Implementation

### 1. DTOs

**File:** `backend/src/main/java/cz/muriel/core/testing/dto/TestRunRequest.java`

```java
package cz.muriel.core.testing.dto;

import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TestRunRequest {
    
    @NotBlank(message = "Test ID is required")
    private String testId;
    
    private String userStoryId;
    
    @NotNull(message = "Test type is required")
    private TestType testType;
    
    @NotBlank(message = "Test name is required")
    private String testName;
    
    private String filePath;
    
    @NotNull(message = "Status is required")
    private TestStatus status;
    
    private Integer durationMs;
    
    private BigDecimal coverageLines;
}
```

**File:** `backend/src/main/java/cz/muriel/core/testing/dto/CoverageStatsDTO.java`

```java
package cz.muriel.core.testing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageStatsDTO {
    private long totalStories;
    private long testedStories;
    private double coveragePercent;
    private Map<String, Long> byType;
    private long failingTests;
    private long passingTests;
}
```

### 2. REST Controller

**File:** `backend/src/main/java/cz/muriel/core/testing/controller/TestRegistryController.java`

```java
package cz.muriel.core.testing.controller;

import cz.muriel.core.testing.dto.CoverageStatsDTO;
import cz.muriel.core.testing.dto.TestRunRequest;
import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.service.TestRegistryService;
import cz.muriel.core.backlog.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test-registry")
@RequiredArgsConstructor
@Slf4j
public class TestRegistryController {

    private final TestRegistryService testRegistryService;
    private final UserStoryRepository userStoryRepository;

    /**
     * Get all tests for a specific User Story
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<List<TestRegistry>> getTestsByStory(@PathVariable String storyId) {
        log.info("Fetching tests for story: {}", storyId);
        List<TestRegistry> tests = testRegistryService.getTestsByStory(storyId);
        return ResponseEntity.ok(tests);
    }

    /**
     * Get overall test coverage statistics
     */
    @GetMapping("/coverage")
    public ResponseEntity<CoverageStatsDTO> getCoverageStats() {
        log.info("Fetching coverage statistics");

        long totalStories = userStoryRepository.count();
        long testedStories = testRegistryService.getTestedStoriesCount();
        double coveragePercent = totalStories > 0 
            ? (testedStories * 100.0 / totalStories) 
            : 0.0;

        // Get coverage by test type
        Map<String, Long> byType = testRegistryService.getCoverageByType()
            .stream()
            .collect(Collectors.toMap(
                arr -> arr[0].toString(),
                arr -> (Long) arr[1]
            ));

        // Get passing and failing test counts
        long passingTests = testRegistryService.getTestsByStatus(TestStatus.PASS).size();
        long failingTests = testRegistryService.getFailingTests().size();

        CoverageStatsDTO stats = CoverageStatsDTO.builder()
            .totalStories(totalStories)
            .testedStories(testedStories)
            .coveragePercent(Math.round(coveragePercent * 100.0) / 100.0)
            .byType(byType)
            .passingTests(passingTests)
            .failingTests(failingTests)
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Get all failing tests
     */
    @GetMapping("/failing")
    public ResponseEntity<List<TestRegistry>> getFailingTests() {
        log.info("Fetching failing tests");
        List<TestRegistry> tests = testRegistryService.getFailingTests();
        return ResponseEntity.ok(tests);
    }

    /**
     * Record a test run
     */
    @PostMapping
    public ResponseEntity<TestRegistry> recordTestRun(@Valid @RequestBody TestRunRequest request) {
        log.info("Recording test run: {} for story: {}", request.getTestId(), request.getUserStoryId());

        TestRegistry testRegistry = TestRegistry.builder()
            .testId(request.getTestId())
            .userStoryId(request.getUserStoryId())
            .testType(request.getTestType())
            .testName(request.getTestName())
            .filePath(request.getFilePath())
            .status(request.getStatus())
            .durationMs(request.getDurationMs())
            .coverageLines(request.getCoverageLines())
            .build();

        TestRegistry saved = testRegistryService.recordTestRun(testRegistry);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get stale tests (not run in last N days)
     */
    @GetMapping("/stale")
    public ResponseEntity<List<TestRegistry>> getStaleTests(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Fetching stale tests (older than {} days)", days);
        List<TestRegistry> tests = testRegistryService.getStaleTests(days);
        return ResponseEntity.ok(tests);
    }
}
```

---

## 🧪 Testing

### Unit Tests

**File:** `backend/src/test/java/cz/muriel/core/testing/controller/TestRegistryControllerTest.java`

```java
package cz.muriel.core.testing.controller;

import cz.muriel.core.testing.dto.CoverageStatsDTO;
import cz.muriel.core.testing.dto.TestRunRequest;
import cz.muriel.core.testing.model.TestRegistry;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import cz.muriel.core.testing.service.TestRegistryService;
import cz.muriel.core.backlog.repository.UserStoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestRegistryController.class)
class TestRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestRegistryService testRegistryService;

    @MockBean
    private UserStoryRepository userStoryRepository;

    @Test
    void shouldGetTestsByStory() throws Exception {
        TestRegistry testRegistry = TestRegistry.builder()
            .testId("E2E-LOGIN-001")
            .userStoryId("CORE-123")
            .testType(TestType.E2E_SMOKE)
            .testName("Login test")
            .status(TestStatus.PASS)
            .build();

        when(testRegistryService.getTestsByStory("CORE-123"))
            .thenReturn(List.of(testRegistry));

        mockMvc.perform(get("/api/test-registry/story/CORE-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].testId").value("E2E-LOGIN-001"))
            .andExpect(jsonPath("$[0].userStoryId").value("CORE-123"));
    }

    @Test
    void shouldGetCoverageStats() throws Exception {
        when(userStoryRepository.count()).thenReturn(100L);
        when(testRegistryService.getTestedStoriesCount()).thenReturn(80L);

        mockMvc.perform(get("/api/test-registry/coverage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalStories").value(100))
            .andExpect(jsonPath("$.testedStories").value(80))
            .andExpect(jsonPath("$.coveragePercent").value(80.0));
    }

    @Test
    void shouldRecordTestRun() throws Exception {
        TestRunRequest request = new TestRunRequest();
        request.setTestId("E2E-LOGIN-001");
        request.setUserStoryId("CORE-123");
        request.setTestType(TestType.E2E_SMOKE);
        request.setTestName("Login test");
        request.setStatus(TestStatus.PASS);

        TestRegistry saved = TestRegistry.builder()
            .id(1L)
            .testId("E2E-LOGIN-001")
            .userStoryId("CORE-123")
            .testType(TestType.E2E_SMOKE)
            .testName("Login test")
            .status(TestStatus.PASS)
            .build();

        when(testRegistryService.recordTestRun(any())).thenReturn(saved);

        mockMvc.perform(post("/api/test-registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.testId").value("E2E-LOGIN-001"));
    }

    @Test
    void shouldValidateTestRunRequest() throws Exception {
        TestRunRequest request = new TestRunRequest();
        // Missing required fields

        mockMvc.perform(post("/api/test-registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
```

### Integration Test

**File:** `backend/src/integration-test/java/cz/muriel/core/testing/TestRegistryIntegrationTest.java`

```java
package cz.muriel.core.testing;

import cz.muriel.core.testing.dto.TestRunRequest;
import cz.muriel.core.testing.model.TestStatus;
import cz.muriel.core.testing.model.TestType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestRegistryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRecordAndRetrieveTestRun() {
        // Record test run
        TestRunRequest request = new TestRunRequest();
        request.setTestId("E2E-INTEGRATION-001");
        request.setUserStoryId("CORE-999");
        request.setTestType(TestType.E2E_SMOKE);
        request.setTestName("Integration test");
        request.setStatus(TestStatus.PASS);

        ResponseEntity<Void> postResponse = restTemplate.postForEntity(
            "/api/test-registry", request, Void.class);
        assertEquals(HttpStatus.OK, postResponse.getStatusCode());

        // Retrieve by story
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            "/api/test-registry/story/CORE-999", String.class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertTrue(getResponse.getBody().contains("E2E-INTEGRATION-001"));
    }
}
```

---

## ✅ Acceptance Criteria

- [ ] DTOs created (TestRunRequest, CoverageStatsDTO)
- [ ] Controller created with all endpoints
- [ ] `GET /story/{id}` returns tests for User Story
- [ ] `GET /coverage` returns correct coverage stats
- [ ] `GET /failing` returns failing tests
- [ ] `POST /` records test run
- [ ] Validation works (@Valid, @NotBlank)
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration test passing
- [ ] API documented (Swagger/OpenAPI)

---

## 🎯 Definition of Done

- [ ] All endpoints implemented
- [ ] Unit tests passing (80%+ coverage)
- [ ] Integration test passing
- [ ] Validation working correctly
- [ ] Error handling implemented
- [ ] Logging added
- [ ] API documentation generated
- [ ] Code reviewed

---

## 🔗 Next Tasks

- **T4**: [Playwright Reporter](./T4-playwright-reporter.md) - Auto-register E2E tests
- **T5**: [JUnit Listener](./T5-junit-listener.md) - Auto-register Unit tests

---

**Back to:** [TF-001 Tasks](./README.md) | [TF-001 Story](../TF-001.md)
